#!/usr/bin/expect -f
set user weggeand
array set hosts {
	0 wedding
	1 britz
	2 buch
	3 buckow
	4 dahlem
	5 gatow
	6 karow
	7 kudamm
	8 lankwitz
	9 marzahn
	10 mitte
	11 pankow
	12 rudow
	13 spandau
	14 staaken
	15 steglitz
	16 tegel
	17 treptow
	18 wannsee
	19 alex
}
set host2 .informatik.hu-berlin.de
set x [lindex $argv 0]
set ip [lindex $argv 1]
set remoteBasePath [lindex $argv 2]

spawn ssh $user@$hosts([expr $x % 18])$host2
set timeout -1
expect "$ "
send "cd $remoteBasePath\r"
expect "$ "
send "java -jar PipeSci.jar worker --ip $ip\r" 
expect "$ "
send "exit\r"
close $spawn_id
