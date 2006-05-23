#!/bin/bash

PATH_SEPARATOR=":"

ulimit -n 1024

CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}.${PATH_SEPARATOR}../core${PATH_SEPARATOR}../lib/messagebundles"
for JAR in $(find ../lib/jars -name *.jar); do 
   CLASSPATH="${CLASSPATH}${PATH_SEPARATOR}${JAR}"
done

export CLASSPATH

java com.limegroup.mojito.Main $1 $2 $3
