#!/bin/bash
parentFolder=$1
clientNum=$2
Data_mb=$3
timeUnit=$4
workflowTitle=$5
declare -a pipeliningOptions=($6 $7 $8)


declare -a INFILES=()
declare -a TITLES=()
declare -a COLORS=("#4c9f76" "#9f4c4c" "#d3d3d3")

# Create Input Data
for i in "${pipeliningOptions[@]}"
do
	find $parentFolder -type d -name "*Pipelining-${i}*" -print0 | xargs -0 -I{} find '{}' -name "Manager.dat" -print0 | xargs -0 -I{} cat '{}' > "${parentFolder}Manager_all_pipe${i}_raw.dat"
	sort -n "${parentFolder}Manager_all_pipe${i}_raw.dat" | LC_ALL=en_US.utf8 numfmt --field 1 --to=si > "${parentFolder}Manager_all_pipe${i}.dat"
	rm "${parentFolder}Manager_all_pipe${i}_raw.dat"
	if [ ${i} = "workflow" ]
	then
		TITLES+=("mixed")
	else
		TITLES+=("${i}")
	fi
	
	INFILES+=("${parentFolder}Manager_all_pipe${i}.dat")
done

# Calculate needed Graph Ranges and relative results
if [ -e "${parentFolder}Manager_all_pipeoff.dat" ]
then
	max=$(awk 'NR == 1 || $1 > max {max=$1}END{print max}' "${parentFolder}Manager_all_pipeoff.dat")
	ymax=$(awk 'NR == 1 || $2 > ymax {ymax=$2}END{print ymax}' "${parentFolder}Manager_all_pipeoff.dat")	
	
	awk 'FNR==NR{a[$1]=$2}FNR!=NR{print 1-a[$1]/$2}' "${parentFolder}Manager_all_pipeon.dat" "${parentFolder}Manager_all_pipeoff.dat" > "${parentFolder}Manager_all_pipeonRelative.dat"
	paste -d' ' "${parentFolder}Manager_all_pipeon.dat" "${parentFolder}Manager_all_pipeonRelative.dat" > "${parentFolder}Manager_all_pipeon_final.dat"
	rm "${parentFolder}Manager_all_pipeon.dat"
	mv "${parentFolder}Manager_all_pipeon_final.dat" "${parentFolder}Manager_all_pipeon.dat"
	
	awk 'FNR==NR{a[$1]=$2}FNR!=NR{print 1-a[$1]/$2}' "${parentFolder}Manager_all_pipeworkflow.dat" "${parentFolder}Manager_all_pipeoff.dat" > "${parentFolder}Manager_all_pipeworkflowRelative.dat"
	paste -d' ' "${parentFolder}Manager_all_pipeworkflow.dat" "${parentFolder}Manager_all_pipeworkflowRelative.dat" > "${parentFolder}Manager_all_pipeworkflow_final.dat"
	rm "${parentFolder}Manager_all_pipeworkflow.dat"
	mv "${parentFolder}Manager_all_pipeworkflow_final.dat" "${parentFolder}Manager_all_pipeworkflow.dat"
else
	max=$(awk 'NR == 1 || $1 > max {max=$1}END{print max}' "${parentFolder}Manager_all_pipeworkflow.dat")
	ymax=$(awk 'NR == 1 || $2 > max {max=$2}END{print max}' "${parentFolder}Manager_all_pipeworkflow.dat")
	
	awk 'FNR==NR{a[$1]=$2}FNR!=NR{print 1-a[$1]/$2}' "${parentFolder}Manager_all_pipeon.dat" "${parentFolder}Manager_all_pipeworkflow.dat" > "${parentFolder}Manager_all_pipeonRelative.dat"
	paste -d' ' "${parentFolder}Manager_all_pipeon.dat" "${parentFolder}Manager_all_pipeonRelative.dat" > "${parentFolder}Manager_all_pipeon_final.dat"
	rm "${parentFolder}Manager_all_pipeon.dat"
	mv "${parentFolder}Manager_all_pipeon_final.dat" "${parentFolder}Manager_all_pipeon.dat"
fi



# Set y-Axis Number Format depending on TimeUnit
format="%.2g"
if [ $timeUnit = "s" ]
then 
	format="%.0f"
fi

OUTFILE="${parentFolder}Makespan.eps"
PLOTFILE="${parentFolder}gnuplot.in" 

echo > $PLOTFILE
echo "set lmargin at screen 0.15" >> $PLOTFILE
echo "set rmargin at screen 0.95" >> $PLOTFILE
echo "set bmargin at screen 0.15" >> $PLOTFILE
echo "set tmargin at screen 0.9" >> $PLOTFILE
echo "set datafile separator \" \"" >> $PLOTFILE
echo "set title \"$workflowTitle - $clientNum tasks - Mean and relative improvement\"" >> $PLOTFILE
echo "set ylabel \"Makespan [${timeUnit}]\"" >> $PLOTFILE
echo "set yrange [0:($ymax+($ymax*0.2))]" >> $PLOTFILE
echo "set xlabel \"Initially Produced Data Volume [MB]\"" >> $PLOTFILE
echo "set xrange [-0.5:$Data_mb]" >> $PLOTFILE
echo "set xtic" >> $PLOTFILE
echo "set xtics rotate by -45" >> $PLOTFILE
echo "set format y \"$format\"" >> $PLOTFILE
echo "set format x \"%.0f\"" >> $PLOTFILE
echo "set key at 0.2,($ymax+($ymax*0.05)) samplen 2 title \"Pipelining:\"" >> $PLOTFILE
echo "set boxwidth 0.9" >> $PLOTFILE
echo "set style fill solid 1 border 0" >> $PLOTFILE
echo "set style histogram errorbars gap 2 lw 3" >> $PLOTFILE
echo "set style data histogram" >> $PLOTFILE
echo "set bars 0.33" >> $PLOTFILE
echo "set terminal postscript eps enhanced" >> $PLOTFILE
echo "set output \"$OUTFILE\"" >> $PLOTFILE

echo -n "plot " >> $PLOTFILE
for j in "${!INFILES[@]}"
do
	echo -n "\"${INFILES[$j]}\" using 2:3:xtic(1) title \"${TITLES[$j]}\" lc rgb \"${COLORS[$j]}\"" >> $PLOTFILE

	if [ $j -lt ${#INFILES[@]} ]
	then 
		echo ",\\" >> $PLOTFILE
	fi
done

if [ -e "${parentFolder}Manager_all_pipeon.dat" ]
then
	#echo ",\\" >> $PLOTFILE
	if [ -e "${parentFolder}Manager_all_pipeworkflow.dat" ]
	then
		echo "\"${parentFolder}Manager_all_pipeon.dat\" using 0:2:(sprintf(\"%d%%\", (\$4*100))) with labels center offset -1.9,1.8 notitle,\\" >> $PLOTFILE
		echo "\"${parentFolder}Manager_all_pipeworkflow.dat\" using 0:2:(sprintf(\"%d%%\", (\$4*100))) with labels center offset 6.5,1.5 notitle" >> $PLOTFILE
	else
		echo "\"${parentFolder}Manager_all_pipeon.dat\" using 0:2:(sprintf(\"%d%%\", (\$4*100))) with labels center offset -1.9,1.8 notitle" >> $PLOTFILE
	fi	
fi

gnuplot $PLOTFILE
