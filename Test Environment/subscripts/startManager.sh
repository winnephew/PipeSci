#!/usr/bin/expect -f
set user weggeand
set host alex.informatik.hu-berlin.de
set Workflow [lindex $argv 0]
set remoteBasePath [lindex $argv 1]

spawn ssh $user@$host
set timeout -1
expect "$ "
send "cd $remoteBasePath\r"
expect "$ "
send "java -jar PipeSci.jar manager --xml $Workflow\r" 
expect "$ "
send "exit\r"
close $spawn_id
