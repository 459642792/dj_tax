#!/bin/bash
pid=`jps -l | grep TaxApplication | awk '{print $1}'`
if [ -n "pid" ];then

kill -9 $pid
sleep 1
echo "stop server  TaxApplication"
fi
