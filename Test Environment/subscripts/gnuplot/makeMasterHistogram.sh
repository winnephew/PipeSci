#!/bin/sh
path=$1 
mb=$2
timeUnit=$3

meanStdDev=$(awk '{for(i=1;i<=NF;i++) {sum[i] += $i; sumsq[i] += ($i)^2}} END {print sum[2]/NR, sqrt((sumsq[2]-sum[2]^2/NR)/NR)}' "${path}/raw/TIME_all_Manager")
#mean=$(echo $meanStdDev | cut -f1 -d " ")
#stdDev=$(echo $meanStdDev | cut -f2 -d " ")

minMax=$(awk -F, 'NR==1{s=m=$1}{a[$1]=$0;m=($1>m)?$1:m;s=($1<s)?$1:s}END{print a[s] a[m]}' "${path}/raw/TIME_all_Manager" | grep -Eo '[0-9]{1,}')
#min=$(echo $minMax | cut -f1 -d " ")
max=$(echo $minMax | cut -f2 -d " ")

#neg=$(bc <<< "$mean-$min")
#pos=$(bc <<< "$mean-$max")

OUTFILE="${path}Makespan.eps"

cat > ${path}Manager.dat << EOF
$mb $meanStdDev
EOF

INFILE="${path}Manager.dat"

gnuplot << EOF
set lmargin at screen 0.20
set rmargin at screen 0.95
set bmargin at screen 0.15
set tmargin at screen 0.85
set datafile separator " "
set title "Makespan [${timeUnit}]"
set ylabel "Time"
set yrange [0:($max+2000)]
set xlabel "Mean with standard deviation"
set xrange [-0.25:0.25]
set xtic
set boxwidth 0.25
set terminal postscript eps enhanced
set output "$OUTFILE"
plot "$INFILE" using :2:3:xtic(2) with boxerrorbars notitle
EOF
