package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.*;
import java.io.*;
import java.util.Properties;
import junit.framework.*;
import java.awt.Color;

public class SettingTest extends com.limegroup.gnutella.util.BaseTestCase {
    
    File settingsFile;
    
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
        if(CommonUtils.isUnix() || CommonUtils.isMacOSX()) {
            testFilePath = "/mickey-mouse.com";
        } else {
            // assume we're on windows
            testFilePath = "c:/mickey-mouse.com";
        }

        String testFilePath2;
        if(CommonUtils.isUnix() || CommonUtils.isMacOSX()) {
            testFilePath2 = "/temp/turkey.txt";
        } else {
            // assume we're on windows
            testFilePath2 = "c:/temp/turkey.txt";
        }
    
        /* Delete any existing file and create a new set of settings */
        TestSettings settings = new TestSettings(settingsFile);
        TestSettings settings2 = new TestSettings(settingsFile);
        
        /* Confirm all of the default values
         * (Which requires one full conversion to and from their string values 
         */
        assertEquals("Bool default", true, settings.BOOL_SETTING.getValue());
        assertEquals("Byte default", (byte)23, settings.BYTE_SETTING.getValue());
        assertEquals("Color default", new Color(255,127,63), 
                     settings.COLOR_SETTING.getValue());        

        // we assume we're on Windows if we're not on any Unix or OS X
        //assertEquals("File default", 
        //           new File("/temp/turkey.txt"),
        //           settings.FILE_SETTING.getValue());
        assertEquals("Int default", 143, settings.INT_SETTING.getValue());
        assertEquals("Long default", 666666, settings.LONG_SETTING.getValue());
        assertEquals("String default", "terrific", settings.STRING_SETTING.getValue());
        
        /* Confirm that we can set everything */
        settings.BOOL_SETTING.setValue(false);
        settings.BYTE_SETTING.setValue((byte)6);
        settings.COLOR_SETTING.setValue(new Color(66, 44, 67));
        settings.FILE_SETTING.setValue(new File(testFilePath));
        settings.INT_SETTING.setValue(234);
        settings.LONG_SETTING.setValue(555555);
        settings.STRING_SETTING.setValue("OK so far");
        
        assertEquals("Bool set", false, settings.BOOL_SETTING.getValue());
        assertEquals("Byte set", (byte)6, settings.BYTE_SETTING.getValue());
        assertEquals("Color set", new Color(66,44,67), settings.COLOR_SETTING.getValue());
        assertEquals("File set", new File(testFilePath), 
                                 settings.FILE_SETTING.getValue());
        assertEquals("Int set", 234, settings.INT_SETTING.getValue());
        assertEquals("Long set", 555555, settings.LONG_SETTING.getValue());
        assertEquals("String set", "OK so far", settings.STRING_SETTING.getValue());
        
        /* Write property to file and confirm that everything reloads properly */
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
        
        /* Confirm that the backup object still has its default settings */
        assertEquals("Bool default", true, settings2.BOOL_SETTING.getValue());
        assertEquals("Byte default", (byte)23, settings2.BYTE_SETTING.getValue());
        assertEquals("Color default", new Color(255,127,63), settings2.COLOR_SETTING.getValue());
        assertEquals("File default", new File(testFilePath2), 
                                     settings2.FILE_SETTING.getValue());
        assertEquals("Int default", 143, settings2.INT_SETTING.getValue());
        assertEquals("Long default", 666666, settings2.LONG_SETTING.getValue());
        assertEquals("String default", "terrific", settings2.STRING_SETTING.getValue());
        
        /* reload it from the real file and make sure we got everything */
        settings2.FACTORY.reload();
        assertEquals("Bool set", false, settings2.BOOL_SETTING.getValue());
        assertEquals("Byte set", (byte)6, settings2.BYTE_SETTING.getValue());
        assertEquals("Color set", new Color(66,44,67), settings2.COLOR_SETTING.getValue());
        assertEquals("File set", new File(testFilePath), 
                                 settings2.FILE_SETTING.getValue());
        assertEquals("Int set", 234, settings2.INT_SETTING.getValue());
        assertEquals("Long set", 555555, settings2.LONG_SETTING.getValue());
        assertEquals("String set", "OK so far", settings2.STRING_SETTING.getValue());
    }
}
