#!/bin/bash


fileBaseName="file"
ClientNum=15


## Aggregate measurements from first Task
find . -name "TIME_Worker*" | xargs grep -rnwl -e "ProcessExecutionTime.*-units" | xargs -I file cat file > "TIME_Task1"

## Aggregate measurements from all the other Tasks
logCounter=1
fileCounter=2
while [ $logCounter -lt $ClientNum ]; do
	find . -name "TIME_Worker*" | xargs grep -rnwl -e "ProcessExecutionTime.*-input, ${fileBaseName}${logCounter}" | xargs -I file cat file > "TIME_Task$fileCounter"
	let logCounter=logCounter+1
	let fileCounter=fileCounter+1
done


#find . -name "TIME_Manager" -print0 | xargs -0 -I file cat file > TIME_Manager_all

#./makeWorkerHistogram.sh "Task Delay" "ms" "Run" "TIME_PLOT_1_2"



