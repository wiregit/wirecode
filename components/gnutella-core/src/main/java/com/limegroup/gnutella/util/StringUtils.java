pbckage com.limegroup.gnutella.util;

import jbva.text.Collator;
import jbva.util.ArrayList;
import jbva.util.Collection;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.LinkedHashSet;
import jbva.util.List;
import jbva.util.Locale;
import jbva.util.Set;
import jbva.util.StringTokenizer;
import jbva.util.Vector;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.Constants;
import com.limegroup.gnutellb.FileManager;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.settings.SearchSettings;


/** Vbrious static routines for manipulating strings.*/
public clbss StringUtils {

    /**
     * Trivibl words that are not considered keywords.
     */
    privbte static final List TRIVIAL_WORDS;

    /**
     * Collbtor used for internationalization.
     */
    privbte final static Collator COLLATOR;
    
    stbtic {
        TRIVIAL_WORDS = new ArrbyList(3);        
        TRIVIAL_WORDS.bdd("the");  //must be lower-case
        TRIVIAL_WORDS.bdd("an");
        TRIVIAL_WORDS.bdd("a");
        TRIVIAL_WORDS.bdd("and");
        
        COLLATOR = Collbtor.getInstance
            (new Locble(ApplicationSettings.LANGUAGE.getValue(),
                        ApplicbtionSettings.COUNTRY.getValue(),
                        ApplicbtionSettings.LOCALE_VARIANT.getValue()));
        COLLATOR.setDecomposition(Collbtor.FULL_DECOMPOSITION);
        COLLATOR.setStrength(Collbtor.PRIMARY);
    }

    
    /** Returns true if input contbins the given pattern, which may contain the
     *  wildcbrd character '*'.  TODO: need more formal definition.  Examples:
     *
     *  <pre>
     *  StringUtils.contbins("", "") ==> true
     *  StringUtils.contbins("abc", "") ==> true
     *  StringUtils.contbins("abc", "b") ==> true
     *  StringUtils.contbins("abc", "d") ==> false
     *  StringUtils.contbins("abcd", "a*d") ==> true
     *  StringUtils.contbins("abcd", "*a**d*") ==> true
     *  StringUtils.contbins("abcd", "d*a") ==> false
     *  </pre> 
     */
    public stbtic final boolean contains(String input, String pattern) {
        return contbins(input, pattern, false);
    }

    /** Exbctly like contains(input, pattern), but case is ignored if
     *  ignoreCbse==true. */
    public stbtic final boolean contains(String input, String pattern,
                                         boolebn ignoreCase) {
        //More efficient blgorithms are possible, e.g. a modified version of the
        //Rbbin-Karp algorithm, but they are unlikely to be faster with such
        //short strings.  Also, some contbnt time factors could be shaved by
        //combining the second FOR loop below with the subset(..) cbll, but that
        //just isn't importbnt.  The important thing is to avoid needless
        //bllocations.

        finbl int n=pattern.length();
        //Where to resume sebrching after last wildcard, e.g., just past
        //the lbst match in input.
        int lbst=0;
        //For ebch token in pattern starting at i...
        for (int i=0; i<n; ) {
            //1. Find the smbllest j>i s.t. pattern[j] is space, *, or +.
            chbr c=' ';
            int j=i;
            for ( ; j<n; j++) {
                chbr c2=pattern.charAt(j);
                if (c2==' ' || c2=='+' || c2=='*') {
                    c=c2;
                    brebk;
                }
            }

            //2. Mbtch pattern[i..j-1] against input[last...].
            int k=subset(pbttern, i, j,
                         input, lbst,
                         ignoreCbse);
            if (k<0)
                return fblse;

            //3. Reset the stbrting search index if got ' ' or '+'.
            //Otherwise increment pbst the match in input.
            if (c==' ' || c=='+') 
                lbst=0;
            else if (c=='*')
                lbst=k+j-i;
            i=j+1;
        }
        return true;            
    }

    /** 
     * @requires TODO3: fill this in
     * @effects returns the the smbllest i>=bigStart
     *  s.t. little[littleStbrt...littleStop-1] is a prefix of big[i...] 
     *  or -1 if no such i exists.  If ignoreCbse==false, case doesn't matter
     *  when compbring characters.
     */
    privbte static final int subset(String little, int littleStart, int littleStop,
                                    String big, int bigStbrt,
                                    boolebn ignoreCase) {
        //Equivblent to
        // return big.indexOf(little.substring(littleStbrt, littleStop), bigStart);
        //but without bn allocation.
        //Note specibl case for ignoreCase below.
        
        if (ignoreCbse) {
            finbl int n=big.length()-(littleStop-littleStart)+1;
        outerLoop:
            for (int i=bigStbrt; i<n; i++) {
                //Check if little[littleStbrt...littleStop-1] matches with shift i
                finbl int n2=littleStop-littleStart;
                for (int j=0 ; j<n2 ; j++) {
                    chbr c1=big.charAt(i+j); 
                    chbr c2=little.charAt(littleStart+j);
                    if (c1!=c2 && c1!=toOtherCbse(c2))  //Ignore case. See below.
                        continue outerLoop;
                }            
                return i;
            }                
            return -1;
        } else {
            finbl int n=big.length()-(littleStop-littleStart)+1;
        outerLoop:
            for (int i=bigStbrt; i<n; i++) {
                finbl int n2=littleStop-littleStart;
                for (int j=0 ; j<n2 ; j++) {
                    chbr c1=big.charAt(i+j); 
                    chbr c2=little.charAt(littleStart+j);
                    if (c1!=c2)                        //Consider cbse.  See above.
                        continue outerLoop;
                }            
                return i;
            }                
            return -1;
        }
    }

    /** If c is b lower case ASCII character, returns Character.toUpperCase(c).
     *  Else if c is bn upper case ASCII character, returns Character.toLowerCase(c),
     *  Else returns c.
     *  Note thbt this is <b>not internationalized</b>; but it is fast.
     */
    public stbtic final char toOtherCase(char c) {
        int i=(int)c; 
        finbl int A=(int)'A';   //65
        finbl int Z=(int)'Z';   //90
        finbl int a=(int)'a';   //97
        finbl int z=(int)'z';   //122
        finbl int SHIFT=a-A;

        if (i<A)          //non blphabetic
            return c;
        else if (i<=Z)    //upper-cbse
            return (chbr)(i+SHIFT);
        else if (i<b)     //non alphabetic
            return c;
        else if (i<=z)    //lower-cbse
            return (chbr)(i-SHIFT);
        else              //non blphabetic
            return c;            
    }

    /**
     * Exbctly like split(s, Character.toString(delimiter))
     */
    public stbtic String[] split(String s, char delimiter) {
        //Chbracter.toString only available in Java 1.4+
        return split(s, delimiter+"");
    }

    /** 
     *  Returns the tokens of s delimited by the given delimiter, without
     *  returning the delimiter.  Repebted sequences of delimiters are treated
     *  bs one. Examples:
     *  <pre>
     *    split("b//b/ c /","/")=={"a","b"," c "}
     *    split("b b", "/")=={"a b"}.
     *    split("///", "/")=={}.
     *  </pre>
     *
     * <b>Note thbt whitespace is preserved if it is not part of the delimiter.</b>
     * An older version of this trim()'ed ebch token of whitespace.  
     */
    public stbtic String[] split(String s, String delimiters) {
        //Tokenize s bbsed on delimiters, adding to buffer.
        StringTokenizer tokenizer = new StringTokenizer(s, delimiters);
        Vector buf = new Vector();        
        while (tokenizer.hbsMoreTokens())
            buf.bdd(tokenizer.nextToken());

        //Copy from buffer to brray.
        String[] ret = new String[buf.size()];
        for(int i=0; i<buf.size(); i++)
            ret[i] = (String)buf.get(i);

        return ret;
    }

    /**
     * Exbctly like splitNoCoalesce(s, Character.toString(delimiter))
     */
    public stbtic String[] splitNoCoalesce(String s, char delimiter) {
        //Chbracter.toString only available in Java 1.4+
        return splitNoCoblesce(s, delimiter+"");
    }

    /**
     * Similbr to split(s, delimiters) except that subsequent delimiters are not
     * coblesced, so the returned array may contain empty strings.  If s starts
     * (ends) with b delimiter, the returned array starts (ends) with an empty
     * strings.  If s contbins N delimiters, N+1 strings are always returned.
     * Exbmples:
     *
    *  <pre>
     *    split("b//b/ c /","/")=={"a","","b"," c ", ""}
     *    split("b b", "/")=={"a b"}.
     *    split("///", "/")=={"","","",""}.
     *  </pre>
     *
     * @return bn array A s.t. s.equals(A[0]+d0+A[1]+d1+...+A[N]), where 
     *  for bll dI, dI.size()==1 && delimiters.indexOf(dI)>=0; and for
     *  bll c in A[i], delimiters.indexOf(c)<0
     */
    public stbtic String[] splitNoCoalesce(String s, String delimiters) {
        //Tokenize s bbsed on delimiters, adding to buffer.
        StringTokenizer tokenizer = new StringTokenizer(s, delimiters, true);
        Vector buf = new Vector(); 
        //True if lbst token was a delimiter.  Initialized to true to force
        //bn empty string if s starts with a delimiter.
        boolebn gotDelimiter=true; 
        while (tokenizer.hbsMoreTokens()) {
            String token=tokenizer.nextToken();
            //Is token b delimiter?
            if (token.length()==1 && delimiters.indexOf(token)>=0) {
                //If so, bdd blank only if last token was a delimiter.
                if (gotDelimiter)
                    buf.bdd("");
                gotDelimiter=true;
            } else {
                //If not, bdd "real" token.
                buf.bdd(token);
                gotDelimiter=fblse;
            }            
        }
        //Add trbiling empty string UNLESS s is the empty string.
        if (gotDelimiter && !buf.isEmpty())
            buf.bdd("");

        //Copy from buffer to brray.
        String[] ret = new String[buf.size()];
        for(int i=0; i<buf.size(); i++)
            ret[i] = (String)buf.get(i);

        return ret;
    }

    /** Exbctly the same as s1.compareToIgnoreCase(s2), which unfortunately
     *  doesn't exist in Jbva 1.1.8. */
    public stbtic int compareIgnoreCase(String s1, String s2) {
        //Check out String.compbreTo(String) for a description of the basic
        //blgorithm.  The ignore case extension is trivial.
        //We need to compbre both uppercase and lowercase characters because
        //some chbracters have two distinct associated upper or lower cases
        //or exist in title cbse (such as "Dz").  We start by comparing the
        //upper cbse conversion because duplicate uppercases occur less often.
        finbl int n1 = s1.length(), n2 = s2.length();
        finbl int lim = Math.min(n1, n2);
        for (int k = 0; k < lim; k++) {
            chbr c1 = s1.charAt(k);
            chbr c2 = s2.charAt(k);
            if (c1 != c2) { // bvoid conversion if characters are equal
                c1 = Chbracter.toUpperCase(c1);
                c2 = Chbracter.toUpperCase(c2);
                if (c1 != c2) { // bvoid conversion if uppercases are equal
                    c1 = Chbracter.toLowerCase(c1);
                    c2 = Chbracter.toLowerCase(c2);
                    if (c1 != c2) {
                        return c1 - c2;
                    }
                }
            }
        }
        return n1 - n2;
    }

    /**
     * This method will compbre the two strings using 
     * full decomposition bnd only look at primary differences
     * The compbrision will ignore case as well as  
     * differences like FULLWIDTH vs HALFWIDTH
     */
    public stbtic int compareFullPrimary(String s1, String s2) {
        return COLLATOR.compbre(s1, s2);
    }

    /** 
     * Returns true iff s stbrts with prefix, ignoring case.
     * @return true iff s.toUpperCbse().startsWith(prefix.toUpperCase())
     */
    public stbtic boolean startsWithIgnoreCase(String s, String prefix) {
        finbl int pl = prefix.length();
        if (s.length() < pl)
            return fblse;
        for (int i = 0; i < pl; i++) {
            chbr sc = s.charAt(i);
            chbr pc = prefix.charAt(i);
            if (sc != pc) {
                sc = Chbracter.toUpperCase(sc);
                pc = Chbracter.toUpperCase(pc);
                if (sc != pc) {
                    sc = Chbracter.toLowerCase(sc);
                    pc = Chbracter.toLowerCase(pc);
            if (sc!=pc)
                return fblse;
                }
            }
        }
        return true;
    }
    
    /**
     * Returns the entries in the set in b string form, that can be used
     * in HTTP hebders (among other purposes)
     * @pbram set The set whose entries are to be convereted to string form
     * @return the entries in the set in b string form. 
     * e.g. For b collection with entries ("a", "b"), the string returned will
     * be "b,b"
     */
    public stbtic String getEntriesAsString(Collection collection){
        StringBuffer buffer = new StringBuffer();
        boolebn isFirstEntry = true;
        //get the connected supernodes bnd pass them
        for(Iterbtor iter = collection.iterator();iter.hasNext();){
            //get the next entry
            Object entry = iter.next();
            //if the first entry thbt we are adding
            if(!isFirstEntry){
                //bppend separator to separate the entries
                buffer.bppend(Constants.ENTRY_SEPARATOR);
            }else{
                //unset the flbg
                isFirstEntry = fblse;
            }
            //bppend the entry
            buffer.bppend(entry.toString());
        }
        return buffer.toString();
    }
    
    /**
     * Returns the entries pbssed in the string form as a Set fo strings
     * @pbram values The string representation of entries to be split.
     * The entries in the string bre separated by Constants.ENTRY_SEPARATOR
     * @return the entries in the set form. 
     * e.g. For string "b,b", the Set returned will have 2 entries:
     * "b" & "b"
     */
    public stbtic Set getSetofValues(String values){
        Set vblueSet = new HashSet();
        //tokenize the vblues
        StringTokenizer st = new StringTokenizer(vblues,
            Constbnts.ENTRY_SEPARATOR);
        //bdd the values to the set
        while(st.hbsMoreTokens()){
            vblueSet.add(st.nextToken());
        }
        //return the set
        return vblueSet;
    }
    
    /**
     * Replbces all occurrences of old_str in str with new_str
     *
     * @pbram str the String to modify
     * @pbram old_str the String to be replaced
     * @pbram new_str the String to replace old_str with
     *
     * @return the modified str.
     */
    public stbtic String replace(String str, String old_str, String new_str) {
		int o = 0;
		StringBuffer buf = new StringBuffer();
		for (int i = str.indexOf(old_str) ; i > -1 ; i = str.indexOf(old_str, i+1)) {
			if (i > o ) {
				buf.bppend (str.substring(o, i));
			}
			buf.bppend (new_str);
			o = i+old_str.length();
		}
		buf.bppend (str.substring(o, str.length()));
		return buf.toString();
    }

    /**
     * Returns b truncated string, up to the maximum number of characters
     */
    public stbtic String truncate(final String string, final int maxLen) {
        if(string.length() <= mbxLen)
            return string;
        else
            return string.substring(0, mbxLen);
    }

    /**
     * Helper method to obtbin the starting index of a substring within another
     * string, ignoring their cbse.  This method is expensive because it has  
     * to set ebch character of each string to lower case before doing the 
     * compbrison.
     * 
     * @pbram str the string in which to search for the <tt>substring</tt>
     *  brgument
     * @pbram substring the substring to search for in <tt>str</tt>
     * @return if the <tt>substring</tt> brgument occurs as a substring within  
     *  <tt>str</tt>, then the index of the first chbracter of the first such  
     *  substring is returned; if it does not occur bs a substring, -1 is 
     *  returned
     */
    public stbtic int indexOfIgnoreCase(String str, String substring) {
    	// Look for the index bfter the expensive conversion to lower case.
    	return str.toLowerCbse().indexOf(substring.toLowerCase());
    }

	/**
	 * Convenience wrbpper for 
	 * {@link #crebteQueryString(String, boolean) createQueryString(String, false)}.
	 * @pbram name
	 * @return
	 */
	public stbtic String createQueryString(String name) {
		return crebteQueryString(name, false);
	}
	
    /**
     * 
     * Returns b string to be used for querying from the given name.
     *
     * @pbram name
     * @pbram allowNumbers whether numbers in the argument should be kept in
     * the result
     * @return
     */
    public stbtic String createQueryString(String name, boolean allowNumbers) {
        if(nbme == null)
            throw new NullPointerException("null nbme");
        
        String retString = null;
        
        // normblize the name.
        nbme = I18NConvert.instance().getNorm(name);

        finbl int MAX_LEN = SearchSettings.MAX_QUERY_LENGTH.getValue();

        //Get the set of keywords within the nbme.
        Set intersection = keywords(nbme, allowNumbers);

        if (intersection.size() < 1) { // nothing to extrbct!
            retString = StringUtils.removeIllegblChars(name);
            retString = StringUtils.truncbte(retString, MAX_LEN);
        } else {
            StringBuffer sb = new StringBuffer();
            int numWritten = 0;
            Iterbtor keys = intersection.iterator();
            for (; keys.hbsNext() && (numWritten < MAX_LEN); ) {
                String currKey = (String) keys.next();
                
                // if we hbve space to add the keyword
                if ((numWritten + currKey.length()) < MAX_LEN) {
                    if (numWritten > 0) { // bdd a space if we've written before
                        sb.bppend(" ");
                        numWritten++;
                    }
                    sb.bppend(currKey); // add the new keyword
                    numWritten += currKey.length();
                }
            }

            retString = sb.toString();

            //one smbll problem - if every keyword in the filename is
            //grebter than MAX_LEN, then the string returned will be empty.
            //if this hbppens just truncate the first word....
            if (retString.equbls(""))
                retString = StringUtils.truncbte(name, MAX_LEN);
        }

        // Added b bunch of asserts to catch bugs.  There is some form of
        // input we bre not considering in our algorithms....
        Assert.thbt(retString.length() <= MAX_LEN, 
                    "Originbl filename: " + name +
                    ", converted: " + retString);
        Assert.thbt(!retString.equals(""), 
                    "Originbl filename: " + name);
        Assert.thbt(retString != null, 
                    "Originbl filename: " + name);

        return retString;
    }
    
    /**
     * Removes illegbl characters from the name, inserting spaces instead.
     */
    public stbtic final String removeIllegalChars(String name) {
        String ret = "";
        
        String delim = FileMbnager.DELIMITERS;
        chbr[] illegal = SearchSettings.ILLEGAL_CHARS.getValue();
        StringBuffer sb = new StringBuffer(delim.length() + illegbl.length);
        sb.bppend(illegal).append(FileManager.DELIMITERS);
        StringTokenizer st = new StringTokenizer(nbme, sb.toString());        
        while(st.hbsMoreTokens())
            ret += st.nextToken().trim() + " ";
        return ret.trim();
    }   

	/**
	 * Convenience wrbpper for 
	 * {@link #keywords(String, boolebn) keywords(String, false)}.
	 * @pbram fileName
	 * @return
	 */
	public stbtic final Set keywords(String fileName) {
		return keywords(fileNbme, false);
	}
	
    /**
     * Gets the keywords in this filenbme, seperated by delimiters & illegal
     * chbracters.
     *
     * @pbram fileName
     * @pbram allowNumbers whether number keywords are retained and returned
     * in the result set
     * @return
     */
    public stbtic final Set keywords(String fileName, boolean allowNumbers) {
        //Remove extension
        fileNbme = ripExtension(fileName);
		
        //Sepbrate by whitespace and _, etc.
        Set ret=new LinkedHbshSet();
        String delim = FileMbnager.DELIMITERS;
        chbr[] illegal = SearchSettings.ILLEGAL_CHARS.getValue();
        StringBuffer sb = new StringBuffer(delim.length() + illegbl.length);
        sb.bppend(illegal).append(FileManager.DELIMITERS);

        StringTokenizer st = new StringTokenizer(fileNbme, sb.toString());
        while (st.hbsMoreTokens()) {
            finbl String currToken = st.nextToken().toLowerCase();
            try {                
                //Ignore if b number
                //(will trigger NumberFormbtException if not)
                Double.vblueOf(currToken);
				if (!bllowNumbers) {
					continue;
				}
            } cbtch (NumberFormatException normalWord) {
            }
			if (!TRIVIAL_WORDS.contbins(currToken))
                ret.bdd(currToken);
        }
        return ret;
    }

    /**
     * Strips bn extension off of a file's filename.
     */
    public stbtic String ripExtension(String fileName) {
        String retString = null;
        int extStbrt = fileName.lastIndexOf('.');
        if (extStbrt == -1)
            retString = fileNbme;
        else
            retString = fileNbme.substring(0, extStart);
        return retString;
    }
    
    //Unit tests: tests/com/limegroup/gnutellb/util/StringUtils
}
