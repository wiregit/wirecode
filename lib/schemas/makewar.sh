#!/bin/sh

### Builds the xml.war & places it in GUI.

VALID_ITEMS="application audio image text video"

rm -f xml.war
jar -c0Mf xml.war xml/data/delete_me

for i in $VALID_ITEMS
do
    jar -u0Mf xml.war xml/misc/$i.gif
    jar -u0Mf xml.war xml/schemas/$i.xsd
done

mv xml.war ../../gui/xml.war

    