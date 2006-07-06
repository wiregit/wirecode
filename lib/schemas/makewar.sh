#!/bin/sh

### Builds the xml.war & places it in GUI.

VALID_ITEMS="application audio image document video"

rm -f xml.war
mkdir xml/data
touch xml/data/delete_me
jar -c0Mf xml.war xml/data/delete_me
rm -rf xml/data

for i in $VALID_ITEMS
do
    jar -u0Mf xml.war xml/misc/$i.gif
    jar -u0Mf xml.war xml/schemas/$i.xsd
done

mv xml.war ../../gui/xml.war

    
