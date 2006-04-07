#!/bin/sh

ulimit -n 1024
java -cp .:lib/commons-logging.jar:lib/log4j.jar:../core:../lib/jars/xerces.jar:../lib/jars/xml-apis.jar com.limegroup.gnutella.dht.tests.PlanetLab $1 $2 $3 $4 $5
