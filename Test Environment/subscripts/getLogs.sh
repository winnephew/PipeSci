#!/usr/bin/expect -f
set user weggeand
set host alex.informatik.hu-berlin.de
set now [clock format [clock seconds] -format "%d-%m-%y_%H-%M-%S" -timezone "Europe/Berlin"] 

set name "[lindex $argv 0]"
set fileBaseName "[lindex $argv 1]"
set localBasePath "[lindex $argv 2]"
set remoteBasePath "[lindex $argv 3]"


exec mkdir $localBasePath/logs/$now$name

set timeout -1

#spawn ssh $user@$host
#expect "$ "
#send "cd $remoteBasePath\r"
#expect "$ "
#send "mv $fileBaseName* logs/\r"
#expect "$ "
#close $spawn_id

spawn scp -r $user@$host:$remoteBasePath/logs/* $localBasePath/logs/$now$name
expect "$ "
close $spawn_id

spawn ssh $user@$host
expect "$ "
send "cd $remoteBasePath\r"
expect "$ "
send "find . -name \"Task*\" -type d -exec rm -rf {} +\r"
expect "$ "
send "rm -rf logs\r"
expect "$ "
send "exit\r"
close $spawn_id

#eval exec /bin/mv [glob /home/andreas/Dropbox/PipeSci/logs/LOG_*] /home/andreas/Dropbox/PipeSci/logs/$now$name/
#eval exec /bin/mv [glob /home/andreas/Dropbox/PipeSci/logs/TIME*] /home/andreas/Dropbox/PipeSci/logs/$now$name/
#eval exec /bin/mv [glob /home/andreas/Dropbox/PipeSci/$fileName*] /home/andreas/Dropbox/PipeSci/logs/$now$name/
