package com.limegroup.gnutella.util;

import junit.framework.*;
import com.sun.java.util.collections.*;

/**
 * Tests StringUtils.
 */
public class StringUtilsTest extends TestCase {
    public StringUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(StringUtilsTest.class);
    }  

    public void testCase() {
        //Case methods.  Test all boundary conditions.
        //See ASCII table for further justification.
        assertTrue(StringUtils.toOtherCase('\0')=='\0');
        assertTrue(StringUtils.toOtherCase('0')=='0');
        assertTrue(StringUtils.toOtherCase('@')=='@');
        assertTrue(StringUtils.toOtherCase('A')=='a');
        assertTrue(StringUtils.toOtherCase('H')=='h');
        assertTrue(StringUtils.toOtherCase('Z')=='z');
        assertTrue(StringUtils.toOtherCase('[')=='[');
        assertTrue(StringUtils.toOtherCase('`')=='`');
        assertTrue(StringUtils.toOtherCase('a')=='A');
        assertTrue(StringUtils.toOtherCase('h')=='H');
        assertTrue(StringUtils.toOtherCase('z')=='Z');
        assertTrue(StringUtils.toOtherCase('{')=='{');        
    }

    public void testContains1() {
        //Wildcards
        assertTrue(StringUtils.contains("", "") == true);
        assertTrue(StringUtils.contains("abc", "") == true);
        assertTrue(StringUtils.contains("abc", "b") == true);
        assertTrue(StringUtils.contains("abc", "d") == false);
        assertTrue(StringUtils.contains("abcd", "a*d") == true);
        assertTrue(StringUtils.contains("abcd", "*a**d*") == true);
        assertTrue(StringUtils.contains("abcd", "d*a") == false);
        assertTrue(StringUtils.contains("abcd", "*.*") == false);
        assertTrue(StringUtils.contains("abc.d", "*.*") == true);
        assertTrue(StringUtils.contains("abc.", "*.*") == true);
    }

    public void testContains2() {
        //Spaces and wildcards
        assertTrue(StringUtils.contains("abcd", "x") == false);
        assertTrue(StringUtils.contains("abcd", "a b") == true);
        assertTrue(StringUtils.contains("abcd", "a x") == false);
        assertTrue(StringUtils.contains("abcd", "a c") == true);
        assertTrue(StringUtils.contains("abcd", "a+c") == true);
        assertTrue(StringUtils.contains("abcd", "d a") == true);
        assertTrue(StringUtils.contains("abcd", "a d+c") == true);
        assertTrue(StringUtils.contains("abcd", "a dc") == false);
        assertTrue(StringUtils.contains("abcd", "a b*c") == true);
        assertTrue(StringUtils.contains("abcd", "a c*b") == false);
        assertTrue(StringUtils.contains("abcd", " ab+") == true);
        assertTrue(StringUtils.contains("abcd", "+x+") == false);
        assertTrue(StringUtils.contains("abcde", "ab bcd") == true);
        assertTrue(StringUtils.contains("abcde", "ab bd") == false);
        assertTrue(StringUtils.contains("abcdefghj",
                                         "+*+*ab*d+def*g c ") == true);  
    }

    public void testContainsCase() {
        //Cases 
        assertTrue(StringUtils.contains("aBcDd", "bCD", true) == true);
        assertTrue(StringUtils.contains("aBcDd", "bCD", false) == false);
        assertTrue(StringUtils.contains("....", "..", true) == true);
        assertTrue(StringUtils.contains("....", "..", false) == true);
    }

    public void testContainsClip2() {
        //Clip2 compatibility      
        assertTrue(StringUtils.contains("abcd", " ") == true);
        assertTrue(StringUtils.contains("abcd", "    ") == true);
    }
        
    public void testSplit() {
        //split tests
        String in;
        String[] expected;
        String[] result;
        
        in="a//b/ c /"; expected=new String[] {"a","b"," c "};
        result=StringUtils.split(in, '/');
        assertTrue(Arrays.equals(expected, result));
        
        in="a b";       expected=new String[] {"a b"};
        result=StringUtils.split(in, '/');
        assertTrue(Arrays.equals(expected, result));
        
        in="///";       expected=new String[] {};
        result=StringUtils.split(in, '/');
        assertTrue(Arrays.equals(expected, result));

        in="a+b|c|+d+|";     expected=new String[] {"a", "b", "c", "d"};
        result=StringUtils.split(in, "+|");
        assertTrue(Arrays.equals(expected, result));

        in="";       expected=new String[] {};
        result=StringUtils.split(in, '/');
        assertTrue(Arrays.equals(expected, result));
    }

        
    public void testSplitNoCoalesce() {
        //split tests
        String in;
        String[] expected;
        String[] result;
  
        in="a//b/ c "; expected=new String[] {"a","","b"," c "};
        result=StringUtils.splitNoCoalesce(in, '/');
        assertTrue(Arrays.equals(expected, result));
      
        in="a//b/ c /"; expected=new String[] {"a","","b"," c ",""};
        result=StringUtils.splitNoCoalesce(in, '/');
        assertTrue(Arrays.equals(expected, result));
  
        in="/a//b/ c "; expected=new String[] {"","a","","b"," c "};
        result=StringUtils.splitNoCoalesce(in, '/');
        assertTrue(Arrays.equals(expected, result));
     
        in="a b";       expected=new String[] {"a b"};
        result=StringUtils.splitNoCoalesce(in, '/');
        assertTrue(Arrays.equals(expected, result));
        
        in="///";       expected=new String[] {"", "", "", ""};
        result=StringUtils.splitNoCoalesce(in, '/');
        assertTrue(Arrays.equals(expected, result));

        in="a+b|c|+d+|"; expected=new String[] {"a", "b", "c", "", "d", "", ""};
        result=StringUtils.splitNoCoalesce(in, "+|");
        assertTrue(Arrays.equals(expected, result));

        in="";       expected=new String[] {};
        result=StringUtils.splitNoCoalesce(in, '/');
        assertTrue(Arrays.equals(expected, result));        
    }

    public void testCompareIgnoreCase() {
        //Unit tests for compareToIgnoreCase.  These require Java 2.
        doCompareIgnoreCase("", "");
        doCompareIgnoreCase("", "b");
        doCompareIgnoreCase("a", "");
        doCompareIgnoreCase("abc", "AbC");
        doCompareIgnoreCase("aXc", "ayc");
        doCompareIgnoreCase("aXc", "aYc");
        doCompareIgnoreCase("axc", "ayc");
        doCompareIgnoreCase("axc", "aYc");
        doCompareIgnoreCase("abc", "AbCdef");
    }

    private static void doCompareIgnoreCase(String a, String b) {
        assertTrue(a.compareToIgnoreCase(b)
                   ==StringUtils.compareIgnoreCase(a, b));
        assertTrue(b.compareToIgnoreCase(a)
                   ==StringUtils.compareIgnoreCase(b, a));
    }

    public void testStartsWithIgnoreCase() {
        assertTrue(StringUtils.startsWithIgnoreCase("abcd", "a"));
        assertTrue(StringUtils.startsWithIgnoreCase("aBcd", "Ab"));
        assertTrue(StringUtils.startsWithIgnoreCase("abcd", ""));
        assertTrue(StringUtils.startsWithIgnoreCase("a", ""));
        assertTrue(StringUtils.startsWithIgnoreCase("", ""));

        assertTrue(! StringUtils.startsWithIgnoreCase("abcd", "x"));
        assertTrue(! StringUtils.startsWithIgnoreCase("a", "ab"));
        assertTrue(! StringUtils.startsWithIgnoreCase("", "a"));
    }
}
