package com.limegroup.gnutella.settings;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.*;
import java.io.*;
import java.util.Properties;
import junit.framework.*;
import java.awt.Color;

/**
 * Tests the setting for character arrays.
 */
public class SettingTest extends BaseTestCase {
    
    private static final long EXPIRY_INTERVAL = 15 * 24 * 60 * 60 * 1000l; //15 days
    private static final String LAST_EXPIRE_TIME = "LAST_EXPIRE_TIME";
    
    File settingsFile;
    
    private final char[] chars = {'a','b','c'};
    
    private final String[] strings = new String[]{"string1", "string2", "string3"};
    private File[] files;
    
    class TestSettings {

        Properties DEF_PROPS;
        Properties PROPS;
        SettingsFactory FACTORY;
        BooleanSetting BOOL_SETTING;
        ByteSetting BYTE_SETTING;
        ColorSetting COLOR_SETTING;
        FileSetting FILE_SETTING;
        IntSetting INT_SETTING;
        LongSetting LONG_SETTING;
        StringSetting STRING_SETTING;
        CharArraySetting CHAR_ARRAY_SETTING;
        StringArraySetting STRING_ARRAY_SETTING;
        FileArraySetting FILE_ARRAY_SETTING;
        
        IntSetting EXPIRABLE_INT_SETTING;
        BooleanSetting EXPIRABLE_BOOLEAN_SETTING;
        
        TestSettings(File file) throws IOException {
            FACTORY = new SettingsFactory(file);
            
            BOOL_SETTING = FACTORY.createBooleanSetting("BOOL_SETTING", true);
            BYTE_SETTING = FACTORY.createByteSetting("BYTE_SETTING", (byte)23);
            COLOR_SETTING = FACTORY.createColorSetting("COLOR_SETTING", 
                                                       new Color(255, 127, 63));
            FILE_SETTING = FACTORY.createFileSetting("FILE_SETTING", 
                                                     new File("/temp/turkey.txt"));
            INT_SETTING = FACTORY.createIntSetting("INT_SETTING", 143);
            LONG_SETTING = FACTORY.createLongSetting("LONG_SETTING", 666666);
            STRING_SETTING = FACTORY.createStringSetting("STRING_SETTING", 
                                                         "terrific");
            CHAR_ARRAY_SETTING = 
                FACTORY.createCharArraySetting("CHAR_ARRAY_SETTING", chars);
                
            STRING_ARRAY_SETTING =
                FACTORY.createStringArraySetting("STRING_ARRAY_SETTING", strings);
            
            FILE_ARRAY_SETTING =
                FACTORY.createFileArraySetting("FILE_ARRAY_SETTING", files);
                
            EXPIRABLE_INT_SETTING =
                FACTORY.createExpirableIntSetting("EXPIRABLE_INT_SETTING", 12061980);
                
            EXPIRABLE_BOOLEAN_SETTING =
                FACTORY.createExpirableBooleanSetting("EXPIRABLE_BOOLEAN_SETTING", false);
                
            FACTORY.getProperties().setProperty(LAST_EXPIRE_TIME, String.valueOf(System.currentTimeMillis()));
        }
    }
    
    
    public SettingTest(java.lang.String testName) {
        super(testName);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(SettingTest.class);
        return suite;
    }
    
    public void setUp() throws Exception {
        settingsFile = new File(getSaveDirectory(), "testSettings.props");
        settingsFile.delete();
    }
        
    
    public void tearDown() {
       settingsFile.delete();
    }

    public void testSetting() throws Exception {
        
        
        String testFilePath;
        String testFilePath2;
        File[] testFileArray;
        
        if(CommonUtils.isUnix() || CommonUtils.isMacOSX()) {
            testFilePath = "/mickey-mouse.com";
            testFilePath2 = "/temp/turkey.txt";
            testFileArray = new File[] { 
                    new File("/temp/A"), new File("/temp/B") };
        } else {
            // assume we're on windows
            testFilePath = "c:/mickey-mouse.com";
            testFilePath2 = "c:/temp/turkey.txt";
            testFileArray = new File[] { 
                    new File("c:/temp/A"), new File("c:/temp/B") };            
        }
    
        files = new File[]{ new File(testFilePath), new File(testFilePath2) };
        
        // Delete any existing file and create a new set of settings */
        TestSettings settings = new TestSettings(settingsFile);
        TestSettings settings2 = new TestSettings(settingsFile);
        
        // Confirm all of the default values
        // (Which requires one full conversion to and from their string values 
        assertEquals("Bool default", true, settings.BOOL_SETTING.getValue());
        assertEquals("Byte default", (byte)23, settings.BYTE_SETTING.getValue());
        assertEquals("Color default", new Color(255,127,63), 
                     settings.COLOR_SETTING.getValue());        

        assertEquals("Int default", 143, settings.INT_SETTING.getValue());
        assertEquals("Long default", 666666, settings.LONG_SETTING.getValue());
        assertEquals("String default", "terrific", settings.STRING_SETTING.getValue());
        assertTrue("char arrays should be equal", 
                   Arrays.equals(new char[]{'a','b','c'}, 
                                 settings.CHAR_ARRAY_SETTING.getValue()));
        assertTrue("string arrays should be equal", 
                   Arrays.equals(new String[]{"string1", "string2", "string3"}, 
                                 settings.STRING_ARRAY_SETTING.getValue()));
        
        assertTrue("file arrays should be equal", 
                   Arrays.equals(files, 
                                 settings.FILE_ARRAY_SETTING.getValue()));
        
        assertEquals("Expiring Int default", 12061980, settings.EXPIRABLE_INT_SETTING.getValue());
        assertEquals("Expiring Boolean default", false, settings.EXPIRABLE_BOOLEAN_SETTING.getValue());
        
        // Confirm that we can set everything 
        settings.BOOL_SETTING.setValue(false);
        settings.BYTE_SETTING.setValue((byte)6);
        settings.COLOR_SETTING.setValue(new Color(66, 44, 67));
        settings.FILE_SETTING.setValue(new File(testFilePath));
        settings.INT_SETTING.setValue(234);
        settings.LONG_SETTING.setValue(555555);
        settings.STRING_SETTING.setValue("OK so far");
        settings.CHAR_ARRAY_SETTING.setValue(new char[] {'d', 'e', 'f'});
        settings.STRING_ARRAY_SETTING.setValue(new String[]{"OK", "so", "far"});
        settings.FILE_ARRAY_SETTING.setValue(new File[]{new File("/temp/A"), new File("/temp/B")});
        settings.EXPIRABLE_INT_SETTING.setValue(0xFFFF);
        settings.EXPIRABLE_BOOLEAN_SETTING.setValue(true);
        
        assertEquals("Bool set", false, settings.BOOL_SETTING.getValue());
        assertEquals("Byte set", (byte)6, settings.BYTE_SETTING.getValue());
        assertEquals("Color set", new Color(66,44,67), settings.COLOR_SETTING.getValue());
        assertEquals("File set", new File(testFilePath), 
                                 settings.FILE_SETTING.getValue());
        assertEquals("Int set", 234, settings.INT_SETTING.getValue());
        assertEquals("Long set", 555555, settings.LONG_SETTING.getValue());
        assertEquals("String set", "OK so far", settings.STRING_SETTING.getValue());
        assertTrue("char arrays should be equal", 
                   Arrays.equals(new char[]{'d','e','f'}, 
                                 settings.CHAR_ARRAY_SETTING.getValue()));
        assertTrue("string arrays should be equal", 
                   Arrays.equals(new String[]{"OK", "so", "far"}, 
                                 settings.STRING_ARRAY_SETTING.getValue()));
        assertTrue("file arrays should be equal", 
                   Arrays.equals(testFileArray, 
                                 settings.FILE_ARRAY_SETTING.getValue()));
                                 
        assertEquals("Expiring Int set", 0xFFFF, settings.EXPIRABLE_INT_SETTING.getValue());
        assertEquals("Expiring Boolean set", true, settings.EXPIRABLE_BOOLEAN_SETTING.getValue());
                                 
        // Write property to file and confirm that everything reloads properly 
        settings.FACTORY.save();
        
        settings = new TestSettings(settingsFile);
        assertEquals("Bool set", false, settings.BOOL_SETTING.getValue());
        assertEquals("Byte set", (byte)6, settings.BYTE_SETTING.getValue());
        assertEquals("Color set", new Color(66,44,67), settings.COLOR_SETTING.getValue());
        assertEquals("File set", new File(testFilePath), 
                                 settings.FILE_SETTING.getValue());
        assertEquals("Int set", 234, settings.INT_SETTING.getValue());
        assertEquals("Long set", 555555, settings.LONG_SETTING.getValue());
        assertEquals("String set", "OK so far", settings.STRING_SETTING.getValue());
        assertTrue("char arrays should be equal", 
                   Arrays.equals(new char[]{'d','e','f'}, 
                                 settings.CHAR_ARRAY_SETTING.getValue()));
        assertTrue("string arrays should be equal", 
                   Arrays.equals(new String[]{"OK", "so", "far"}, 
                                 settings.STRING_ARRAY_SETTING.getValue()));
        assertTrue("file arrays should be equal", 
                   Arrays.equals(testFileArray, 
                                 settings.FILE_ARRAY_SETTING.getValue()));
        
        assertEquals("Expiring Int set", 0xFFFF, settings.EXPIRABLE_INT_SETTING.getValue());
        assertEquals("Expiring Boolean set", true, settings.EXPIRABLE_BOOLEAN_SETTING.getValue());
                                 
        // Confirm that the backup object still has its default settings 
        assertEquals("Bool default", true, settings2.BOOL_SETTING.getValue());
        assertEquals("Byte default", (byte)23, settings2.BYTE_SETTING.getValue());
        assertEquals("Color default", new Color(255,127,63), settings2.COLOR_SETTING.getValue());
        assertEquals("File default", new File(testFilePath2), 
                                     settings2.FILE_SETTING.getValue());
        assertEquals("Int default", 143, settings2.INT_SETTING.getValue());
        assertEquals("Long default", 666666, settings2.LONG_SETTING.getValue());
        assertEquals("String default", "terrific", settings2.STRING_SETTING.getValue());
        assertTrue("char arrays should be equal", 
                   Arrays.equals(new char[]{'a','b','c'}, 
                                 settings2.CHAR_ARRAY_SETTING.getValue()));
        assertTrue("string arrays should be equal", 
                   Arrays.equals(new String[]{ "string1", "string2", "string3" }, 
                                 settings2.STRING_ARRAY_SETTING.getValue()));
        assertTrue("file arrays should be equal", 
                   Arrays.equals(files, 
                                 settings2.FILE_ARRAY_SETTING.getValue()));
        
        assertEquals("Expiring Int default", 12061980, settings2.EXPIRABLE_INT_SETTING.getValue());
        assertEquals("Expiring Boolean default", false, settings2.EXPIRABLE_BOOLEAN_SETTING.getValue());
        
        // reload it from the real file and make sure we got everything 
        settings2.FACTORY.reload();
        assertEquals("Bool set", false, settings2.BOOL_SETTING.getValue());
        assertEquals("Byte set", (byte)6, settings2.BYTE_SETTING.getValue());
        assertEquals("Color set", new Color(66,44,67), settings2.COLOR_SETTING.getValue());
        assertEquals("File set", new File(testFilePath), 
                                 settings2.FILE_SETTING.getValue());
        assertEquals("Int set", 234, settings2.INT_SETTING.getValue());
        assertEquals("Long set", 555555, settings2.LONG_SETTING.getValue());
        assertEquals("String set", "OK so far", settings2.STRING_SETTING.getValue());
        assertTrue("char arrays should be equal", 
                   Arrays.equals(new char[]{'d','e','f'}, 
                                 settings2.CHAR_ARRAY_SETTING.getValue()));
        assertTrue("string arrays should be equal", 
                   Arrays.equals(new String[]{"OK", "so", "far"}, 
                                 settings2.STRING_ARRAY_SETTING.getValue()));
        assertTrue("file arrays should be equal", 
                   Arrays.equals(testFileArray, 
                                 settings2.FILE_ARRAY_SETTING.getValue()));
                                 
        assertEquals("Expiring Int set", 0xFFFF, settings2.EXPIRABLE_INT_SETTING.getValue());
        assertEquals("Expiring Boolean set", true, settings2.EXPIRABLE_BOOLEAN_SETTING.getValue());
        
        settings2.FACTORY.save();
        
        // Change the date to NOW - 14days
        settings = new TestSettings(settingsFile);
        Properties PROPS = settings.FACTORY.getProperties();
        PROPS.setProperty(LAST_EXPIRE_TIME, String.valueOf(System.currentTimeMillis()-EXPIRY_INTERVAL));
        settings.FACTORY.save();
        
        // Do they have their default values now?
        settings2 = new TestSettings(settingsFile);
        assertEquals("Expiring Int default", 12061980, settings2.EXPIRABLE_INT_SETTING.getValue());
        assertEquals("Expiring Int default", true, settings2.EXPIRABLE_INT_SETTING.isDefault());
        
        assertEquals("Expiring Boolean default", false, settings2.EXPIRABLE_BOOLEAN_SETTING.getValue());
        assertEquals("Expiring Boolean default", true, settings2.EXPIRABLE_BOOLEAN_SETTING.isDefault());
    }
}
