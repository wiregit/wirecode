package com.limegroup.gnutella.util;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.*;

import com.limegroup.gnutella.ErrorService;
import com.ibm.icu.text.Normalizer;

final class I18NConvertICU extends AbstractI18NConverter {

    private final java.util.BitSet _excluded;
    private final Map _cMap;
    private final java.util.BitSet _replaceWithSpace;

    I18NConvertICU() {
    	java.util.BitSet bs = null;
        java.util.BitSet bs2 = null;
    	Map hm = null;
    	try {
    	    ClassLoader cl = getClass().getClassLoader();
    	    
    	    //read in the explusion bitset
    	    ObjectInputStream ois = 
                new ObjectInputStream(cl.getResource("excluded.dat").openStream());
    	    bs = (java.util.BitSet)ois.readObject();
                
    	    //read in the case map
            ois = new ObjectInputStream(cl.getResource("caseMap.dat").openStream());
            hm = (HashMap)ois.readObject();

            ois = new  ObjectInputStream(cl.getResource("replaceSpace.dat").openStream());
            bs2 = (java.util.BitSet)ois.readObject();
            
    	}
    	catch(IOException ioe) {
    	    ErrorService.error(ioe);
    	}
    	catch(ClassNotFoundException ce) {
    	    ErrorService.error(ce);
    	}
    
    	_excluded = bs;
    	_cMap = hm;
        _replaceWithSpace = bs2;
    }
    
    /**
     * Return the converted form of the string s
     * this method will also split the s into the different
     * unicode blocks
     * @param s String to be converted
     * @return the converted string
     */
    public String getNorm(String s) {
        return convert(s);
    } 
    
    
    /**
     * Returns an array of keywords built from parameter s.
     * The string s will be first converted (removal of accents, etc.)
     * then split into the unicode blocks, then the array will be created
     * by splitting the string by 'space'.  The conversion will convert
     * all delim characters to '\u0020' so we just split with '\u0020'
     * @param s source string to split into keywords
     * @return an array of keywords created from s
     */
    public String[] getKeywords(String s) {
        return StringUtils.split(convert(s), "\u0020");
    }
    
    
    /**
     * convert the string into NFKC + removal of accents, symbols, etc.
     * uses icu4j's Normalizer to first decompose to NFKD form,
     * then removes all codepoints in the exclusion BitSet 
     * finally composes to NFC and adds spaces '\u0020' between
     * different unicode blocks
     *
     * @param String to convert
     * @return converted String
     */
    private String convert(String s) {
    	//decompose to NFKD
    	String nfkd = Normalizer.decompose(s, true);
    	StringBuffer buf = new StringBuffer();
    	int len = nfkd.length();
    	String lower;
    	char c;
    
    	//loop through the string and check for excluded chars
    	//and lower case if necessary
    	for(int i = 0; i < len; i++) {
    	    c = nfkd.charAt(i);
            if(_replaceWithSpace.get(c)) {
                buf.append(" ");
            }
    	    else if(!_excluded.get(c)) {
                lower = (String)_cMap.get(String.valueOf(c));
                if(lower != null)
                    buf.append(lower);
                else
                    buf.append(c);
    	    }
    	}
    	
    	//compose to nfc and split
    	return blockSplit(Normalizer.compose(buf.toString(), false));
    }

}





