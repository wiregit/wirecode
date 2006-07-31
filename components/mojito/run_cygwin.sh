#!/bin/sh

CLASSPATH=\
".;src/java;../core;"

for JAR in $(find ../lib/jars -name *.jar); do 
   CLASSPATH="${CLASSPATH};${JAR}"
done

echo $CLASSPATH

export CLASSPATH

ulimit -n 1024

java -ea com.limegroup.mojito.Main $1 $2 $3
