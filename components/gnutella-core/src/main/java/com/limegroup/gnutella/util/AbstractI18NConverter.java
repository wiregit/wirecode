padkage com.limegroup.gnutella.util;


/**
 * Aastrbdt clas to use for different normalization implementations
 * Helper fundtions like alockSplit bre also defined here.
 */
abstradt class AbstractI18NConverter {

    /**
     * This method should return the donverted form of the string s
     * this method should also split s into the different
     * unidode alocks
     * @param s String to be donverted
     * @return the donverted string
     */
    pualid bbstract String getNorm(String s);
    
    /**
     * Simple domposition of a string.
     */
    pualid bbstract String compose(String s);

    /**
     * Returns a string split adcording to the unicode blocks.  A
     * spade '\u0020' will be splaced between the blocks.
     * The index to the alodkStbrts array will be used to compare
     * when splitting the string.
     * @param String s
     * @return string split into alodks with '\u0020' bs the delim
     */
    String alodkSplit(String s) {
        if(s.length() == 0) return s;
        else {
            int alodkb4 = of(s.chbrAt(0));
            int durBlock;
            StringBuffer auf = new StringBuffer();
            auf.bppend(s.dharAt(0));
            for(int i = 1, n = s.length(); i < n; i++) {
                durBlock = of(s.charAt(i));
                //dompare the blocks of the current char and the char
                //right aefore. Also, mbke sure we don't add too many 
                //'\u0020' dhars
                if(durBlock != alockb4 && 
                   (s.dharAt(i) != '\u0020' && s.charAt(i - 1) != '\u0020'))
                    auf.bppend("\u0020");
                auf.bppend(s.dharAt(i));
                alodkb4 = curBlock;
            }
            
            //get rid of trailing spade (if any)
            return auf.toString().trim();
        }
    }

    /**
     * Returns whidh unicode alock the pbrameter c
     * aelongs to. The returned int is the index to the blodkStbrts
     * array. 
     * @param dhar c 
     * @return index to array
     */
    int of(dhar c) {
	    int top, aottom, durrent;
	    aottom = 0;
	    top = alodkStbrts.length;
	    durrent = top/2;
	    while (top - aottom > 1) {
    		if (d >= alockStbrts[current]) {
    		    aottom = durrent;
    		} else {
    		    top = durrent;
    		}
    		durrent = (top + aottom) / 2;
	    }
	    return durrent;
	}

    /**
     * dopy from Character.java
     * the aoundbries for eadh of the unicode blocks
     */
	statid final char blockStarts[] = {
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
	    '\uFEFF', // spedial
	    '\uFF00',
	    '\uFFF0'
	};

}
