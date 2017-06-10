#!/bin/bash
########### Params #############
# The following Files have to be present at your LocalBasePath: this script and its "subscripts"-folder, config.properties, PipeSci.jar, TaskNursery.jar and the workflow file you specify below.
# Important hint: for long tests you will have to configure your ssh settings to not lose connection: add "ServerAliveInterval 120" and "ServerAliveCountMax 600" to your /etc/ssh/ssh_config - this will make tests up to 20 hours possible by sending a keep-alive-pulse every 2 minutes and only disconnecting after 600 pulses have been sent without any other traffic.

# name/path to the workflow xml-File from LocalBasePath (see below)
Workflow=sequence_5.xml


# Whether to use pipelining or not: "on", "off", "workflow" - "on"/"off" will activate/deactivate pipelining for every task in the workflow, "workflow" will take take the information for each task from the workflow specification
Pipelining=on


# number of test runs (repetitions) to execute
Runs=10


# Data volume to be generated by Producer task. 
Data_mb=20
		
# Data chunks Task Nursery works on (volume needed in order to produce new output).
Part_size=0.5

# ip of the manager host (required independently from spawnManager argument) 
# 141.20.21.85 - condor-ip, manager will be spawned on condor - alternatives: Ip=87.169.112.41 - Ip=$(hostname -I) - Ip=$(hostname -I | cut -f2 -d " ")
Ip=141.20.21.161 		


# Base Name of the in- and output files. Is used to save result file of workflow to local. Also used to analyze performance measurements for gnuplot.
FileBaseName="file"		


# Used to find the current version off the programs and workflow and send them to remote. Also used to get result/logs/timing onto local.
# LocalBasePath=/home/andreas/Dropbox/PipeSci	
LocalBasePath=/vol/fob-vol6/mi13/weggeand/Bachelor/PipeSci


# Working Folder on Remote
RemoteBasePath=/vol/fob-vol6/mi13/weggeand/Bachelor/PipeSci/currentTest


# 0, 1: in case you want to run the manager somewhere else seperately you can set this to 0 - if 1, the manager will be spawned on the current machine
SpawnManager=1	


# ms, s
TimeUnit=s

############ End of Params #############


parentFolder="$1/"

## Step 1: Propagate above options onto Program properties and Workflow file!

## Count the amount of Workers we are going to need. Important: Manager and Workers are spawned on star, rabe, condor, adler - the other pool-machines are not able to have tcp-connections with each other - multiple workers per host have to be started when more than 4 workers are used it is to be expected that performance degrades, when more workers are spawned than cores are available.
clientNum=$(grep -c "</job>" $Workflow)	
	
sed -i 's/pipelining=.*/pipelining='"$Pipelining"'/' ./config.properties
sed -i 's/waitWorkers=[0-9]*/waitWorkers='"$clientNum"'/' ./config.properties
sed -i 's/-mb [0-9]*/-mb '"$Data_mb"'/' ./$Workflow
sed -i 's/-partsize [0-9]*/-partsize '"$Part_size"'/' ./$Workflow

logFolderName="${Workflow}_${clientNum}Clients_${Data_mb}MB_Pipelining-${Pipelining}"

### Step 2: Run Test Iterations
runCounter=0
while [ $runCounter -lt $Runs ]; do

	cd subscripts

	### Transfer all needed Files to Remote Hosts 
	./sendFilesToRemote.sh $Workflow $LocalBasePath $RemoteBasePath 

	if [ $SpawnManager -gt 0 ]
	then 
		## Start Manager
		./startManager.sh $Workflow $RemoteBasePath &   
		managerPID=$!
	fi

	sleep 5
	counter=1
	let clientNum=clientNum+1
	while [  $counter -lt $clientNum ]; do
		## Start Clients		
		./startClients.sh $counter $Ip $RemoteBasePath &     
		declare "PID_$counter"=$!          
		let counter=counter+1 
	done

	counter=1
	while [  $counter -lt $clientNum ]; do
		pid=PID_$counter	
		wait  ${!pid}	
		let counter=counter+1 
	done

	if [ $SpawnManager -gt 0 ]
	then 
	   wait ${!managerPID}
	fi

	let clientNum=clientNum-1

	sleep 20		# give nfs time to synchronize

	## Move Logs and Timing Files from Remote Host to Local	
	./getLogs.sh $logFolderName $FileBaseName $LocalBasePath $RemoteBasePath &
	getLogsPID=$!
	wait ${!getLogsPID}
	cd ..
	let runCounter=runCounter+1
done

timestamp=$( date +%Y-%m-%d:%H:%M:%S )

path="./Auswertungen/"$parentFolder""$logFolderName"_"$Part_size"MbParts_"$Runs"Runs_"$timestamp"/"
mkdir $path
mkdir "$path/raw"
mv ./logs/* $path

## Step 3: Analyze Timing Files

## Manager Statistical Data
find $path -name "TIME_Manager*" -print0 | xargs -0 -I file cat file > "${path}/raw/TIME_all_Manager"
./subscripts/gnuplot/makeMasterHistogram.sh "${path}" $Data_mb $TimeUnit


## Worker Statistical Data
find $path -name "TIME_Worker*" | xargs grep -rnwl -e "ProcessExecutionTime.*-mb" | xargs -I file cat file > "${path}/raw/TIME_Task1"	## Aggregate measurements from first Task

logCounter=1
fileCounter=2
while [ $logCounter -lt $clientNum ]; do	## Aggregate measurements from all the other Tasks
	find $path -name "TIME_Worker*" | xargs grep -rnwl -e "ProcessExecutionTime.*-input, ${FileBaseName}${logCounter}" | xargs -I file cat file > "${path}/raw/TIME_Task$fileCounter"
	let logCounter=logCounter+1
	let fileCounter=fileCounter+1
done

subscripts/gnuplot/makeWorkerHistogram.sh "${path}" $Runs $clientNum

exit 0


