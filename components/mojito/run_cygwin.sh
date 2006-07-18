#!/bin/sh

CLASSPATH=\
".;../lib/jars/commons-logging.jar;../lib/jars/log4j.jar;"

export CLASSPATH

ulimit -n 1024

java -ea com.limegroup.mojito.Main $1 $2 $3
