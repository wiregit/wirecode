package com.limegroup.gnutella.util;

/*
 * class that handles the removal of accents, etc.
 */
public class I18NConvert {

    /* data class to use for the raw look up of conversions */
    private final I18NData _data;
    
    /* instance */
    private final static I18NConvert _instance = new I18NConvert();
    
    private I18NConvert() {
        _data = I18NData.instance();
    }

    /* accesor */
    public static I18NConvert instance() {
        return _instance;
    }

    /*
     * Return the converted form of the string s
     * this method will also split the s into the different
     * unicode blocks
     * @param s String to be converted
     * @return the converted string
     */
    public String getNorm(String s) {
        return blockSplit(getKC(getDK(s)));
    } 
    
    
    /*
     * Returns an array of keywords built from parameter s.
     * The string s will be first converted (removal of accents, etc.)
     * then split into the unicode blocks, then the array will be created
     * by splitting the string by 'space'.  The conversion will convert
     * all delim characters to '\u0020' so we just split with '\u0020'
     * @param s source string to split into keywords
     * @return an array of keywords created from s
     */
    public String[] getKeywords(String s) {
        return StringUtils.split(blockSplit(getKC(getDK(s))), " ");
    }
    

    /*
     * Return the decomposed form of parameter s. For each char
     * in the String s, we do a look up using the data class
     * for the decomposed format (this format is not strictly 
     * a NFKD format since it will also remove accents and symbols)
     * @param s string to decompose
     * @return the converted string
     */
    private String getDK(String s) {
        if(s.length() == 0) return  s;
        else {
            StringBuffer buf = new StringBuffer();
            for(int i = 0, n = s.length(); i < n; i++)
                buf.append(_data.getDK(s.charAt(i)));
            return buf.toString();
        }
    }

    /*
     * Return the composed form of string s. Do a look up on the data
     * class for any entries that would combine two chars at a time.
     * Similar to composition described in Technical Report 15 on 
     * www.unicode.org site.
     * @param s String to be composed
     * @return converted form
     */
    private String getKC(String s) {
        if(s.length() == 0) return s;
        else {
            char first = s.charAt(0);
            StringBuffer b = new StringBuffer();
            String comped = "";
            
            for(int i = 1, n = s.length(); i < n; i++) {
                //see if these two chars can be combined according
                //to the look up table
                //TODO: this look up can use a 32bit int created from the
                //    : two chars rather than use a string (need to change the
                //    : underlying data struct for I18NData
                comped = _data.getKC(String.valueOf(first) + String.valueOf(s.charAt(i)));
                //able to compose so we set the composed char to
                //the first to see if more compositions can be made
                if(comped != null) 
                    first = comped.charAt(0);
                else {
                    //the two chars weren't composed so append to
                    //buffer and set the first char to the next char
                    b.append(first);
                    first = s.charAt(i);
                }
            }
            //append the last char used
            b.append(first);
        return b.toString();
        }
    }

    /*
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

    /*
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

    /*
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



