#!/bin/sh

ulimit -n 1024
java -cp .:lib/commons-logging.jar:lib/log4j.jar com.limegroup.mojito.Main $1 $2 $3
