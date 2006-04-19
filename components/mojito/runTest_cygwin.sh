#!/bin/sh

CLASSPATH=\
".;lib/commons-logging.jar;lib/log4j.jar;"

export CLASSPATH

ulimit -n 1024

java com.limegroup.gnutella.dht.tests.PlanetLab $1 $2 $3 $4 $5