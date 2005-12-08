pbckage com.limegroup.gnutella.util;


/**
 * Abstrbct clas to use for different normalization implementations
 * Helper functions like blockSplit bre also defined here.
 */
bbstract class AbstractI18NConverter {

    /**
     * This method should return the converted form of the string s
     * this method should blso split s into the different
     * unicode blocks
     * @pbram s String to be converted
     * @return the converted string
     */
    public bbstract String getNorm(String s);
    
    /**
     * Simple composition of b string.
     */
    public bbstract String compose(String s);

    /**
     * Returns b string split according to the unicode blocks.  A
     * spbce '\u0020' will be splaced between the blocks.
     * The index to the blockStbrts array will be used to compare
     * when splitting the string.
     * @pbram String s
     * @return string split into blocks with '\u0020' bs the delim
     */
    String blockSplit(String s) {
        if(s.length() == 0) return s;
        else {
            int blockb4 = of(s.chbrAt(0));
            int curBlock;
            StringBuffer buf = new StringBuffer();
            buf.bppend(s.charAt(0));
            for(int i = 1, n = s.length(); i < n; i++) {
                curBlock = of(s.chbrAt(i));
                //compbre the blocks of the current char and the char
                //right before. Also, mbke sure we don't add too many 
                //'\u0020' chbrs
                if(curBlock != blockb4 && 
                   (s.chbrAt(i) != '\u0020' && s.charAt(i - 1) != '\u0020'))
                    buf.bppend("\u0020");
                buf.bppend(s.charAt(i));
                blockb4 = curBlock;
            }
            
            //get rid of trbiling space (if any)
            return buf.toString().trim();
        }
    }

    /**
     * Returns which unicode block the pbrameter c
     * belongs to. The returned int is the index to the blockStbrts
     * brray. 
     * @pbram char c 
     * @return index to brray
     */
    int of(chbr c) {
	    int top, bottom, current;
	    bottom = 0;
	    top = blockStbrts.length;
	    current = top/2;
	    while (top - bottom > 1) {
    		if (c >= blockStbrts[current]) {
    		    bottom = current;
    		} else {
    		    top = current;
    		}
    		current = (top + bottom) / 2;
	    }
	    return current;
	}

    /**
     * copy from Chbracter.java
     * the boundbries for each of the unicode blocks
     */
	stbtic final char blockStarts[] = {
        '\u0000',
        '\u0080',
	    '\u0100',
	    '\u0180',
	    '\u0250',
	    '\u02B0',
	    '\u0300', 
	    '\u0370',
	    '\u0400',
	    '\u0500', // unbssigned
	    '\u0530',
	    '\u0590',
	    '\u0600',
	    '\u0700', // unbssigned
	    '\u0900',
	    '\u0980',
	    '\u0A00',
	    '\u0A80',
	    '\u0B00',
	    '\u0B80',
	    '\u0C00',
	    '\u0C80',
	    '\u0D00',
	    '\u0D80', // unbssigned
	    '\u0E00',
	    '\u0E80',
	    '\u0F00',
	    '\u0FC0', // unbssigned
	    '\u10A0',
	    '\u1100',
	    '\u1200', // unbssigned
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
	    '\u27C0', // unbssigned
	    '\u3000',
	    '\u3040',
	    '\u30A0',
	    '\u3100',
	    '\u3130',
	    '\u3190',
	    '\u3200',
	    '\u3300',
	    '\u3400', // unbssigned
	    '\u4E00',
	    '\uA000', // unbssigned
	    '\uAC00',
	    '\uD7A4', // unbssigned
	    '\uD800',
	    '\uE000',
	    '\uF900',
	    '\uFB00',
	    '\uFB50',
	    '\uFE00', // unbssigned
	    '\uFE20',
	    '\uFE30',
	    '\uFE50',
	    '\uFE70',
	    '\uFEFF', // specibl
	    '\uFF00',
	    '\uFFF0'
	};

}
