package com.limegroup.gnutella.util;

import java.io.UnsupportedEncodingException;
import java.text.Collator;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.SearchSettings;


/** Various static routines for manipulating strings.*/
public class StringUtils {

    /**
     * Trivial words that are not considered keywords.
     */
    private static final List<String> TRIVIAL_WORDS;

    /**
     * Collator used for internationalization.
     */
    private final static Collator COLLATOR;
    
    static {
        // must be lower-case
        TRIVIAL_WORDS = Arrays.asList("the", "an", "a", "and");
        
        COLLATOR = Collator.getInstance
            (new Locale(ApplicationSettings.LANGUAGE.getValue(),
                        ApplicationSettings.COUNTRY.getValue(),
                        ApplicationSettings.LOCALE_VARIANT.getValue()));
        COLLATOR.setDecomposition(Collator.FULL_DECOMPOSITION);
        COLLATOR.setStrength(Collator.PRIMARY);
    }

    
    /** Returns true if input contains the given pattern, which may contain the
     *  wildcard character '*'.  TODO: need more formal definition.  Examples:
     *
     *  <pre>
     *  StringUtils.contains("", "") ==> true
     *  StringUtils.contains("abc", "") ==> true
     *  StringUtils.contains("abc", "b") ==> true
     *  StringUtils.contains("abc", "d") ==> false
     *  StringUtils.contains("abcd", "a*d") ==> true
     *  StringUtils.contains("abcd", "*a**d*") ==> true
     *  StringUtils.contains("abcd", "d*a") ==> false
     *  </pre> 
     */
    public static final boolean contains(String input, String pattern) {
        return contains(input, pattern, false);
    }

    /** Exactly like contains(input, pattern), but case is ignored if
     *  ignoreCase==true. */
    public static final boolean contains(String input, String pattern,
                                         boolean ignoreCase) {
        //More efficient algorithms are possible, e.g. a modified version of the
        //Rabin-Karp algorithm, but they are unlikely to be faster with such
        //short strings.  Also, some contant time factors could be shaved by
        //combining the second FOR loop below with the subset(..) call, but that
        //just isn't important.  The important thing is to avoid needless
        //allocations.

        final int n=pattern.length();
        //Where to resume searching after last wildcard, e.g., just past
        //the last match in input.
        int last=0;
        //For each token in pattern starting at i...
        for (int i=0; i<n; ) {
            //1. Find the smallest j>i s.t. pattern[j] is space, *, or +.
            char c=' ';
            int j=i;
            for ( ; j<n; j++) {
                char c2=pattern.charAt(j);
                if (c2==' ' || c2=='+' || c2=='*') {
                    c=c2;
                    break;
                }
            }

            //2. Match pattern[i..j-1] against input[last...].
            int k=subset(pattern, i, j,
                         input, last,
                         ignoreCase);
            if (k<0)
                return false;

            //3. Reset the starting search index if got ' ' or '+'.
            //Otherwise increment past the match in input.
            if (c==' ' || c=='+') 
                last=0;
            else if (c=='*')
                last=k+j-i;
            i=j+1;
        }
        return true;            
    }
    
    public static boolean containsCharacters(String input, char [] chars) {
        char [] inputChars = input.toCharArray();
        Arrays.sort(inputChars);
        for(int i=0; i<chars.length; i++) {
            if(Arrays.binarySearch(inputChars, chars[i]) >= 0) return true;
        }
        return false;
    }

    /** 
     * @requires TODO3: fill this in
     * @effects returns the the smallest i>=bigStart
     *  s.t. little[littleStart...littleStop-1] is a prefix of big[i...] 
     *  or -1 if no such i exists.  If ignoreCase==false, case doesn't matter
     *  when comparing characters.
     */
    private static final int subset(String little, int littleStart, int littleStop,
                                    String big, int bigStart,
                                    boolean ignoreCase) {
        //Equivalent to
        // return big.indexOf(little.substring(littleStart, littleStop), bigStart);
        //but without an allocation.
        //Note special case for ignoreCase below.
        
        if (ignoreCase) {
            final int n=big.length()-(littleStop-littleStart)+1;
        outerLoop:
            for (int i=bigStart; i<n; i++) {
                //Check if little[littleStart...littleStop-1] matches with shift i
                final int n2=littleStop-littleStart;
                for (int j=0 ; j<n2 ; j++) {
                    char c1=big.charAt(i+j); 
                    char c2=little.charAt(littleStart+j);
                    if (c1!=c2 && c1!=toOtherCase(c2))  //Ignore case. See below.
                        continue outerLoop;
                }            
                return i;
            }                
            return -1;
        } else {
            final int n=big.length()-(littleStop-littleStart)+1;
        outerLoop:
            for (int i=bigStart; i<n; i++) {
                final int n2=littleStop-littleStart;
                for (int j=0 ; j<n2 ; j++) {
                    char c1=big.charAt(i+j); 
                    char c2=little.charAt(littleStart+j);
                    if (c1!=c2)                        //Consider case.  See above.
                        continue outerLoop;
                }            
                return i;
            }                
            return -1;
        }
    }

    /** If c is a lower case ASCII character, returns Character.toUpperCase(c).
     *  Else if c is an upper case ASCII character, returns Character.toLowerCase(c),
     *  Else returns c.
     *  Note that this is <b>not internationalized</b>; but it is fast.
     */
    public static final char toOtherCase(char c) {
        int i = c; 
        final int A= 'A';   //65
        final int Z= 'Z';   //90
        final int a= 'a';   //97
        final int z= 'z';   //122
        final int SHIFT=a-A;

        if (i<A)          //non alphabetic
            return c;
        else if (i<=Z)    //upper-case
            return (char)(i+SHIFT);
        else if (i<a)     //non alphabetic
            return c;
        else if (i<=z)    //lower-case
            return (char)(i-SHIFT);
        else              //non alphabetic
            return c;            
    }

    /**
     * Exactly like split(s, Character.toString(delimiter))
     */
    public static String[] split(String s, char delimiter) {
        //Character.toString only available in Java 1.4+
        return split(s, delimiter+"");
    }

    /** 
     *  Returns the tokens of s delimited by the given delimiter, without
     *  returning the delimiter.  Repeated sequences of delimiters are treated
     *  as one. Examples:
     *  <pre>
     *    split("a//b/ c /","/")=={"a","b"," c "}
     *    split("a b", "/")=={"a b"}.
     *    split("///", "/")=={}.
     *  </pre>
     *
     * <b>Note that whitespace is preserved if it is not part of the delimiter.</b>
     * An older version of this trim()'ed each token of whitespace.  
     */
    public static String[] split(String s, String delimiters) {
        //Tokenize s based on delimiters, adding to buffer.
        StringTokenizer tokenizer = new StringTokenizer(s, delimiters);
        Vector<String> buf = new Vector<String>();        
        while (tokenizer.hasMoreTokens())
            buf.add(tokenizer.nextToken());

        //Copy from buffer to array.
        String[] ret = new String[buf.size()];
        for(int i=0; i<buf.size(); i++)
            ret[i] = buf.get(i);

        return ret;
    }

    /**
     * Exactly like splitNoCoalesce(s, Character.toString(delimiter))
     */
    public static String[] splitNoCoalesce(String s, char delimiter) {
        //Character.toString only available in Java 1.4+
        return splitNoCoalesce(s, delimiter+"");
    }

    /**
     * Similar to split(s, delimiters) except that subsequent delimiters are not
     * coalesced, so the returned array may contain empty strings.  If s starts
     * (ends) with a delimiter, the returned array starts (ends) with an empty
     * strings.  If s contains N delimiters, N+1 strings are always returned.
     * Examples:
     *
    *  <pre>
     *    split("a//b/ c /","/")=={"a","","b"," c ", ""}
     *    split("a b", "/")=={"a b"}.
     *    split("///", "/")=={"","","",""}.
     *  </pre>
     *
     * @return an array A s.t. s.equals(A[0]+d0+A[1]+d1+...+A[N]), where 
     *  for all dI, dI.size()==1 && delimiters.indexOf(dI)>=0; and for
     *  all c in A[i], delimiters.indexOf(c)<0
     */
    public static String[] splitNoCoalesce(String s, String delimiters) {
        //Tokenize s based on delimiters, adding to buffer.
        StringTokenizer tokenizer = new StringTokenizer(s, delimiters, true);
        Vector<String> buf = new Vector<String>(); 
        //True if last token was a delimiter.  Initialized to true to force
        //an empty string if s starts with a delimiter.
        boolean gotDelimiter=true; 
        while (tokenizer.hasMoreTokens()) {
            String token=tokenizer.nextToken();
            //Is token a delimiter?
            if (token.length()==1 && delimiters.indexOf(token)>=0) {
                //If so, add blank only if last token was a delimiter.
                if (gotDelimiter)
                    buf.add("");
                gotDelimiter=true;
            } else {
                //If not, add "real" token.
                buf.add(token);
                gotDelimiter=false;
            }            
        }
        //Add trailing empty string UNLESS s is the empty string.
        if (gotDelimiter && !buf.isEmpty())
            buf.add("");

        //Copy from buffer to array.
        String[] ret = new String[buf.size()];
        for(int i=0; i<buf.size(); i++)
            ret[i] = buf.get(i);

        return ret;
    }

    /** Exactly the same as s1.compareToIgnoreCase(s2), which unfortunately
     *  doesn't exist in Java 1.1.8. */
    public static int compareIgnoreCase(String s1, String s2) {
        //Check out String.compareTo(String) for a description of the basic
        //algorithm.  The ignore case extension is trivial.
        //We need to compare both uppercase and lowercase characters because
        //some characters have two distinct associated upper or lower cases
        //or exist in title case (such as "Dz").  We start by comparing the
        //upper case conversion because duplicate uppercases occur less often.
        final int n1 = s1.length(), n2 = s2.length();
        final int lim = Math.min(n1, n2);
        for (int k = 0; k < lim; k++) {
            char c1 = s1.charAt(k);
            char c2 = s2.charAt(k);
            if (c1 != c2) { // avoid conversion if characters are equal
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
                if (c1 != c2) { // avoid conversion if uppercases are equal
                    c1 = Character.toLowerCase(c1);
                    c2 = Character.toLowerCase(c2);
                    if (c1 != c2) {
                        return c1 - c2;
                    }
                }
            }
        }
        return n1 - n2;
    }

    /**
     * This method will compare the two strings using 
     * full decomposition and only look at primary differences
     * The comparision will ignore case as well as  
     * differences like FULLWIDTH vs HALFWIDTH
     */
    public static int compareFullPrimary(String s1, String s2) {
        return COLLATOR.compare(s1, s2);
    }

    /** 
     * Returns true iff s starts with prefix, ignoring case.
     * @return true iff s.toUpperCase().startsWith(prefix.toUpperCase())
     */
    public static boolean startsWithIgnoreCase(String s, String prefix) {
        final int pl = prefix.length();
        if (s.length() < pl)
            return false;
        for (int i = 0; i < pl; i++) {
            char sc = s.charAt(i);
            char pc = prefix.charAt(i);
            if (sc != pc) {
                sc = Character.toUpperCase(sc);
                pc = Character.toUpperCase(pc);
                if (sc != pc) {
                    sc = Character.toLowerCase(sc);
                    pc = Character.toLowerCase(pc);
            if (sc!=pc)
                return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Replaces all occurrences of old_str in str with new_str
     *
     * @param str the String to modify
     * @param old_str the String to be replaced
     * @param new_str the String to replace old_str with
     *
     * @return the modified str.
     */
    public static String replace(String str, String old_str, String new_str) {
		int o = 0;
        StringBuilder buf = new StringBuilder();
		for (int i = str.indexOf(old_str) ; i > -1 ; i = str.indexOf(old_str, i+1)) {
			if (i > o ) {
				buf.append (str.substring(o, i));
			}
			buf.append (new_str);
			o = i+old_str.length();
		}
		buf.append (str.substring(o, str.length()));
		return buf.toString();
    }

    /**
     * Returns a truncated string, up to the maximum number of characters
     */
    public static String truncate(final String string, final int maxLen) {
        if(string.length() <= maxLen)
            return string;
        else
            return string.substring(0, maxLen);
    }

    /**
     * Helper method to obtain the starting index of a substring within another
     * string, ignoring their case.  This method is expensive because it has  
     * to set each character of each string to lower case before doing the 
     * comparison.
     * 
     * @param str the string in which to search for the <tt>substring</tt>
     *  argument
     * @param substring the substring to search for in <tt>str</tt>
     * @return if the <tt>substring</tt> argument occurs as a substring within  
     *  <tt>str</tt>, then the index of the first character of the first such  
     *  substring is returned; if it does not occur as a substring, -1 is 
     *  returned
     */
    public static int indexOfIgnoreCase(String str, String substring) {
    	// Look for the index after the expensive conversion to lower case.
    	return str.toLowerCase().indexOf(substring.toLowerCase());
    }

	/**
	 * Convenience wrapper for 
	 * {@link #createQueryString(String, boolean) createQueryString(String, false)}.
	 * @param name
	 * @return
	 */
	public static String createQueryString(String name) {
		return createQueryString(name, false);
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
    public static String createQueryString(String name, boolean allowNumbers) {
        if(name == null)
            throw new NullPointerException("null name");
        
        String retString = null;
        
        // normalize the name.
        name = I18NConvert.instance().getNorm(name);

        final int MAX_LEN = SearchSettings.MAX_QUERY_LENGTH.getValue();

        //Get the set of keywords within the name.
        Set<String> intersection = keywords(name, allowNumbers);

        if (intersection.size() < 1) { // nothing to extract!
            retString = StringUtils.removeIllegalChars(name);
            retString = StringUtils.truncate(retString, MAX_LEN);
        } else {
            StringBuilder sb = new StringBuilder();
            int numWritten = 0;
            for(String currKey : intersection) {
                if(numWritten >= MAX_LEN)
                    break;
                
                // if we have space to add the keyword
                if ((numWritten + currKey.length()) < MAX_LEN) {
                    if (numWritten > 0) { // add a space if we've written before
                        sb.append(" ");
                        numWritten++;
                    }
                    sb.append(currKey); // add the new keyword
                    numWritten += currKey.length();
                }
            }

            retString = sb.toString();

            //one small problem - if every keyword in the filename is
            //greater than MAX_LEN, then the string returned will be empty.
            //if this happens just truncate the first word....
            if (retString.equals(""))
                retString = StringUtils.truncate(name, MAX_LEN);
        }

        // Added a bunch of asserts to catch bugs.  There is some form of
        // input we are not considering in our algorithms....
        Assert.that(retString.length() <= MAX_LEN, 
                    "Original filename: " + name +
                    ", converted: " + retString);
        
        if(!intersection.isEmpty())
            Assert.that(!retString.equals(""), "Original filename: " + name);
            
        Assert.that(retString != null, 
                    "Original filename: " + name);

        return retString;
    }
    
    /**
     * Removes illegal characters from the name, inserting spaces instead.
     */
    public static final String removeIllegalChars(String name) {
        String ret = "";
        
        String delim = FileManager.DELIMITERS;
        char[] illegal = SearchSettings.ILLEGAL_CHARS.getValue();
        StringBuilder sb = new StringBuilder(delim.length() + illegal.length);
        sb.append(illegal).append(FileManager.DELIMITERS);
        StringTokenizer st = new StringTokenizer(name, sb.toString());        
        while(st.hasMoreTokens())
            ret += st.nextToken().trim() + " ";
        return ret.trim();
    }   

	/**
	 * Convenience wrapper for 
	 * {@link #keywords(String, boolean) keywords(String, false)}.
	 * @param fileName
	 * @return
	 */
	public static final Set<String> keywords(String fileName) {
		return keywords(fileName, false);
	}
	
    /**
     * Gets the keywords in this filename, seperated by delimiters & illegal
     * characters.
     *
     * @param fileName
     * @param allowNumbers whether number keywords are retained and returned
     * in the result set
     * @return
     */
    public static final Set<String> keywords(String fileName, boolean allowNumbers) {
        //Remove extension
        fileName = ripExtension(fileName);
		
        //Separate by whitespace and _, etc.
        Set<String> ret=new LinkedHashSet<String>();
        String delim = FileManager.DELIMITERS;
        char[] illegal = SearchSettings.ILLEGAL_CHARS.getValue();
        StringBuilder sb = new StringBuilder(delim.length() + illegal.length);
        sb.append(illegal).append(FileManager.DELIMITERS);

        StringTokenizer st = new StringTokenizer(fileName, sb.toString());
        while (st.hasMoreTokens()) {
            final String currToken = st.nextToken().toLowerCase();
            try {                
                //Ignore if a number
                //(will trigger NumberFormatException if not)
                Double.valueOf(currToken);
				if (!allowNumbers) {
					continue;
				}
            } catch (NumberFormatException normalWord) {
            }
			if (!TRIVIAL_WORDS.contains(currToken))
                ret.add(currToken);
        }
        return ret;
    }

    /**
     * Strips an extension off of a file's filename.
     */
    public static String ripExtension(String fileName) {
        String retString = null;
        int extStart = fileName.lastIndexOf('.');
        if (extStart == -1)
            retString = fileName;
        else
            retString = fileName.substring(0, extStart);
        return retString;
    }
    
    /**
     * Utility wrapper for getting a String object out of
     * byte [] using the ascii encoding.
     */
    public static String getASCIIString(byte [] bytes) {
    	return getEncodedString(bytes, Constants.ASCII_ENCODING);
    }
    
    /**
     * Utility wrapper for getting a String object out of
     * byte [] using the UTF-8 encoding.
     */
    public static String getUTF8String(byte [] bytes) {
    	return getEncodedString(bytes, Constants.UTF_8_ENCODING);
    }
    
    /**
     * @return a string with an encoding we know we support.
     */
    private static String getEncodedString(byte [] bytes, String encoding) {
    	try {
    		return new String(bytes, encoding);
    	} catch (UnsupportedEncodingException impossible) {
    		ErrorService.error(impossible);
    		return null;
    	}
    }
    
    //Unit tests: tests/com/limegroup/gnutella/util/StringUtils
}
