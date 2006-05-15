#!/bin/sh

CLASSPATH=\
".;lib/commons-logging.jar;lib/log4j.jar;lib/core.jar;"

export CLASSPATH

ulimit -n 1024

java com.limegroup.mojito.Main $1 $2 $3
