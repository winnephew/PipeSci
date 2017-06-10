#!/usr/bin/expect -f
set user weggeand
set host alex.informatik.hu-berlin.de
set Workflow [lindex $argv 0]
set LocalBasePath [lindex $argv 1]
set RemoteBasePath [lindex $argv 2]


spawn scp $LocalBasePath/$Workflow $LocalBasePath/PipeSci.jar $LocalBasePath/TaskNursery.jar $LocalBasePath/config.properties $user@$host:$RemoteBasePath
expect "$ "
close $spawn_id
