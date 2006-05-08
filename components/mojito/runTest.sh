#!/bin/sh

ulimit -n 1024
java -cp .:lib/commons-logging.jar:lib/log4j.jar:lib/core.jar com.limegroup.gnutella.dht.tests.PlanetLab $1 $2 $3 $4 $5
