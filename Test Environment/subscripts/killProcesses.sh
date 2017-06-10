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


spawn ssh $user@$hosts(0)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(1)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(2)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(3)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(4)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(5)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(6)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(7)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(8)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(9)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(10)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(11)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(12)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(13)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(14)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(15)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(16)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(17)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(18)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id

spawn ssh $user@$hosts(19)$host2
set timeout -1
expect "$ "
send "pkill PipeSci\r"
expect "$ "
send "pkill TaskNursery\r"
expect "$ "
send "exit\r"
close $spawn_id
