#!/bin/bash


uniquify() {
    echo "Uniquifying po files";
    
    for i in *.po; do
	msguniq -o $i.uniq -u $i;
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

bundle() {
    echo "Creating RersourceBundles";
    
    for i in *.po; do
	msgfmt --java2 -d . -r LimeMessages -l `basename $i | sed -e 's/.po//'` $i;
    done
}

makejar() {
    echo "Making jar";

    jar -cuf messages.jar *.class
}

bundle
