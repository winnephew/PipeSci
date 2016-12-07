#!/bin/bash
path=$1
RUNS=$2
TASKNUM=$3
XRANGE=$TASKNUM+1

counter=0
while [ $counter -lt $TASKNUM ]; do
	let counter=counter+1	
	meanStdDev=$(awk '{for(i=1;i<=NF;i++) {if(NF==11) { execSum[i] += $i; execSumsq[i] += ($i)^2; } if(NF==3) { tDelaySum[i] += $i; tDelaySumsq[i] += ($i)^2; } if(NF==2) { throughputSum[i] += $i; throughputSumsq[i] += ($i)^2; } } } END {print execSum[2]/(NR/3), sqrt((execSumsq[2]-execSum[2]^2/(NR/3))/(NR/3)), tDelaySum[2]/(NR/3), sqrt((tDelaySumsq[2]-tDelaySum[2]^2/(NR/3))/(NR/3)),  throughputSum[2]/(NR/3), sqrt((throughputSumsq[2]-throughputSum[2]^2/(NR/3))/(NR/3))}' "${path}raw/TIME_Task$counter")

	## FILE Format: TaskNum executionTimeMean executionTimeStdDev taskDelayMean taskDelayStdDev througputMean throughputStdDev
	cat >> ${path}Workers.dat << EOF
	$counter $meanStdDev
EOF
## EOF must not be indented!!
done

## 1. Step: Plot Execution Time
OUTFILE="${path}WorkerExecTime.png"
execMeanMax=$(awk -v max=0 '{if($2>max){max=$2}}END{print max} ' ${path}Workers.dat)
execStdDevMax=$(awk -v max=0 '{if($3>max){max=$3}}END{print max} ' ${path}Workers.dat)
SCALE=$(bc <<< "$execMeanMax+(5*$execStdDevMax)+1000")
#every "Line Increment":"Data Block Increment (when Blank lines)":"First Line":"First Data Block":"Last Line":"Last Data Block"
gnuplot << EOF
set key bmargin left
set lmargin at screen 0.20
set rmargin at screen 0.95
set bmargin at screen 0.15
set tmargin at screen 0.85
set datafile separator " "
set title "Execution Time"
set ylabel "Time (ms)"
set yrange [0:$SCALE]
set xlabel "Tasks"
set xrange [0:$XRANGE]
set xtic
set boxwidth 0.25
set terminal png
set output "$OUTFILE"
plot "${path}Workers.dat" using 1:2:3:xtic(1) with boxerrorbars notitle
EOF

## 2. Step: Plot Task Delay
OUTFILE="${path}WorkerTaskDelay.png"
tDelaytMeanMax=$(awk -v max=0 '{if($4>max){max=$4}}END{print max} ' ${path}Workers.dat)
tDelayStdDevMax=$(awk -v max=0 '{if($5>max){max=$5}}END{print max} ' ${path}Workers.dat)
tDelayScale=$(bc <<< $tDelaytMeanMax+$tDelayStdDevMax*2)
gnuplot << EOF
set key bmargin left
set lmargin at screen 0.20
set rmargin at screen 0.95
set bmargin at screen 0.15
set tmargin at screen 0.85
set datafile separator " "
set title "Task Delay"
set ylabel "Time (ms)"
set yrange [0:$tDelayScale]
set xlabel "Tasks"
set xrange [0:$XRANGE]
set xtic
set boxwidth 0.25
set terminal png
set output "$OUTFILE"
plot "${path}Workers.dat" using 1:4:5:xtic(1) with boxerrorbars notitle
EOF


## 3. Step: Plot Throughput
OUTFILE="${path}WorkerThroughput.png"
throughputMeanMax=$(awk -v max=0 '{if($6>max){max=$6}}END{print max} ' ${path}Workers.dat)
throughputStdDevMax=$(awk -v max=0 '{if($7>max){max=$7}}END{print max} ' ${path}Workers.dat)
throughputScale=$throughputtMeanMax
gnuplot << EOF
set key bmargin left
set lmargin at screen 0.20
set rmargin at screen 0.95
set bmargin at screen 0.15
set tmargin at screen 0.85
set datafile separator " "
set title "Throughput"
set ylabel "Time (ms) - Logscale"
set yrange [1:$throughputScale]
set xlabel "Tasks"
set xrange [0:$XRANGE]
set xtic
set boxwidth 0.25
set terminal png
set logscale y 10
set output "$OUTFILE"
plot "${path}Workers.dat" using 1:6:7:xtic(1) with boxerrorbars notitle
EOF

