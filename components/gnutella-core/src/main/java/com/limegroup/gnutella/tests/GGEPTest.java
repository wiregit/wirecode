package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.message.*;
import com.sun.java.util.collections.*;
import junit.framework.*;

public class GGEPTest extends TestCase {

    private Map hashMap = null;

    public GGEPTest(String name) {
        super(name);
    }

    protected void setUp() {
        hashMap = new HashMap();
    }

    public void testMapConstructor1() {
        hashMap.clear();
        hashMap.put("A","B");
        hashMap.put("C",null);
        hashMap.put(GGEP.GGEP_HEADER_BROWSE_HOST,"");
        try {
            GGEP temp = new GGEP(hashMap);
        }
        catch (BadGGEPPropertyException hopefullyNot) {
            Assert.assertTrue("Test 1 Constructor Failed!", false);
        }
    }

    public void testMapConstructor2() {
        hashMap.clear();

        hashMap.put("THIS KEY IS WAY TO LONG!", "");

        try {
            GGEP temp = new GGEP(hashMap);
            Assert.assertTrue("Test 2 Constructor Failed!", false);
        }
        catch (BadGGEPPropertyException hopefullySo) {
        }        
    }

    public void testMapConstructor3() {
        hashMap.clear();

        StringBuffer bigBoy = new StringBuffer(GGEP.MAX_VALUE_SIZE_IN_BYTES+10);
        for (int i = 0; i < GGEP.MAX_VALUE_SIZE_IN_BYTES+10; i++)
            bigBoy.append("1");
        
        hashMap.put("WHATEVER", bigBoy);
        try {
            GGEP temp = new GGEP(hashMap);
            Assert.assertTrue("Test 3 Constructor Failed!", false);
        }
        catch (BadGGEPPropertyException hopefullySo) {
        }
    }


    public void testByteArrayConstructor1() {
        byte[] bytes = new byte[2];
        bytes[0] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x8A;
        try {
            GGEP temp = new GGEP(bytes,0);
        }
        catch (BadGGEPBlockException hopefullyNot) {
            Assert.assertTrue("Test 4 Constructor Failed!", false);
        }
    }


    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite =  new TestSuite("GGEP Unit Tests");
        suite.addTest(new TestSuite(GGEPTest.class));
        return suite;
    }



}
