package com.limegroup.gnutella.gui;

import junit.framework.Test;

import com.limegroup.gnutella.gui.search.DisplayManager;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.sun.naming.internal.ResourceManager;


//this is in the com.limegroup.gnutella.gui.search but this calss
//needs access to the ResourceManager.class which is package private in
//com.limegroup.gnutella.gui.....
public class DisplayManagerTest extends BaseTestCase {

    private final String vxsd = "video";

    public DisplayManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DisplayManagerTest.class);
    }

    DisplayManager dispM;
    
    
    public void setUp() throws Exception {

    }
    
    public void testLocale() throws Exception {
        
        dispM = DisplayManager.instance();
        
        checkEnglish(dispM);

        ApplicationSettings.LANGUAGE.setValue("ja");
        ApplicationSettings.COUNTRY.setValue("JP");
        
        PrivilegedAccessor.invokeMethod(ResourceManager.class,
                                        "resetLocaleOptions",
                                        new Object[]{},
                                        new Class[]{});
        
        dispM = (DisplayManager)PrivilegedAccessor.invokeConstructor(DisplayManager.class,
                                                     new Object[]{});
        checkJA(dispM);
    }


    //are these tests even worth it????
    private void checkEnglish(DisplayManager dm) {
        assertEquals("Title", 
                     dm.getDisplayName("videos__video__title__", vxsd));
        assertEquals("Director", 
                     dm.getDisplayName("videos__video__director__", vxsd));
        assertEquals("Producer", 
                     dm.getDisplayName("videos__video__producer__", vxsd));
        assertEquals("Studio", 
                     dm.getDisplayName("videos__video__studio__", vxsd));
        assertEquals("Stars", 
                     dm.getDisplayName("videos__video__stars__", vxsd));
    }

    private void checkJA(DisplayManager dm) {
        assertEquals("\u4f5c\u540d", 
                     dm.getDisplayName("videos__video__title__", vxsd));
        assertEquals("\u76e3\u7763",
                     dm.getDisplayName("videos__video__director__", vxsd));
        assertEquals("\u88fd\u4f5c/\u30d7\u30ed\u30c7\u30e5\u30b5\u30fc", 
                     dm.getDisplayName("videos__video__producer__", vxsd));
        assertEquals("\u30b9\u30bf\u30b8\u30aa", 
                     dm.getDisplayName("videos__video__studio__", vxsd));
        assertEquals("\u4e3b\u6f14", 
                     dm.getDisplayName("videos__video__stars__", vxsd));
    }

}





