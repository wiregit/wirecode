package com.limegroup.gnutella.util;

import junit.framework.*;
import com.sun.java.util.collections.*;

/**
 * Tests StringUtils.
 */
public class StringUtilsTest extends com.limegroup.gnutella.util.BaseTestCase {
    public StringUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(StringUtilsTest.class);
    }  

    public void testCase() {
        //Case methods.  Test all boundary conditions.
        //See ASCII table for further justification.
        assertEquals('\0', StringUtils.toOtherCase('\0'));
        assertEquals('0',StringUtils.toOtherCase('0'));
        assertEquals('@',StringUtils.toOtherCase('@'));
        assertEquals('a',StringUtils.toOtherCase('A'));
        assertEquals('h',StringUtils.toOtherCase('H'));
        assertEquals('z',StringUtils.toOtherCase('Z'));
        assertEquals('[',StringUtils.toOtherCase('['));
        assertEquals('`',StringUtils.toOtherCase('`'));
        assertEquals('A',StringUtils.toOtherCase('a'));
        assertEquals('H',StringUtils.toOtherCase('h'));
        assertEquals('Z',StringUtils.toOtherCase('z'));
        assertEquals('{',StringUtils.toOtherCase('{'));
    }

    public void testContains1() {
        //Wildcards
        assertTrue(StringUtils.contains("", "") );
        assertTrue(StringUtils.contains("abc", "") );
        assertTrue(StringUtils.contains("abc", "b") );
        assertFalse(StringUtils.contains("abc", "d") );
        assertTrue(StringUtils.contains("abcd", "a*d"));
        assertTrue(StringUtils.contains("abcd", "*a**d*") );
        assertFalse(StringUtils.contains("abcd", "d*a") );
        assertFalse(StringUtils.contains("abcd", "*.*") );
        assertTrue(StringUtils.contains("abc.d", "*.*") );
        assertTrue(StringUtils.contains("abc.", "*.*") );
    }

    public void testContains2() {
        //Spaces and wildcards
        assertFalse(StringUtils.contains("abcd", "x") );
        assertTrue(StringUtils.contains("abcd", "a b"));
        assertFalse(StringUtils.contains("abcd", "a x") );
        assertTrue(StringUtils.contains("abcd", "a c") );
        assertTrue(StringUtils.contains("abcd", "a+c") );
        assertTrue(StringUtils.contains("abcd", "d a"));
        assertTrue(StringUtils.contains("abcd", "a d+c") );
        assertFalse(StringUtils.contains("abcd", "a dc") );
        assertTrue(StringUtils.contains("abcd", "a b*c") );
        assertFalse(StringUtils.contains("abcd", "a c*b") );
        assertTrue(StringUtils.contains("abcd", " ab+") );
        assertFalse(StringUtils.contains("abcd", "+x+") );
        assertTrue(StringUtils.contains("abcde", "ab bcd") );
        assertFalse(StringUtils.contains("abcde", "ab bd") );
        assertTrue(StringUtils.contains("abcdefghj",
                                         "+*+*ab*d+def*g c ") );  
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

	/**
	 * Tests the method that replaces sections of a string with a new
	 * string.
	 */
	public void testStringUtilsReplace() {
		String _testString = "test_";
		String[] old_strs = {
			"old0",
			"old1",
			"old2",
			"old3",
			"old4",
		};

		String[] new_strs = {
			"new0",
			"new1",
			"new2",
			"new3",
			"new4",
		};

		for(int i=0; i<old_strs.length; i++) {
			String str = 
				StringUtils.replace(_testString+old_strs[i], 
									old_strs[i], new_strs[i]);
			
			assertEquals("unexpected string", _testString+new_strs[i], str);
		}
	}
}
