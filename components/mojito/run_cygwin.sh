#!/bin/sh

CLASSPATH=\
".;lib/commons-logging.jar;lib/log4j.jar;../core;../lib/jars/xerces.jar;../lib/jars/xml-apis.jar;"

export CLASSPATH

ulimit -n 1024

java de.kapsi.net.kademlia.Main $1 $2 $3
