package com.limegroup.gnutella.util;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.*;

import com.limegroup.gnutella.ErrorService;

import com.ibm.icu.text.Normalizer;


/**
 * class that handles the removal of accents, etc.
 * this class now uses the Normalizer of icu4j
 */
public class I18NConvert {

    /** BitSet to check for excluded code points */
    private final java.util.BitSet _excluded;
    /** lookup map for locale independent case mapping */
    private final Map _cMap;
    
    /* instance */
    private final static I18NConvert _instance = new I18NConvert();
    
    /**
     * constructor : load in the excluded codepoints and the case map
     */
    private I18NConvert() {
    	java.util.BitSet bs = null;
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
    
    	}
    	catch(IOException ioe) {
    	    ErrorService.error(ioe);
    	}
    	catch(ClassNotFoundException ce) {
    	    ErrorService.error(ce);
    	}
    
    	_excluded = bs;
    	_cMap = hm;
    }

    /** accesor */
    public static I18NConvert instance() {
        return _instance;
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
    	    if(!_excluded.get(c)) {
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


    /**
     * Returns a string split according to the unicode blocks.  A
     * space '\u0020' will be splaced between the blocks.
     * The index to the blockStarts array will be used to compare
     * when splitting the string.
     * @param String s
     * @return string split into blocks with '\u0020' as the delim
     */
    private String blockSplit(String s) {
        if(s.length() == 0) return s;
        else {
            int blockb4 = of(s.charAt(0));
            int curBlock;
            StringBuffer buf = new StringBuffer();
            buf.append(s.charAt(0));
            for(int i = 1, n = s.length(); i < n; i++) {
                curBlock = of(s.charAt(i));
                //compare the blocks of the current char and the char
                //right before. Also, make sure we don't add too many 
                //'\u0020' chars
                if(curBlock != blockb4 && 
                   (s.charAt(i) != '\u0020' && s.charAt(i - 1) != '\u0020'))
                    buf.append("\u0020");
                buf.append(s.charAt(i));
                blockb4 = curBlock;
            }
            
            //get rid of trailing space (if any)
            return buf.toString().trim();
        }
    }

    /**
     * Returns which unicode block the parameter c
     * belongs to. The returned int is the index to the blockStarts
     * array. 
     * @param char c 
     * @return index to array
     */
	private int of(char c) {
	    int top, bottom, current;
	    bottom = 0;
	    top = blockStarts.length;
	    current = top/2;
	    while (top - bottom > 1) {
    		if (c >= blockStarts[current]) {
    		    bottom = current;
    		} else {
    		    top = current;
    		}
    		current = (top + bottom) / 2;
	    }
	    return current;
	}

    /**
     * copy from Character.java
     * the boundaries for each of the unicode blocks
     */
	private static final char blockStarts[] = {
        '\u0000',
        '\u0080',
	    '\u0100',
	    '\u0180',
	    '\u0250',
	    '\u02B0',
	    '\u0300', 
	    '\u0370',
	    '\u0400',
	    '\u0500', // unassigned
	    '\u0530',
	    '\u0590',
	    '\u0600',
	    '\u0700', // unassigned
	    '\u0900',
	    '\u0980',
	    '\u0A00',
	    '\u0A80',
	    '\u0B00',
	    '\u0B80',
	    '\u0C00',
	    '\u0C80',
	    '\u0D00',
	    '\u0D80', // unassigned
	    '\u0E00',
	    '\u0E80',
	    '\u0F00',
	    '\u0FC0', // unassigned
	    '\u10A0',
	    '\u1100',
	    '\u1200', // unassigned
        '\u13A0',
        '\u1400',
        '\u1680',
        '\u16A0',
        '\u1700',
        '\u1720',
        '\u1740',
        '\u1760',
        '\u1780',
        '\u1800',
        '\u1900',
        '\u1950',
        '\u19E0',
        '\u1D00',
	    '\u1E00',
	    '\u1F00',
	    '\u2000',
	    '\u2070',
	    '\u20A0',
	    '\u20D0',
	    '\u2100',
	    '\u2150',
	    '\u2190',
	    '\u2200',
	    '\u2300',
	    '\u2400',
	    '\u2440',
	    '\u2460',
	    '\u2500',
	    '\u2580',
	    '\u25A0',
	    '\u2600',
	    '\u2700',
	    '\u27C0', // unassigned
	    '\u3000',
	    '\u3040',
	    '\u30A0',
	    '\u3100',
	    '\u3130',
	    '\u3190',
	    '\u3200',
	    '\u3300',
	    '\u3400', // unassigned
	    '\u4E00',
	    '\uA000', // unassigned
	    '\uAC00',
	    '\uD7A4', // unassigned
	    '\uD800',
	    '\uE000',
	    '\uF900',
	    '\uFB00',
	    '\uFB50',
	    '\uFE00', // unassigned
	    '\uFE20',
	    '\uFE30',
	    '\uFE50',
	    '\uFE70',
	    '\uFEFF', // special
	    '\uFF00',
	    '\uFFF0'
	};


}



