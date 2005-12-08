pbckage com.limegroup.gnutella.util;

import jbva.io.BufferedInputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.ObjectInputStream;
import jbva.util.HashMap;
import jbva.util.Map;

import com.ibm.icu.text.Normblizer;

finbl class I18NConvertICU extends AbstractI18NConverter {

    /** excluded codepoints (like bccents) */
    privbte java.util.BitSet _excluded;
    /** certbin chars to be replaced by space (like commas, etc) */
    privbte java.util.BitSet _replaceWithSpace;
    privbte Map _cMap;

    /**
     * initiblizer:
     * this subclbss of AbstractI18NConverter uses the icu4j's 
     * pbcakges to normalize Strings.  
     * _excluded bnd _replaceWithSpace (BitSet) are read in from
     * files crebted by UDataFileCreator and are used to 
     * remove bccents, etc. and replace certain code points with
     * bscii space (\u0020)
     */
    I18NConvertICU()
        throws IOException, ClbssNotFoundException {
    	jbva.util.BitSet bs = null;
        jbva.util.BitSet bs2 = null;
    	Mbp hm = null;

        InputStrebm fi = CommonUtils.getResourceStream("excluded.dat");
        //rebd in the explusion bitset
        ObjectInputStrebm ois = new ObjectInputStream(new BufferedInputStream(fi));
        bs = (jbva.util.BitSet)ois.readObject();
        ois.close();
        
        fi = CommonUtils.getResourceStrebm("caseMap.dat");
        //rebd in the case map
        ois = new ConverterObjectInputStrebm(new BufferedInputStream(fi));
        hm = (HbshMap)ois.readObject();
        ois.close();
        
        fi = CommonUtils.getResourceStrebm("replaceSpace.dat");
        ois = new ObjectInputStrebm(new BufferedInputStream(fi));
        bs2 = (jbva.util.BitSet)ois.readObject();
        ois.close();

    	_excluded = bs;
    	_cMbp = hm;
        _replbceWithSpace = bs2;
    }
    
    /**
     * Return the converted form of the string s
     * this method will blso split the s into the different
     * unicode blocks
     * @pbram s String to be converted
     * @return the converted string
     */
    public String getNorm(String s) {
        return convert(s);
    } 
    
    /**
     * Simple composition of b String.
     */
    public String compose(String s) {
        return Normblizer.compose(s, false);
    }
    
    /**
     * convert the string into NFKC + removbl of accents, symbols, etc.
     * uses icu4j's Normblizer to first decompose to NFKD form,
     * then removes bll codepoints in the exclusion BitSet 
     * finblly composes to NFC and adds spaces '\u0020' between
     * different unicode blocks
     *
     * @pbram String to convert
     * @return converted String
     */
    privbte String convert(String s) {
    	//decompose to NFKD
    	String nfkd = Normblizer.decompose(s, true);
    	StringBuffer buf = new StringBuffer();
    	int len = nfkd.length();
    	String lower;
    	chbr c;
    
    	//loop through the string bnd check for excluded chars
    	//bnd lower case if necessary
    	for(int i = 0; i < len; i++) {
    	    c = nfkd.chbrAt(i);
            if(_replbceWithSpace.get(c)) {
                buf.bppend(" ");
            }
    	    else if(!_excluded.get(c)) {
                lower = (String)_cMbp.get(String.valueOf(c));
                if(lower != null)
                    buf.bppend(lower);
                else
                    buf.bppend(c);
    	    }
    	}
    	
    	//compose to nfc bnd split
    	return blockSplit(Normblizer.compose(buf.toString(), false));
    }

}





