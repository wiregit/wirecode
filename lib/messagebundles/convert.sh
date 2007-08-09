#!/bin/bash


uniquify() {
    echo "Uniquifying po files";
    
    for i in *.po; do
	msguniq --use-first -o $i.uniq -u $i;
    done
}

rename() {
    echo "Renaming them";
    
    for i in *.uniq; do
	mv $i `basename $i | sed -e 's/.uniq//'`;
    done
}
 
merge() {   
    echo "Merging them";
    
    for i in *.po; do
	msgmerge --backup=numbered -U $i extractedkeys.pot;
    done
}

generateDefault() {
    echo "Generating default catalog";
    msgen extractedkeys.pot -o default.po
    perl -p -i -e "s/CHARSET/UTF-8/g;" default.po
}

classbundle() {
    echo "Creating class RersourceBundles";
    
    for i in *.po; do
	msgfmt --java2 -d . -r LimeMessages -l `basename $i | sed -e 's/.po//'` $i;
    done
    msgfmt --java2 -d . -r LimeMessages default.po
}

propsbundle() {
    echo "Creating properties RersourceBundles";
    
    for i in *.po; do
	msgcat -t "UTF-8" -p -o "LimeMessages_`basename $i | sed -e 's/.po//'`.properties" $i;
    done
    msgcat -t "UTF-8" -p -o LimeMessages.properties default.po
}

makejar() {
    echo "Making jar";

    jar -cuf messages.jar *.class
}

#uniquify
#rename
#merge
#generateDefault
#classbundle
propsbundle
#makejar
