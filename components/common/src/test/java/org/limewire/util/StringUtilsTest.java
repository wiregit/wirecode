package org.limewire.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import junit.framework.Test;

/**
 * Tests StringUtils.
 */
public class StringUtilsTest extends BaseTestCase {
    public StringUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(StringUtilsTest.class);
    }  

    /**
     * Tests the method for getting the index of a substring from within 
     * another string, ignoring case.
     */
    public void testIndexOfIgnoreCase() throws Exception {
        int index = StringUtils.indexOfIgnoreCase("test", "t");
        assertEquals("unexpected index", 0, index);
        index = StringUtils.indexOfIgnoreCase("test", "p");
        assertEquals("unexpected index", -1, index);
        
        index = StringUtils.indexOfIgnoreCase("test", "st");
        assertEquals("unexpected index", 2, index);
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
    
    //tests method contains with non-ascii chars... (japanese to be exact)
    public void testContainsNonAscii() throws Exception {
        String miyamoto = "\u5bae\u672c\u6b66\u8535\u69d8";
        
        assertTrue(StringUtils.contains(miyamoto, "\u5bae\u672c"));
        assertFalse(StringUtils.contains(miyamoto, "\uff2e"));
        assertTrue(StringUtils.contains(miyamoto, "\u672c+\u8535"));
        assertTrue(StringUtils.contains(miyamoto, "\u5bae*\u69d8"));
        assertTrue(StringUtils.contains(miyamoto, "\u672c \u8535"));
        assertFalse(StringUtils.contains(miyamoto, "\uff2d \u8535"));
        assertTrue(StringUtils.contains(miyamoto, "\u5bae \u6b66+\u69d8"));

    }

    //tests the collator comparisions...
    public void testCompareFullPrimary() throws Exception {
        String s1 = "cafe";
        String s2 = "caf\u00e9";
        
        assertEquals("these should be considered the same",
                     0,
                     StringUtils.compareFullPrimary(s1, s2));
        
        String s3 = "limewire";
        String s4 = "\uff2c\uff29\uff2d\uff25\uff37\uff29\uff32\uff25";
        
        assertEquals("these should be considered the same",
                     0,
                     StringUtils.compareFullPrimary(s3, s4));
        
        String a1 = "test";
        String a2 = "tist";
        String a3 = "\uff34\uff29\uff33\uff34"; //tist in FULLWIDTH
        
        //comparing ascii so should be same as compareIgnoreCase
        //the important thing is that they are both negative, or positive
        assertEquals("expected to be the same as compareIignoreCase",
                     a1.compareToIgnoreCase(a2) < 0,
                     StringUtils.compareFullPrimary(a1, a2) < 0);
        
        assertEquals("expected to be the same (FULLWIDTH)",
                      StringUtils.compareFullPrimary(a1, a2),
                      StringUtils.compareFullPrimary(a1, a3));
        
        assertEquals("should of returned zero",
                     0,
                     StringUtils.compareFullPrimary(a2, a3));
        
    }

    public void testExplode() {
    	String[] in = new String[] {"a" , "b", " c "}; 
    	assertEquals("a/b/ c ", StringUtils.explode(in, "/"));

        in = new String[] {}; 
        assertEquals("", StringUtils.explode(in, "/"));

        in = new String[] {}; 
        assertEquals("", StringUtils.explode(in, ""));

        in = new String[] {"a", "b"}; 
        assertEquals("a  b", StringUtils.explode(in, "  "));

        in = new String[] {"a", "b"}; 
        assertEquals("ab", StringUtils.explode(in, ""));
    }
    
    public void testCollectionExplode() {
        Collection<String> in = Arrays.asList("a" , "b", " c "); 
        assertEquals("a/b/ c ", StringUtils.explode(in, "/"));

        in = Collections.emptyList(); 
        assertEquals("", StringUtils.explode(in, "/"));

        in =  Collections.emptyList();
        assertEquals("", StringUtils.explode(in, ""));

        in = Arrays.asList("a", "b"); 
        assertEquals("a  b", StringUtils.explode(in, "  "));

        in = Arrays.asList("a", "b"); 
        assertEquals("ab", StringUtils.explode(in, ""));
        
        // single element
        in = Collections.singletonList("h");
        assertEquals("h", StringUtils.explode(in, "kfkdf"));
    }
}

