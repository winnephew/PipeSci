#!/bin/sh
TITLE=$1
YLABEL=$2
XLABEL=$3
LOGFILE=$4 #./time.log
LOGFILETWO=$5 
OUTFILE=$6 #./time-plot.png

# plot 'file' using 1:2:(sqrt($1)) with xerrorbar

gnuplot << EOF
set lmargin at screen 0.20
set rmargin at screen 0.95
set bmargin at screen 0.15
set tmargin at screen 0.85
set datafile separator " "
set title "$TITLE"
set ylabel "$YLABEL"
set yrange [0:24000]
set xlabel "$XLABEL"
set xrange [-1:21]
set style fill transparent solid 0.7 border -1
set boxwidth 0.6 relative
set style histogram clustered gap 0.1
set terminal png
set output "$OUTFILE"
plot "$LOGFILE" using 2 with histogram title "Pipelining",\
     "$LOGFILETWO" using 2 with histogram title "No Pipelining"
