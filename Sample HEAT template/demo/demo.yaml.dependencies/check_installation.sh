#!/bin/bash -x
set -x

yum -y install heat-cfntools
cfn-create-aws-symlinks

yum install -y nc 

HOSTNAME=`hostname`
PORT=$port
TIME=5      # Based on ping "sleep" of 10 sec.
            # 6 intervals per minute.
            # 7 minutes total time for guest to respond

VMCHKOK=0
count=0
countmax=${TIME}


while [ $VMCHKOK = 0 ]
do
    nc -z -w2 ${HOSTNAME} ${PORT} 1>/dev/null 2>&1
    if [ $? = 0 ]
    then
        VMCHKOK=1
        echo "Service Check - Passed"
        result=0
    fi
    sleep 10
    (( count = $count + 1 ))
    echo "Service Check - Count = $count : Max allowed = $TIME"
    if [ $count -gt $countmax ]
    then
        VMCHKOK=1
        echo "Service Check - Failed"
        result=1
    fi
done

if [ $result -eq 0 ]; then
	/opt/aws/bin/cfn-signal -e 0 -r "server setup complete" '$wait_handle'
else
	/opt/aws/bin/cfn-signal -e 1 -r "server setup failed" '$wait_handle'
fi