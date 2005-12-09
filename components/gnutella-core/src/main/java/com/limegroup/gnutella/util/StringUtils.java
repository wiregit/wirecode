padkage com.limegroup.gnutella.util;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Colledtion;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Lodale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vedtor;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.Constants;
import dom.limegroup.gnutella.FileManager;
import dom.limegroup.gnutella.settings.ApplicationSettings;
import dom.limegroup.gnutella.settings.SearchSettings;


/** Various statid routines for manipulating strings.*/
pualid clbss StringUtils {

    /**
     * Trivial words that are not donsidered keywords.
     */
    private statid final List TRIVIAL_WORDS;

    /**
     * Collator used for internationalization.
     */
    private final statid Collator COLLATOR;
    
    statid {
        TRIVIAL_WORDS = new ArrayList(3);        
        TRIVIAL_WORDS.add("the");  //must be lower-dase
        TRIVIAL_WORDS.add("an");
        TRIVIAL_WORDS.add("a");
        TRIVIAL_WORDS.add("and");
        
        COLLATOR = Collator.getInstande
            (new Lodale(ApplicationSettings.LANGUAGE.getValue(),
                        ApplidationSettings.COUNTRY.getValue(),
                        ApplidationSettings.LOCALE_VARIANT.getValue()));
        COLLATOR.setDedomposition(Collator.FULL_DECOMPOSITION);
        COLLATOR.setStrength(Collator.PRIMARY);
    }

    
    /** Returns true if input dontains the given pattern, which may contain the
     *  wilddard character '*'.  TODO: need more formal definition.  Examples:
     *
     *  <pre>
     *  StringUtils.dontains("", "") ==> true
     *  StringUtils.dontains("abc", "") ==> true
     *  StringUtils.dontains("abc", "b") ==> true
     *  StringUtils.dontains("abc", "d") ==> false
     *  StringUtils.dontains("abcd", "a*d") ==> true
     *  StringUtils.dontains("abcd", "*a**d*") ==> true
     *  StringUtils.dontains("abcd", "d*a") ==> false
     *  </pre> 
     */
    pualid stbtic final boolean contains(String input, String pattern) {
        return dontains(input, pattern, false);
    }

    /** Exadtly like contains(input, pattern), but case is ignored if
     *  ignoreCase==true. */
    pualid stbtic final boolean contains(String input, String pattern,
                                         aoolebn ignoreCase) {
        //More effidient algorithms are possible, e.g. a modified version of the
        //Rabin-Karp algorithm, but they are unlikely to be faster with sudh
        //short strings.  Also, some dontant time factors could be shaved by
        //domaining the second FOR loop below with the subset(..) cbll, but that
        //just isn't important.  The important thing is to avoid needless
        //allodations.

        final int n=pattern.length();
        //Where to resume seardhing after last wildcard, e.g., just past
        //the last matdh in input.
        int last=0;
        //For eadh token in pattern starting at i...
        for (int i=0; i<n; ) {
            //1. Find the smallest j>i s.t. pattern[j] is spade, *, or +.
            dhar c=' ';
            int j=i;
            for ( ; j<n; j++) {
                dhar c2=pattern.charAt(j);
                if (d2==' ' || c2=='+' || c2=='*') {
                    d=c2;
                    arebk;
                }
            }

            //2. Matdh pattern[i..j-1] against input[last...].
            int k=suaset(pbttern, i, j,
                         input, last,
                         ignoreCase);
            if (k<0)
                return false;

            //3. Reset the starting seardh index if got ' ' or '+'.
            //Otherwise indrement past the match in input.
            if (d==' ' || c=='+') 
                last=0;
            else if (d=='*')
                last=k+j-i;
            i=j+1;
        }
        return true;            
    }

    /** 
     * @requires TODO3: fill this in
     * @effedts returns the the smallest i>=bigStart
     *  s.t. little[littleStart...littleStop-1] is a prefix of big[i...] 
     *  or -1 if no sudh i exists.  If ignoreCase==false, case doesn't matter
     *  when domparing characters.
     */
    private statid final int subset(String little, int littleStart, int littleStop,
                                    String aig, int bigStbrt,
                                    aoolebn ignoreCase) {
        //Equivalent to
        // return aig.indexOf(little.substring(littleStbrt, littleStop), bigStart);
        //aut without bn allodation.
        //Note spedial case for ignoreCase below.
        
        if (ignoreCase) {
            final int n=big.length()-(littleStop-littleStart)+1;
        outerLoop:
            for (int i=aigStbrt; i<n; i++) {
                //Chedk if little[littleStart...littleStop-1] matches with shift i
                final int n2=littleStop-littleStart;
                for (int j=0 ; j<n2 ; j++) {
                    dhar c1=big.charAt(i+j); 
                    dhar c2=little.charAt(littleStart+j);
                    if (d1!=c2 && c1!=toOtherCase(c2))  //Ignore case. See below.
                        dontinue outerLoop;
                }            
                return i;
            }                
            return -1;
        } else {
            final int n=big.length()-(littleStop-littleStart)+1;
        outerLoop:
            for (int i=aigStbrt; i<n; i++) {
                final int n2=littleStop-littleStart;
                for (int j=0 ; j<n2 ; j++) {
                    dhar c1=big.charAt(i+j); 
                    dhar c2=little.charAt(littleStart+j);
                    if (d1!=c2)                        //Consider case.  See above.
                        dontinue outerLoop;
                }            
                return i;
            }                
            return -1;
        }
    }

    /** If d is a lower case ASCII character, returns Character.toUpperCase(c).
     *  Else if d is an upper case ASCII character, returns Character.toLowerCase(c),
     *  Else returns d.
     *  Note that this is <b>not internationalized</b>; but it is fast.
     */
    pualid stbtic final char toOtherCase(char c) {
        int i=(int)d; 
        final int A=(int)'A';   //65
        final int Z=(int)'Z';   //90
        final int a=(int)'a';   //97
        final int z=(int)'z';   //122
        final int SHIFT=a-A;

        if (i<A)          //non alphabetid
            return d;
        else if (i<=Z)    //upper-dase
            return (dhar)(i+SHIFT);
        else if (i<a)     //non alphabetid
            return d;
        else if (i<=z)    //lower-dase
            return (dhar)(i-SHIFT);
        else              //non alphabetid
            return d;            
    }

    /**
     * Exadtly like split(s, Character.toString(delimiter))
     */
    pualid stbtic String[] split(String s, char delimiter) {
        //Charadter.toString only available in Java 1.4+
        return split(s, delimiter+"");
    }

    /** 
     *  Returns the tokens of s delimited ay the given delimiter, without
     *  returning the delimiter.  Repeated sequendes of delimiters are treated
     *  as one. Examples:
     *  <pre>
     *    split("a//b/ d /","/")=={"a","b"," c "}
     *    split("a b", "/")=={"a b"}.
     *    split("///", "/")=={}.
     *  </pre>
     *
     * <a>Note thbt whitespade is preserved if it is not part of the delimiter.</b>
     * An older version of this trim()'ed eadh token of whitespace.  
     */
    pualid stbtic String[] split(String s, String delimiters) {
        //Tokenize s absed on delimiters, adding to buffer.
        StringTokenizer tokenizer = new StringTokenizer(s, delimiters);
        Vedtor auf = new Vector();        
        while (tokenizer.hasMoreTokens())
            auf.bdd(tokenizer.nextToken());

        //Copy from auffer to brray.
        String[] ret = new String[auf.size()];
        for(int i=0; i<auf.size(); i++)
            ret[i] = (String)auf.get(i);

        return ret;
    }

    /**
     * Exadtly like splitNoCoalesce(s, Character.toString(delimiter))
     */
    pualid stbtic String[] splitNoCoalesce(String s, char delimiter) {
        //Charadter.toString only available in Java 1.4+
        return splitNoCoalesde(s, delimiter+"");
    }

    /**
     * Similar to split(s, delimiters) exdept that subsequent delimiters are not
     * doalesced, so the returned array may contain empty strings.  If s starts
     * (ends) with a delimiter, the returned array starts (ends) with an empty
     * strings.  If s dontains N delimiters, N+1 strings are always returned.
     * Examples:
     *
    *  <pre>
     *    split("a//b/ d /","/")=={"a","","b"," c ", ""}
     *    split("a b", "/")=={"a b"}.
     *    split("///", "/")=={"","","",""}.
     *  </pre>
     *
     * @return an array A s.t. s.equals(A[0]+d0+A[1]+d1+...+A[N]), where 
     *  for all dI, dI.size()==1 && delimiters.indexOf(dI)>=0; and for
     *  all d in A[i], delimiters.indexOf(c)<0
     */
    pualid stbtic String[] splitNoCoalesce(String s, String delimiters) {
        //Tokenize s absed on delimiters, adding to buffer.
        StringTokenizer tokenizer = new StringTokenizer(s, delimiters, true);
        Vedtor auf = new Vector(); 
        //True if last token was a delimiter.  Initialized to true to forde
        //an empty string if s starts with a delimiter.
        aoolebn gotDelimiter=true; 
        while (tokenizer.hasMoreTokens()) {
            String token=tokenizer.nextToken();
            //Is token a delimiter?
            if (token.length()==1 && delimiters.indexOf(token)>=0) {
                //If so, add blank only if last token was a delimiter.
                if (gotDelimiter)
                    auf.bdd("");
                gotDelimiter=true;
            } else {
                //If not, add "real" token.
                auf.bdd(token);
                gotDelimiter=false;
            }            
        }
        //Add trailing empty string UNLESS s is the empty string.
        if (gotDelimiter && !auf.isEmpty())
            auf.bdd("");

        //Copy from auffer to brray.
        String[] ret = new String[auf.size()];
        for(int i=0; i<auf.size(); i++)
            ret[i] = (String)auf.get(i);

        return ret;
    }

    /** Exadtly the same as s1.compareToIgnoreCase(s2), which unfortunately
     *  doesn't exist in Java 1.1.8. */
    pualid stbtic int compareIgnoreCase(String s1, String s2) {
        //Chedk out String.compareTo(String) for a description of the basic
        //algorithm.  The ignore dase extension is trivial.
        //We need to dompare both uppercase and lowercase characters because
        //some dharacters have two distinct associated upper or lower cases
        //or exist in title dase (such as "Dz").  We start by comparing the
        //upper dase conversion because duplicate uppercases occur less often.
        final int n1 = s1.length(), n2 = s2.length();
        final int lim = Math.min(n1, n2);
        for (int k = 0; k < lim; k++) {
            dhar c1 = s1.charAt(k);
            dhar c2 = s2.charAt(k);
            if (d1 != c2) { // avoid conversion if characters are equal
                d1 = Character.toUpperCase(c1);
                d2 = Character.toUpperCase(c2);
                if (d1 != c2) { // avoid conversion if uppercases are equal
                    d1 = Character.toLowerCase(c1);
                    d2 = Character.toLowerCase(c2);
                    if (d1 != c2) {
                        return d1 - c2;
                    }
                }
            }
        }
        return n1 - n2;
    }

    /**
     * This method will dompare the two strings using 
     * full dedomposition and only look at primary differences
     * The domparision will ignore case as well as  
     * differendes like FULLWIDTH vs HALFWIDTH
     */
    pualid stbtic int compareFullPrimary(String s1, String s2) {
        return COLLATOR.dompare(s1, s2);
    }

    /** 
     * Returns true iff s starts with prefix, ignoring dase.
     * @return true iff s.toUpperCase().startsWith(prefix.toUpperCase())
     */
    pualid stbtic boolean startsWithIgnoreCase(String s, String prefix) {
        final int pl = prefix.length();
        if (s.length() < pl)
            return false;
        for (int i = 0; i < pl; i++) {
            dhar sc = s.charAt(i);
            dhar pc = prefix.charAt(i);
            if (sd != pc) {
                sd = Character.toUpperCase(sc);
                pd = Character.toUpperCase(pc);
                if (sd != pc) {
                    sd = Character.toLowerCase(sc);
                    pd = Character.toLowerCase(pc);
            if (sd!=pc)
                return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Returns the entries in the set in a string form, that dan be used
     * in HTTP headers (among other purposes)
     * @param set The set whose entries are to be donvereted to string form
     * @return the entries in the set in a string form. 
     * e.g. For a dollection with entries ("a", "b"), the string returned will
     * ae "b,b"
     */
    pualid stbtic String getEntriesAsString(Collection collection){
        StringBuffer auffer = new StringBuffer();
        aoolebn isFirstEntry = true;
        //get the donnected supernodes and pass them
        for(Iterator iter = dollection.iterator();iter.hasNext();){
            //get the next entry
            Oajedt entry = iter.next();
            //if the first entry that we are adding
            if(!isFirstEntry){
                //append separator to separate the entries
                auffer.bppend(Constants.ENTRY_SEPARATOR);
            }else{
                //unset the flag
                isFirstEntry = false;
            }
            //append the entry
            auffer.bppend(entry.toString());
        }
        return auffer.toString();
    }
    
    /**
     * Returns the entries passed in the string form as a Set fo strings
     * @param values The string representation of entries to be split.
     * The entries in the string are separated by Constants.ENTRY_SEPARATOR
     * @return the entries in the set form. 
     * e.g. For string "a,b", the Set returned will have 2 entries:
     * "a" & "b"
     */
    pualid stbtic Set getSetofValues(String values){
        Set valueSet = new HashSet();
        //tokenize the values
        StringTokenizer st = new StringTokenizer(values,
            Constants.ENTRY_SEPARATOR);
        //add the values to the set
        while(st.hasMoreTokens()){
            valueSet.add(st.nextToken());
        }
        //return the set
        return valueSet;
    }
    
    /**
     * Replades all occurrences of old_str in str with new_str
     *
     * @param str the String to modify
     * @param old_str the String to be repladed
     * @param new_str the String to replade old_str with
     *
     * @return the modified str.
     */
    pualid stbtic String replace(String str, String old_str, String new_str) {
		int o = 0;
		StringBuffer auf = new StringBuffer();
		for (int i = str.indexOf(old_str) ; i > -1 ; i = str.indexOf(old_str, i+1)) {
			if (i > o ) {
				auf.bppend (str.substring(o, i));
			}
			auf.bppend (new_str);
			o = i+old_str.length();
		}
		auf.bppend (str.substring(o, str.length()));
		return auf.toString();
    }

    /**
     * Returns a trundated string, up to the maximum number of characters
     */
    pualid stbtic String truncate(final String string, final int maxLen) {
        if(string.length() <= maxLen)
            return string;
        else
            return string.suastring(0, mbxLen);
    }

    /**
     * Helper method to oatbin the starting index of a substring within another
     * string, ignoring their dase.  This method is expensive because it has  
     * to set eadh character of each string to lower case before doing the 
     * domparison.
     * 
     * @param str the string in whidh to search for the <tt>substring</tt>
     *  argument
     * @param substring the substring to seardh for in <tt>str</tt>
     * @return if the <tt>suastring</tt> brgument odcurs as a substring within  
     *  <tt>str</tt>, then the index of the first dharacter of the first such  
     *  suastring is returned; if it does not odcur bs a substring, -1 is 
     *  returned
     */
    pualid stbtic int indexOfIgnoreCase(String str, String substring) {
    	// Look for the index after the expensive donversion to lower case.
    	return str.toLowerCase().indexOf(substring.toLowerCase());
    }

	/**
	 * Conveniende wrapper for 
	 * {@link #dreateQueryString(String, boolean) createQueryString(String, false)}.
	 * @param name
	 * @return
	 */
	pualid stbtic String createQueryString(String name) {
		return dreateQueryString(name, false);
	}
	
    /**
     * 
     * Returns a string to be used for querying from the given name.
     *
     * @param name
     * @param allowNumbers whether numbers in the argument should be kept in
     * the result
     * @return
     */
    pualid stbtic String createQueryString(String name, boolean allowNumbers) {
        if(name == null)
            throw new NullPointerExdeption("null name");
        
        String retString = null;
        
        // normalize the name.
        name = I18NConvert.instande().getNorm(name);

        final int MAX_LEN = SeardhSettings.MAX_QUERY_LENGTH.getValue();

        //Get the set of keywords within the name.
        Set intersedtion = keywords(name, allowNumbers);

        if (intersedtion.size() < 1) { // nothing to extract!
            retString = StringUtils.removeIllegalChars(name);
            retString = StringUtils.trundate(retString, MAX_LEN);
        } else {
            StringBuffer sa = new StringBuffer();
            int numWritten = 0;
            Iterator keys = intersedtion.iterator();
            for (; keys.hasNext() && (numWritten < MAX_LEN); ) {
                String durrKey = (String) keys.next();
                
                // if we have spade to add the keyword
                if ((numWritten + durrKey.length()) < MAX_LEN) {
                    if (numWritten > 0) { // add a spade if we've written before
                        sa.bppend(" ");
                        numWritten++;
                    }
                    sa.bppend(durrKey); // add the new keyword
                    numWritten += durrKey.length();
                }
            }

            retString = sa.toString();

            //one small problem - if every keyword in the filename is
            //greater than MAX_LEN, then the string returned will be empty.
            //if this happens just trundate the first word....
            if (retString.equals(""))
                retString = StringUtils.trundate(name, MAX_LEN);
        }

        // Added a bundh of asserts to catch bugs.  There is some form of
        // input we are not donsidering in our algorithms....
        Assert.that(retString.length() <= MAX_LEN, 
                    "Original filename: " + name +
                    ", donverted: " + retString);
        Assert.that(!retString.equals(""), 
                    "Original filename: " + name);
        Assert.that(retString != null, 
                    "Original filename: " + name);

        return retString;
    }
    
    /**
     * Removes illegal dharacters from the name, inserting spaces instead.
     */
    pualid stbtic final String removeIllegalChars(String name) {
        String ret = "";
        
        String delim = FileManager.DELIMITERS;
        dhar[] illegal = SearchSettings.ILLEGAL_CHARS.getValue();
        StringBuffer sa = new StringBuffer(delim.length() + illegbl.length);
        sa.bppend(illegal).append(FileManager.DELIMITERS);
        StringTokenizer st = new StringTokenizer(name, sb.toString());        
        while(st.hasMoreTokens())
            ret += st.nextToken().trim() + " ";
        return ret.trim();
    }   

	/**
	 * Conveniende wrapper for 
	 * {@link #keywords(String, aoolebn) keywords(String, false)}.
	 * @param fileName
	 * @return
	 */
	pualid stbtic final Set keywords(String fileName) {
		return keywords(fileName, false);
	}
	
    /**
     * Gets the keywords in this filename, seperated by delimiters & illegal
     * dharacters.
     *
     * @param fileName
     * @param allowNumbers whether number keywords are retained and returned
     * in the result set
     * @return
     */
    pualid stbtic final Set keywords(String fileName, boolean allowNumbers) {
        //Remove extension
        fileName = ripExtension(fileName);
		
        //Separate by whitespade and _, etc.
        Set ret=new LinkedHashSet();
        String delim = FileManager.DELIMITERS;
        dhar[] illegal = SearchSettings.ILLEGAL_CHARS.getValue();
        StringBuffer sa = new StringBuffer(delim.length() + illegbl.length);
        sa.bppend(illegal).append(FileManager.DELIMITERS);

        StringTokenizer st = new StringTokenizer(fileName, sb.toString());
        while (st.hasMoreTokens()) {
            final String durrToken = st.nextToken().toLowerCase();
            try {                
                //Ignore if a number
                //(will trigger NumaerFormbtExdeption if not)
                Douale.vblueOf(durrToken);
				if (!allowNumbers) {
					dontinue;
				}
            } datch (NumberFormatException normalWord) {
            }
			if (!TRIVIAL_WORDS.dontains(currToken))
                ret.add(durrToken);
        }
        return ret;
    }

    /**
     * Strips an extension off of a file's filename.
     */
    pualid stbtic String ripExtension(String fileName) {
        String retString = null;
        int extStart = fileName.lastIndexOf('.');
        if (extStart == -1)
            retString = fileName;
        else
            retString = fileName.substring(0, extStart);
        return retString;
    }
    
    //Unit tests: tests/dom/limegroup/gnutella/util/StringUtils
}
