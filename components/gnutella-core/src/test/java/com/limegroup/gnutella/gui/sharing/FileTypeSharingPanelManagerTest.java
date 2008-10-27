package com.limegroup.gnutella.gui.sharing;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.limewire.core.settings.OldLibrarySettings;
import org.limewire.setting.StringArraySetting;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.gui.GUIBaseTestCase;
import com.limegroup.gnutella.gui.GuiCoreMediator;

@SuppressWarnings("deprecation")
public class FileTypeSharingPanelManagerTest extends GUIBaseTestCase {

    public FileTypeSharingPanelManagerTest(String name) {
        super(name);
    }

    private String extensions;
    private String extensions_unchecked;
    private String extensions_custom;
    private boolean migrate;


    public static Test suite() { 
        return buildTestSuite(FileTypeSharingPanelManagerTest.class); 
    }
    
    @Override
    public void setUp() {
        extensions = OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue();
        extensions_unchecked = OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue();
        extensions_custom = OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue();
        migrate = OldLibrarySettings.EXTENSIONS_MIGRATE.getValue();
        
        LimeTestUtils.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                this.requestStaticInjection(GuiCoreMediator.class);
            }
        
        });
        
    }
    
    @Override
    public void tearDown() {
        OldLibrarySettings.EXTENSIONS_TO_SHARE.setValue(extensions);
        OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.setValue(extensions_unchecked);
        OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.setValue(extensions_custom);
        OldLibrarySettings.EXTENSIONS_MIGRATE.setValue(migrate);
    }
    
    public void testMigrate() throws Exception {
        OldLibrarySettings.EXTENSIONS_MIGRATE.setValue(true);
        OldLibrarySettings.EXTENSIONS_TO_SHARE.setValue("mp3;wav;hello");
        
        FileTypeSharingPanelManager manager = new FileTypeSharingPanelManager();
        
        manager.initCore();
        
        manager.applyOptions();
        
        assertFalse(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains(";;"));
        assertFalse(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue().contains(";;"));
        assertFalse(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue().contains(";;"));
        assertTrue(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains("mp3"));
        assertTrue(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains("hello"));
        assertTrue(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains("wav"));
        assertTrue(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().matches("^[^;]*;[^;]*;[^;]*$"));
        assertEquals(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue(), "hello");
        assertFalse(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue().contains("mp3"));
        assertTrue(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue().contains("doc"));
        assertFalse(OldLibrarySettings.EXTENSIONS_MIGRATE.getValue());
        assertFalse(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().endsWith(";"));
    }

    
    public void testMigrateMalformed() throws Exception {
        OldLibrarySettings.EXTENSIONS_MIGRATE.setValue(true);
        OldLibrarySettings.EXTENSIONS_TO_SHARE.setValue("mp3;wav;;hello;");

        FileTypeSharingPanelManager manager = new FileTypeSharingPanelManager();
        
        manager.initCore();
        
        manager.applyOptions();

        assertFalse(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains(";;"));
        assertFalse(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue().contains(";;"));
        assertFalse(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue().contains(";;"));
        assertTrue(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains("mp3"));
        assertTrue(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains("hello"));
        assertTrue(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains("wav"));
        assertTrue(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().matches("^[^;]*;[^;]*;[^;]*$"));
        assertEquals(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue(), "hello");
        assertFalse(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue().contains("mp3"));
        assertTrue(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue().contains("doc"));
        assertFalse(OldLibrarySettings.EXTENSIONS_MIGRATE.getValue());
        assertFalse(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().endsWith(";"));
    }

    
    public void testNormal() throws Exception {
        OldLibrarySettings.EXTENSIONS_MIGRATE.setValue(false);
        OldLibrarySettings.EXTENSIONS_TO_SHARE.setValue("--blank--");
        
        OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.setValue("mp3;wav;hello");
        OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.setValue("test;hello");
        
        FileTypeSharingPanelManager manager = new FileTypeSharingPanelManager();
        
        manager.initCore();
        
        manager.applyOptions();
        
        assertFalse(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains(";;"));
        assertFalse(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue().contains(";;"));
        assertFalse(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue().contains(";;"));
        assertTrue(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains("test"));
        assertTrue(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains("doc"));
        assertFalse(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains("mp3"));
        assertFalse(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains("hello"));
        assertFalse(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().endsWith(";"));
        assertTrue(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue().contains("test"));
        assertTrue(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue().contains("hello"));
        assertTrue(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue().contains("mp3"));
        assertTrue(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue().contains("wav"));
        assertFalse(OldLibrarySettings.EXTENSIONS_MIGRATE.getValue());
    }
   
    public void testExtras() throws Exception {
        OldLibrarySettings.EXTENSIONS_MIGRATE.setValue(false);
        OldLibrarySettings.EXTENSIONS_TO_SHARE.setValue("--blank--");
        
        OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.setValue("");
        OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.setValue("hello;test;hello;hello;hello;hello");
        
        FileTypeSharingPanelManager manager = new FileTypeSharingPanelManager();
        
        manager.initCore();
        
        manager.applyOptions();
        
        assertFalse(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains(";;"));
        assertFalse(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue().contains(";;"));
        assertFalse(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue().contains(";;"));
        assertTrue(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains("test"));
        assertTrue(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains("doc"));
        assertTrue(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains("hello"));
        assertFalse(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().endsWith(";"));
        assertTrue(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue().contains("test"));
        assertTrue(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue().contains("hello"));
        assertTrue(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue().length() == 0);
        assertFalse(OldLibrarySettings.EXTENSIONS_MIGRATE.getValue());
        
        int index;

        // Assert doubling of hello has been removed and not passed on
        assertTrue((index = OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue().indexOf("hello")) > -1);
        assertFalse(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue().indexOf("hello", index+1) > -1);
        assertTrue((index = OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().indexOf("hello")) > -1);
        assertFalse(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().indexOf("hello", index+1) > -1);
    }

    
    // TODO: Fix this test
    /*  This tests bullet one from requirement two of GUI-299.  At the current time
     *   there is no simple way to test adding a new default shared extension at runtime without
     *   code modifications.
     *   
     *   The following test case should therefore be run after the final clause is removed from
     *   SharingSettings.DEFAULT_EXTENSIONS_TO_SHARE.
    
    public void testAddNewDefault() throws Exception {
        SharingSettings.EXTENSIONS_MIGRATE.setValue(false);
        SharingSettings.EXTENSIONS_TO_SHARE.setValue(SharingSettings.getDefaultDisabledExtensionsAsString());
        SharingSettings.EXTENSIONS_LIST_UNSHARED.setValue("mp3");
        SharingSettings.EXTENSIONS_LIST_CUSTOM.setValue("extra");
        
        PrivilegedAccessor.setValue(SharingSettings.class, "DEFAULT_EXTENSIONS_TO_SHARE",
                SharingSettings.getDefaultExtensionsAsString()+";newdef");
        
        FileTypeSharingPanelManager manager = new FileTypeSharingPanelManager();
        
        manager.initCore();
        
        manager.applyOptions();
        
        assertFalse(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains(";;"));
        assertFalse(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().contains(";;"));
        assertFalse(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().contains(";;"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("extra"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("newdef"));
        assertFalse(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("mp3"));
        
        
        assertTrue(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().equals("extra"));
        assertEquals(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue(), "mp3");
        
        assertFalse(SharingSettings.EXTENSIONS_MIGRATE.getValue());
    }
    */
    
    
    public void testClearAll() throws Exception {
        OldLibrarySettings.EXTENSIONS_MIGRATE.setValue(false);
        OldLibrarySettings.EXTENSIONS_TO_SHARE.setValue("mp3;doc;custom");
        
        OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.setValue(OldLibrarySettings.getDefaultExtensionsAsString());
        OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.setValue("");
        
        FileTypeSharingPanelManager manager = new FileTypeSharingPanelManager();
        
        manager.initCore();
        manager.applyOptions();
        
        Set<Integer> taken = new HashSet<Integer>();
        
        for ( String ext : OldLibrarySettings.getDefaultExtensions() ) {
            int index;
            if ((index = find(StringArraySetting.decode(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue()), ext)) != -1) {
                if (taken.contains(index)) {
                    fail("Warning -- duplicate extension in defaults: " + ext);
                } 
                else {
                    taken.add(index);
                }
            }
            else {
                fail("Unshare Lost: " + ext);
            }
        }
        
        assertFalse(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().contains(";;"));
        assertFalse(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue().contains(";;"));
        assertFalse(OldLibrarySettings.EXTENSIONS_LIST_UNSHARED.getValue().contains(";;"));
        assertTrue(OldLibrarySettings.EXTENSIONS_TO_SHARE.getValue().length() == 0);
        assertTrue(OldLibrarySettings.EXTENSIONS_LIST_CUSTOM.getValue().length() == 0);
        assertFalse(OldLibrarySettings.EXTENSIONS_MIGRATE.getValue());
        


    }   
    
    private static int find(Object[] list, Object value) {
        for ( int i=0 ; i<list.length ; i++ ) {
            if (list[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }
    
}
