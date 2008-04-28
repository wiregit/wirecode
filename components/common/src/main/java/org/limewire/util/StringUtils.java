package org.limewire.util;

import java.io.UnsupportedEncodingException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;


/**
 * Provides static methods to split, check for substrings, change case and
 * compare strings, along with additional string utility methods.
 */
public class StringUtils {

    /**
     * Collator used for internationalization.
     */
    private volatile static Collator COLLATOR;
    
    static {
        COLLATOR = Collator.getInstance(Locale.getDefault());
        COLLATOR.setDecomposition(Collator.FULL_DECOMPOSITION);
        COLLATOR.setStrength(Collator.PRIMARY);
    }
    
    /** Updates the locale that string-matching will use. */
    public static void setLocale(Locale locale) {
        Collator later = Collator.getInstance(locale);
        later.setDecomposition(Collator.FULL_DECOMPOSITION);
        later.setStrength(Collator.PRIMARY);
        COLLATOR = later;
    }

    /**
     * Returns true if input contains the given pattern, which may contain the
     *  wildcard character '*'.  TODO: need more formal definition.  Examples:
     *
     *  <pre>
     *  StringUtils.contains(&quot;&quot;, &quot;&quot;) ==&gt; true
     *  StringUtils.contains(&quot;abc&quot;, &quot;&quot;) ==&gt; true
     *  StringUtils.contains(&quot;abc&quot;, &quot;b&quot;) ==&gt; true
     *  StringUtils.contains(&quot;abc&quot;, &quot;d&quot;) ==&gt; false
     *  StringUtils.contains(&quot;abcd&quot;, &quot;a*d&quot;) ==&gt; true
     *  StringUtils.contains(&quot;abcd&quot;, &quot;*a**d*&quot;) ==&gt; true
     *  StringUtils.contains(&quot;abcd&quot;, &quot;d*a&quot;) ==&gt; false
     *  </pre> 
     */
    public static final boolean contains(String input, String pattern) {
        return contains(input, pattern, false);
    }

    /**
     * Exactly like contains(input, pattern), but case is ignored if
     * ignoreCase==true.
     */
    public static final boolean contains(String input, String pattern, boolean ignoreCase) {
        // More efficient algorithms are possible, e.g. a modified version of
        // the
        //Rabin-Karp algorithm, but they are unlikely to be faster with such
        //short strings.  Also, some contant time factors could be shaved by
        // combining the second FOR loop below with the subset(..) call, but
        // that
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
            int k = subset(pattern, i, j, input, last, ignoreCase);
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
            if (Arrays.binarySearch(inputChars, chars[i]) >= 0)
                return true;
        }
        return false;
    }

    /** 
     * @requires TODO3: fill this in
     * @effects returns the the smallest i>=bigStart s.t.
     *          little[littleStart...littleStop-1] is a prefix of big[i...] or
     *          -1 if no such i exists. If ignoreCase==false, case doesn't
     *          matter when comparing characters.
     */
    private static final int subset(String little, int littleStart, int littleStop, String big,
            int bigStart, boolean ignoreCase) {
        //Equivalent to
        // return big.indexOf(little.substring(littleStart, littleStop),
        // bigStart);
        //but without an allocation.
        //Note special case for ignoreCase below.
        
        if (ignoreCase) {
            final int n=big.length()-(littleStop-littleStart)+1;
            outerLoop: for (int i = bigStart; i < n; i++) {
                // Check if little[littleStart...littleStop-1] matches with
                // shift i
                final int n2=littleStop-littleStart;
                for (int j=0 ; j<n2 ; j++) {
                    char c1=big.charAt(i+j); 
                    char c2=little.charAt(littleStart+j);
                    if (c1 != c2 && c1 != toOtherCase(c2)) // Ignore case. See
                                                            // below.
                        continue outerLoop;
                }            
                return i;
            }                
            return -1;
        } else {
            final int n=big.length()-(littleStop-littleStart)+1;
            outerLoop: for (int i = bigStart; i < n; i++) {
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

    /**
     * If c is a lower case ASCII character, returns Character.toUpperCase(c).
     * Else if c is an upper case ASCII character, returns
     * Character.toLowerCase(c), Else returns c. Note that this is <b>not
     * internationalized</b>; but it is fast.
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
        else
            // non alphabetic
            return c;            
    }

    /**
     * Exactly like split(s, Character.toString(delimiter))
     */
    public static String[] split(String s, char delimiter) {
        return split(s, Character.toString(delimiter));
    }

    /** 
     *  Returns the tokens of s delimited by the given delimiter, without
     * returning the delimiter. Repeated sequences of delimiters are treated as
     * one. Examples:
     * 
     *  <pre>
     *    split(&quot;a//b/ c /&quot;,&quot;/&quot;)=={&quot;a&quot;,&quot;b&quot;,&quot; c &quot;}
     *    split(&quot;a b&quot;, &quot;/&quot;)=={&quot;a b&quot;}.
     *    split(&quot;///&quot;, &quot;/&quot;)=={}.
     *  </pre>
     *
     * <b>Note that whitespace is preserved if it is not part of the delimiter.</b>
     * An older version of this trim()'ed each token of whitespace.  
     */
    public static String[] split(String s, String delimiters) {
        //Tokenize s based on delimiters, adding to buffer.
        StringTokenizer tokenizer = new StringTokenizer(s, delimiters);
        List<String> tokens = new ArrayList<String>();        
        while (tokenizer.hasMoreTokens())
            tokens.add(tokenizer.nextToken());

        return tokens.toArray(new String[0]);
    }

    /**
     * Exactly like splitNoCoalesce(s, Character.toString(delimiter))
     */
    public static String[] splitNoCoalesce(String s, char delimiter) {
        return splitNoCoalesce(s, Character.toString(delimiter));
    }

    /**
     * Similar to split(s, delimiters) except that subsequent delimiters are not
     * coalesced, so the returned array may contain empty strings.  If s starts
     * (ends) with a delimiter, the returned array starts (ends) with an empty
     * strings.  If s contains N delimiters, N+1 strings are always returned.
     * Examples:
     *
    *  <pre>
     *    split(&quot;a//b/ c /&quot;,&quot;/&quot;)=={&quot;a&quot;,&quot;&quot;,&quot;b&quot;,&quot; c &quot;, &quot;&quot;}
     *    split(&quot;a b&quot;, &quot;/&quot;)=={&quot;a b&quot;}.
     *    split(&quot;///&quot;, &quot;/&quot;)=={&quot;&quot;,&quot;&quot;,&quot;&quot;,&quot;&quot;}.
     *  </pre>
     *
     * @return an array A s.t. s.equals(A[0]+d0+A[1]+d1+...+A[N]), where for all
     *         dI, dI.size()==1 && delimiters.indexOf(dI)>=0; and for all c in
     *         A[i], delimiters.indexOf(c)<0
     */
    public static String[] splitNoCoalesce(String s, String delimiters) {
        //Tokenize s based on delimiters, adding to buffer.
        StringTokenizer tokenizer = new StringTokenizer(s, delimiters, true);
        List<String> tokens = new ArrayList<String>(); 
        //True if last token was a delimiter.  Initialized to true to force
        //an empty string if s starts with a delimiter.
        boolean gotDelimiter=true; 
        while (tokenizer.hasMoreTokens()) {
            String token=tokenizer.nextToken();
            //Is token a delimiter?
            if (token.length()==1 && delimiters.indexOf(token)>=0) {
                //If so, add blank only if last token was a delimiter.
                if (gotDelimiter)
                    tokens.add("");
                gotDelimiter=true;
            } else {
                //If not, add "real" token.
                tokens.add(token);
                gotDelimiter=false;
            }            
        }
        //Add trailing empty string UNLESS s is the empty string.
        if (gotDelimiter && !tokens.isEmpty())
            tokens.add("");

        return tokens.toArray(new String[0]);
    }

    /**
     * This method will compare the two strings using full decomposition and
     * only look at primary differences The comparision will ignore case as well
     * as differences like FULLWIDTH vs HALFWIDTH
     */
    public static int compareFullPrimary(String s1, String s2) {
        return COLLATOR.compare(s1, s2);
    }

    /** 
     * Returns true iff s starts with prefix, ignoring case.
     * 
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
     * string, ignoring their case. This method is expensive because it has to
     * set each character of each string to lower case before doing the
     * comparison.  Uses the default <code>Locale</code> for case conversion.
     *
     * @param str the string in which to search for the <tt>substring</tt>
     *  argument
     * @param substring the substring to search for in <tt>str</tt>
     * @return if the <tt>substring</tt> argument occurs as a substring within
     *         <tt>str</tt>, then the index of the first character of the
     *         first such substring is returned; if it does not occur as a
     *         substring, -1 is returned
     */
    public static int indexOfIgnoreCase(String str, String substring) {
    	return indexOfIgnoreCase(str, substring, Locale.getDefault());
    }

    /**
     * Helper method to obtain the starting index of a substring within another
     * string, ignoring their case. This method is expensive because it has to
     * set each character of each string to lower case before doing the
     * comparison.
     * 
     * @param str the string in which to search for the <tt>substring</tt>
     *  argument
     * @param substring the substring to search for in <tt>str</tt>
     * @param locale the <code>Locale</code> to use when converting the case
     *        of <code>str</code> and <code>substring</code>. This is
     *        necessary because case conversion is <code>Locale</code>
     *        specific.
     * @return if the <tt>substring</tt> argument occurs as a substring within  
     *         <tt>str</tt>, then the index of the first character of the
     *         first such substring is returned; if it does not occur as a
     *         substring, -1 is returned
     */
    public static int indexOfIgnoreCase(String str, String substring, Locale locale) {
    	// Look for the index after the expensive conversion to lower case.
    	return str.toLowerCase(locale).indexOf(substring.toLowerCase(locale));
    }

	/**
     * Utility wrapper for getting a String object out of byte [] using the
     * ascii encoding.
     */
    public static String getASCIIString(byte [] bytes) {
    	return getEncodedString(bytes, "ISO-8859-1");
    }
    
    /**
     * Utility wrapper for getting a String object out of byte [] using the
     * UTF-8 encoding.
     */
    public static String getUTF8String(byte [] bytes) {
    	return getEncodedString(bytes, "UTF-8");
    }
    
    /**
     * @return a string with an encoding we know we support.
     */
    private static String getEncodedString(byte [] bytes, String encoding) {
    	try {
    		return new String(bytes, encoding);
    	} catch (UnsupportedEncodingException impossible) {
            throw new RuntimeException(impossible);
    	}
    }
    
    /**
	 * Returns the tokens of array concanated to a delimited by the given
	 * delimiter, without Examples:
	 * 
	 * <pre>
     *     explode({ &quot;a&quot;, &quot;b&quot; }, &quot; &quot;) == &quot;a b&quot;
     *     explode({ &quot;a&quot;, &quot;b&quot; }, &quot;&quot;) == &quot;ab&quot;
	 * </pre>
	 */
    public static String explode(String[] array, String delimeter) {
		StringBuilder sb = new StringBuilder();
		if (array.length > 0) {
			sb.append(array[0]);
			for (int i = 1; i < array.length; i++) {
				sb.append(delimeter);
				sb.append(array[i]);
			}
		}
		return sb.toString();
    }
    
    /**
     * Returns the tokens of a collection concanated to a delimited by the given
     * delimiter.
     */
    public static String explode(Collection<String> collection, String delimiter) {
        StringBuilder sb = new StringBuilder();
        if (!collection.isEmpty()) {
            Iterator<String> i = collection.iterator(); 
            sb.append(i.next());
            while (i.hasNext()) {
                sb.append(delimiter);
                sb.append(i.next());
            }
        }
        return sb.toString();
    }
    
    //Unit tests: tests/com/limegroup/gnutella/util/StringUtils

    /**
     * A wrapped version of {@link String#getBytes(String)} that changes the
     * unlikely encoding exception into a runtime exception. Returns empty array
     * if the passed in string is null.
     */
    public static byte[] toUTF8Bytes(String string) {
        if (string == null)
            return new byte[0];
        try {
            return string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported?", ex);
        }
    }

    /**
     * A wrapped version of {@link String#String(byte[], String)} that changes
     * the unlikely encoding exception into a runtime exception. Returns null if
     * the passed in array is null.
     */
    public static String toStringFromUTF8Bytes(byte[] bytes) {
        if (bytes == null)
            return null;
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported?", ex);
        }
    }

}
