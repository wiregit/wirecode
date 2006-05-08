#!/bin/sh

ulimit -n 1024
java -cp .:lib/commons-logging.jar:lib/log4j.jar:lib/core.jar de.kapsi.net.kademlia.Main $1 $2 $3
