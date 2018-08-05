#!/bin/bash
export PATH
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
CONF_DIR=$DEPLOY_DIR/conf

LOGS_DIR=""
if [ -n "$LOGS_FILE" ]; then
    LOGS_DIR=`dirname $LOGS_FILE`
else
    LOGS_DIR=$DEPLOY_DIR/logs
fi

if [ ! -d $LOGS_DIR ]; then
    mkdir $LOGS_DIR
fi

STDOUT_FILE=$LOGS_DIR/logs.log
LIB_DIR=$DEPLOY_DIR/lib
APP_PATH=com.yun9.service.tax.TaxApplication
APP_NAME=TaxApplication
LIB_JARS=`ls $LIB_DIR|grep .jar|awk '{print "'$LIB_DIR'/"$0}'|tr "\n" ":"`

JAVA_OPTS="-Djava.awt.headless=true -Djava.net.preferIPv4Stack=true "

JAVA_MEM_OPTS=""
BITS=`java -version 2>&1 | grep -i 64-bit`
if [ -n "$BITS" ]; then
    JAVA_MEM_OPTS=" -server -Xmx1g -Xms1g -Xmn256m -XX:PermSize=128m -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 "
else
    JAVA_MEM_OPTS=" -server -Xms1g -Xmx1g -XX:PermSize=128m -XX:SurvivorRatio=2 -XX:+UseParallelGC "
fi
echo -e "$JAVA_OPTS"
echo -e  "$JAVA_MEM_OPTS"
echo -e  "$CONF_DIR"
echo -e "Starting the $SERVER_NAME ..."
nohup java $JAVA_OPTS $JAVA_MEM_OPTS  -classpath $CONF_DIR:$LIB_JARS $APP_PATH > $STDOUT_FILE 2>&1 &

sleep 2

TIMEOUT=60
USEDTIME=0
echo -e "starting... $APP_NAME"
while [ $USEDTIME -lt $TIMEOUT ];do
   echo ''
   sleep 1
   if [ `grep -c "Started $APP_NAME" $STDOUT_FILE` -eq '1' ];then
   break
   fi
   if [ `grep -c "Application startup failed" $STDOUT_FILE` -eq '1' ];then
   pid=`jps -l | grep $APP_NAME | awk '{print $1}'`
   if [ -n "pid" ];then

   kill -9 $pid
   fi
   cat $STDOUT_FILE | tail -n 100
   exit 1
   break
   fi
   echo -e "=> \c "
   for i in $(seq 1 $USEDTIME)
   do
   echo -e "=\c";
   done
   echo -e "cost $USEDTIME seconds. \c "
   USEDTIME=$((USEDTIME + 1))
   if [ $USEDTIME -eq $TIMEOUT ];then
   pid=`jps -l | grep $APP_NAME | awk '{print $1}'`
   if [ -n "pid" ];then
   kill -9 $pid
   sleep 1
   fi

   cat $STDOUT_FILE | tail -n 100
   echo -e "startup failed,timeout"
   exit 1
   fi
done
   cat $STDOUT_FILE | tail -n 100
   exit 0
