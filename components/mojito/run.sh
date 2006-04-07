#!/bin/sh

ulimit -n 1024
java -cp .:lib/commons-logging.jar:lib/log4j.jar:../core:../lib/jars/xerces.jar:../lib/jars/xml-apis.jar de.kapsi.net.kademlia.Main $1 $2 $3
