package com.limegroup.gnutella.routing;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;

/**
 * Unit tests for HashFunction
 */
public class HashFunctionTest extends BaseTestCase {
        
	public HashFunctionTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(HashFunctionTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testNormal() {
        //1. Basic HashFunction.hash tests.  These unit tests were generated by the reference
        //implementation of HashFunction.  Some I've checked manually.
        assertEquals(0, HashFunction.hash("", (byte)13));
        assertEquals(6791, HashFunction.hash("eb", (byte)13));
        assertEquals(7082, HashFunction.hash("ebc", (byte)13));
        assertEquals(6698, HashFunction.hash("ebck", (byte)13));
        assertEquals(3179, HashFunction.hash("ebckl", (byte)13));
        assertEquals(3235, HashFunction.hash("ebcklm", (byte)13));
        assertEquals(6438, HashFunction.hash("ebcklme", (byte)13));
        assertEquals(1062, HashFunction.hash("ebcklmen", (byte)13));
        assertEquals(3527, HashFunction.hash("ebcklmenq", (byte)13));
        assertEquals(0, HashFunction.hash("", (byte)16));
        assertEquals(65003, HashFunction.hash("n", (byte)16));
        assertEquals(54193, HashFunction.hash("nd", (byte)16));
        assertEquals(4953, HashFunction.hash("ndf", (byte)16));
        assertEquals(58201, HashFunction.hash("ndfl", (byte)16));
        assertEquals(34830, HashFunction.hash("ndfla", (byte)16));
        assertEquals(36910, HashFunction.hash("ndflal", (byte)16));
        assertEquals(34586, HashFunction.hash("ndflale", (byte)16));
        assertEquals(37658, HashFunction.hash("ndflalem", (byte)16));
        assertEquals(45559, HashFunction.hash("ndflaleme", (byte)16));
        assertEquals(318, HashFunction.hash("ol2j34lj", (byte)10));
        assertEquals(503, HashFunction.hash("asdfas23", (byte)10));
        assertEquals(758, HashFunction.hash("9um3o34fd", (byte)10));
        assertEquals(281, HashFunction.hash("a234d", (byte)10));
        assertEquals(767, HashFunction.hash("a3f", (byte)10));
        assertEquals(581, HashFunction.hash("3nja9", (byte)10));
        assertEquals(146, HashFunction.hash("2459345938032343", (byte)10));
        assertEquals(342, HashFunction.hash("7777a88a8a8a8", (byte)10));
        assertEquals(861, HashFunction.hash("asdfjklkj3k", (byte)10));
        assertEquals(1011, HashFunction.hash("adfk32l", (byte)10));
        assertEquals(944, HashFunction.hash("zzzzzzzzzzz", (byte)10));
    }
    
    public void testOffset() {
        //2. Offset tests.
        assertEquals(58201, HashFunction.hash("ndfl", 0, 4, (byte)16));
        assertEquals(58201, HashFunction.hash("_ndfl_", 1, 1+4, (byte)16));
        assertEquals(58201, HashFunction.hash("__ndfl__", 2, 2+4, (byte)16));
        assertEquals(58201, HashFunction.hash("___ndfl___", 3, 3+4, (byte)16));
    }
    
    public void testCase() {
        //3. Case tests.
        assertEquals(581, HashFunction.hash("3nja9", (byte)10));
        assertEquals(581, HashFunction.hash("3NJA9", (byte)10));
        assertEquals(581, HashFunction.hash("3nJa9", (byte)10));
    }

    /**
     * Test the keywords splitting function.
     */
    public void testKeywords() {
        String path1 = 
            "/home/test/this_is_gReAt_teSt/WE_ArE_herE_tO_stAy.txt";
        String[] splitPath1 = 
        {"home", "test", "this", "is", "great", "test",
         "we", "are", "here", "to", "stay", "txt"};

        String path2 = 
            "/home/test/\u00e2cc\u00e8nts/r\u00c8mov\u00cb_th\u00eas\u00c8_th\u00cengs.txt";
        String[] splitPath2 = 
        {"home", "test", "accents", "remove", "these", "things", "txt"};
            
        String path3 = 
            "/home/test/\u9234\u6728\u305f\u308d\u3046/\u30c6\u30b9\u30c8\u306e\u30d5\u30a1\u30a4\u30eb\uff3f\uff34\uff25\uff33\uff34\uff3f\uff26\uff29\uff2c\uff25.txt";
        String[] splitPath3 =
        {"home", "test", "\u9234\u6728", "\u305f\u308d\u3046", 
         "\u30c6\u30b9\u30c8", "\u306e", "\u30d5\u30a1\u30a4\u30eb",
         "test", "file", "txt"};
        
        
        String[] split = HashFunction.keywords(path1);
        assertEquals("number of keywords should be " + splitPath1.length, 
                     splitPath1.length, 
                     split.length);
        assertTrue("the keywords don't match : path1", 
                   compareStringArray(splitPath1, split));

        split = HashFunction.keywords(path2);
        assertEquals("number of keywords should be " + splitPath2.length, 
                     splitPath2.length,
                     split.length);
        assertTrue("the keywords don't match : path2",
                   compareStringArray(splitPath2, split));

        split = HashFunction.keywords(path3);
        assertEquals("number of keywords should be " + splitPath3.length, 
                     splitPath3.length,
                     split.length);
        assertTrue("the keywords don't match : path3",
                   compareStringArray(splitPath3, split));
    }

    /**
     * test that proper prefixes are returned.
     */
    public void testPrefixes() {

        String[] sp = HashFunction.getPrefixes(new String[]{"hey", "hey2"});
        assertTrue("should only have returned 2 string", 
                   sp.length == 2);
        
        String[] prefixedExpected = 
        {"goods", "good", "goo", 
         "woods", "wood", "woo",
         "snoozes", "snooze", "snooz"};
        
        sp = 
        HashFunction.getPrefixes(new String[]{"goods", "woods", "snoozes"});
        assertTrue("should have 9 elements in returned array",
                   sp.length == 9);
        assertTrue("didn't return expected prefix strings",
                   compareStringArray(prefixedExpected, sp));
        
        //just make sure non-ascii chars don't break
        prefixedExpected = 
        new String[]{"\u5bae\u672c\u6b66\u8535\u69d8",
         "\u5bae\u672c\u6b66\u8535",
         "\u5bae\u672c\u6b66"};
        
        sp =
            HashFunction.getPrefixes
            (new String[]{"\u5bae\u672c\u6b66\u8535\u69d8"});
        assertTrue("should have 3 elements in array",
                   sp.length == 3);
        assertTrue("didn't return expected prefix strings",
                   compareStringArray(prefixedExpected, sp));

    }

    /**
     * compares two arrays by comparing the strings in the arrays.
     * the order does matter so the string elements in the array
     * must be in the same order inorder for this function to 
     * return true
     */
    private boolean compareStringArray(String[] a1, String[] a2) {
        if(a1.length != a2.length)
            return false;
        
        for(int i = 0; i < a1.length; i++) {
            if(!a1[i].equals(a2[i])) {
                return false;
            }
        }

        return true;
    }

}
