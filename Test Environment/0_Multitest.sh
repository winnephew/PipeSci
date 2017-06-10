#!/bin/bash

## This file facilitates running multiple Tests with different setting, testing multiple workflows with different amounts of data both with and without pipelining. 
## If you only want to do a small test with a single setting, please use 1_Testrun.sh.

## Note: You will still have to configure base settings in like Folders and IP in 1_Testrun.sh.


################ Params #####################

parentFolderName="sequence5_with_5partDelay"

graphTitle="Workflow with sequence of 5 tasks"
# Unit of Time measurements in resulting Plot: ms, s 
TimeUnit="s" 

Runs=10

declare -a workflows=("sequence_5.xml")	# "broadcastAndMerge.xml" "sequence_long_pipe1122.xml"

declare -a pipelining=("on" "off" "workflow")

declare -a Data_mb=(200)

declare -a Part_size=(1)

################ End of Params ##############

sed -i 's/timing=.*/timing='"$TimeUnit"'/' ./config.properties
sed -i '0,/TimeUnit=.*/{s/TimeUnit=.*/TimeUnit='"$TimeUnit"'/}' ./1_Testrun.sh
sed -i '0,/Runs=.*/{s/Runs=.*/Runs='"$Runs"'/}' ./1_Testrun.sh


	
parentFolder="./Auswertungen/$parentFolderName"
mkdir $parentFolder

for i in "${workflows[@]}"
do

	for j in "${pipelining[@]}"
	do
		for k in "${Data_mb[@]}"
		do
			for l in "${Part_size[@]}"
			do
				sed -i '0,/Workflow=.*/{s/Workflow=.*/Workflow='"$i"'/}' ./1_Testrun.sh
				sed -i '0,/Pipelining=.*/{s/Pipelining=.*/Pipelining='"$j"'/}' ./1_Testrun.sh
				sed -i '0,/Data_mb=.*/{s/Data_mb=.*/Data_mb='"$k"'/}' ./1_Testrun.sh
				sed -i '0,/Part_size=.*/{s/Part_size=.*/Part_size='"$l"'/}' ./1_Testrun.sh
			

				./1_Testrun.sh $parentFolderName  
			done
		done
	done
	clientNum=$(grep -c "</job>" $i)	
	subscripts/gnuplot/multiMasterHistogram.sh $parentFolder $clientNum ${#Data_mb[@]} $TimeUnit $Runs ${pipelining[@]}

	
done
