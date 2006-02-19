package com.limegroup.gnutella.settings;

import java.util.Properties;

import com.limegroup.gnutella.util.BaseTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;


public class PowerOfTwoSettingTest extends BaseTestCase {

    public PowerOfTwoSettingTest(String name) {
        super(name);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(PowerOfTwoSettingTest.class);
        return suite;
    }
    
    public void setUp() throws Exception {
        
    }
        
    public void tearDown() {
       
    }
    
    public void testUsedSizes() throws Exception {
        // As of early 2006, {64, 128, 256} are the only legal values we use
        PowerOfTwoSetting set64 = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", 64, "", Long.MAX_VALUE, Long.MIN_VALUE);
        assertEquals("Created setting has unexpected value", 64, set64.getValue());
        PowerOfTwoSetting set128 = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", 128, "", Long.MAX_VALUE, Long.MIN_VALUE);
        assertEquals("Created setting has unexpected value", 128, set128.getValue());
        PowerOfTwoSetting set256 = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", 256, "", Long.MAX_VALUE, Long.MIN_VALUE);
        assertEquals("Created setting has unexpected value", 256, set256.getValue());
        
        // Now test with the real max and min from QRT_TABLE_SIZE_IN_KILOBYTES
        set64 = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", 64, "", 256, 64);
        assertEquals("Created setting has unexpected value", 64, set64.getValue());
        set128 = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", 128, "", 256, 64);
        assertEquals("Created setting has unexpected value", 128, set128.getValue());
        set256 = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", 256, "", 256, 64);
        assertEquals("Created setting has unexpected value", 256, set256.getValue());
    }
    
    public void testDefaultValueChecks() throws Exception {
        PowerOfTwoSetting setting = null;
        
        try {
            setting = new PowerOfTwoSetting(new Properties(), new Properties(), 
                    "", 65, "", Long.MAX_VALUE, Long.MIN_VALUE);
            fail("PowerOfTwoSetting has a default value that isn't a power of two.");
        } catch (IllegalArgumentException expectedException) {;}
        try {
            setting = new PowerOfTwoSetting(new Properties(), new Properties(), 
                    "", 192, "", Long.MAX_VALUE, Long.MIN_VALUE);
            fail("PowerOfTwoSetting has a default value that isn't a power of two.");
        } catch (IllegalArgumentException expectedException) {;}
        try {
            setting = new PowerOfTwoSetting(new Properties(), new Properties(), 
                    "", 0, "", Long.MAX_VALUE, Long.MIN_VALUE);
            fail("PowerOfTwoSetting has a default value that isn't a power of two.");
        } catch (IllegalArgumentException expectedException) {;}
        try {
            setting = new PowerOfTwoSetting(new Properties(), new Properties(), 
                    "", -2, "", Long.MAX_VALUE, Long.MIN_VALUE);
            fail("PowerOfTwoSetting has a default value that isn't a power of two.");
        } catch (IllegalArgumentException expectedException) {;}
    }
    
    public void testSetValue() throws Exception {
        PowerOfTwoSetting setting = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", 64, "", 256, 64);
        assertEquals("Created setting has unexpected value", 64, setting.getValue());
        
        setting.setValue(128);
        assertEquals("Setting did not accept a legal value.", 128, setting.getValue());
        
        setting.setValue(256);
        assertEquals("Setting did not accept a legal value.", 256, setting.getValue());
        
        // Set value from String
        setting.setValue("64");
        assertEquals("Setting did not accept a legal value.", 64, setting.getValue());
        
        
        // Test powers of two that are too big or too small
        setting.setValue(512);
        assertEquals("Setting accepted an illegal value.", 64, setting.getValue());
        
        setting.setValue(32);
        assertEquals("Setting accepted an illegal value.", 64, setting.getValue());
        
        // Test values likely to fool naive implementations of power-of-two checking
        setting.setValue(192);
        assertEquals("Setting accepted an illegal value.", 64, setting.getValue());
        
        setting.setValue(96);
        assertEquals("Setting accepted an illegal value.", 64, setting.getValue());
        
        setting.setValue(0);
        assertEquals("Setting accepted an illegal value.", 64, setting.getValue());
        
        setting.setValue(-2);
        assertEquals("Setting accepted an illegal value.", 64, setting.getValue());
        
        setting.setValue(127);
        assertEquals("Setting accepted an illegal value.", 64, setting.getValue());
        
        setting.setValue(129);
        assertEquals("Setting accepted an illegal value.", 64, setting.getValue());
    }
    
}
