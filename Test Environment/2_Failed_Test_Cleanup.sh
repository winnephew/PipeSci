#!/bin/bash
########### Params #############
logFoldername="failed"

# Base Name of the in- and output files. Is used to save result file of workflow to local. Also used to analyze performance measurements for gnuplot.
FileBaseName="file"		


# Used to find the current version off the programs and workflow and send them to remote. Also used to get result/logs/timing onto local.
# LocalBasePath="/home/andreas/Dropbox/PipeSci"	
LocalBasePath="/vol/fob-vol6/mi13/weggeand/Bachelor/PipeSci"

# Working Folder on Remote
RemoteBasePath="/vol/fob-vol6/mi13/weggeand/Bachelor/PipeSci"


#./subscripts/getLogs.sh $logFoldername $FileBaseName $LocalBasePath $RemoteBasePath

./subscripts/killProcesses.sh
