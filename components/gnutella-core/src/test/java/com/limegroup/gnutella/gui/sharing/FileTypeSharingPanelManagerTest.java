package com.limegroup.gnutella.gui.sharing;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.limewire.setting.StringArraySetting;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.gui.GUIBaseTestCase;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.settings.SharingSettings;

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
        extensions = SharingSettings.EXTENSIONS_TO_SHARE.getValue();
        extensions_unchecked = SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue();
        extensions_custom = SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue();
        migrate = SharingSettings.EXTENSIONS_MIGRATE.getValue();
        
        LimeTestUtils.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                this.requestStaticInjection(GuiCoreMediator.class);
            }
        
        });
        
    }
    
    @Override
    public void tearDown() {
        SharingSettings.EXTENSIONS_TO_SHARE.setValue(extensions);
        SharingSettings.EXTENSIONS_LIST_UNSHARED.setValue(extensions_unchecked);
        SharingSettings.EXTENSIONS_LIST_CUSTOM.setValue(extensions_custom);
        SharingSettings.EXTENSIONS_MIGRATE.setValue(migrate);
    }
    
    public void testMigrate() throws Exception {
        SharingSettings.EXTENSIONS_MIGRATE.setValue(true);
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("mp3;wav;hello");
        
        FileTypeSharingPanelManager manager = new FileTypeSharingPanelManager();
        
        manager.initCore();
        
        manager.applyOptions();
        
        assertFalse(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains(";;"));
        assertFalse(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().contains(";;"));
        assertFalse(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().contains(";;"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("mp3"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("hello"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("wav"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().matches("^[^;]*;[^;]*;[^;]*$"));
        assertEquals(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue(), "hello");
        assertFalse(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().contains("mp3"));
        assertTrue(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().contains("doc"));
        assertFalse(SharingSettings.EXTENSIONS_MIGRATE.getValue());
        assertFalse(SharingSettings.EXTENSIONS_TO_SHARE.getValue().endsWith(";"));
    }

    
    public void testMigrateMalformed() throws Exception {
        SharingSettings.EXTENSIONS_MIGRATE.setValue(true);
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("mp3;wav;;hello;");

        FileTypeSharingPanelManager manager = new FileTypeSharingPanelManager();
        
        manager.initCore();
        
        manager.applyOptions();

        assertFalse(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains(";;"));
        assertFalse(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().contains(";;"));
        assertFalse(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().contains(";;"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("mp3"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("hello"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("wav"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().matches("^[^;]*;[^;]*;[^;]*$"));
        assertEquals(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue(), "hello");
        assertFalse(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().contains("mp3"));
        assertTrue(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().contains("doc"));
        assertFalse(SharingSettings.EXTENSIONS_MIGRATE.getValue());
        assertFalse(SharingSettings.EXTENSIONS_TO_SHARE.getValue().endsWith(";"));
    }

    
    public void testNormal() throws Exception {
        SharingSettings.EXTENSIONS_MIGRATE.setValue(false);
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("--blank--");
        
        SharingSettings.EXTENSIONS_LIST_UNSHARED.setValue("mp3;wav;hello");
        SharingSettings.EXTENSIONS_LIST_CUSTOM.setValue("test;hello");
        
        FileTypeSharingPanelManager manager = new FileTypeSharingPanelManager();
        
        manager.initCore();
        
        manager.applyOptions();
        
        assertFalse(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains(";;"));
        assertFalse(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().contains(";;"));
        assertFalse(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().contains(";;"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("test"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("doc"));
        assertFalse(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("mp3"));
        assertFalse(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("hello"));
        assertFalse(SharingSettings.EXTENSIONS_TO_SHARE.getValue().endsWith(";"));
        assertTrue(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().contains("test"));
        assertTrue(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().contains("hello"));
        assertTrue(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().contains("mp3"));
        assertTrue(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().contains("wav"));
        assertFalse(SharingSettings.EXTENSIONS_MIGRATE.getValue());
    }
   
    public void testExtras() throws Exception {
        SharingSettings.EXTENSIONS_MIGRATE.setValue(false);
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("--blank--");
        
        SharingSettings.EXTENSIONS_LIST_UNSHARED.setValue("");
        SharingSettings.EXTENSIONS_LIST_CUSTOM.setValue("hello;test;hello;hello;hello;hello");
        
        FileTypeSharingPanelManager manager = new FileTypeSharingPanelManager();
        
        manager.initCore();
        
        manager.applyOptions();
        
        assertFalse(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains(";;"));
        assertFalse(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().contains(";;"));
        assertFalse(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().contains(";;"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("test"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("doc"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains("hello"));
        assertFalse(SharingSettings.EXTENSIONS_TO_SHARE.getValue().endsWith(";"));
        assertTrue(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().contains("test"));
        assertTrue(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().contains("hello"));
        assertTrue(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().length() == 0);
        assertFalse(SharingSettings.EXTENSIONS_MIGRATE.getValue());
        
        int index;

        // Assert doubling of hello has been removed and not passed on
        assertTrue((index = SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().indexOf("hello")) > -1);
        assertFalse(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().indexOf("hello", index+1) > -1);
        assertTrue((index = SharingSettings.EXTENSIONS_TO_SHARE.getValue().indexOf("hello")) > -1);
        assertFalse(SharingSettings.EXTENSIONS_TO_SHARE.getValue().indexOf("hello", index+1) > -1);
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
        SharingSettings.EXTENSIONS_MIGRATE.setValue(false);
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("mp3;doc;custom");
        
        SharingSettings.EXTENSIONS_LIST_UNSHARED.setValue(SharingSettings.getDefaultExtensionsAsString());
        SharingSettings.EXTENSIONS_LIST_CUSTOM.setValue("");
        
        FileTypeSharingPanelManager manager = new FileTypeSharingPanelManager();
        
        manager.initCore();
        manager.applyOptions();
        
        Set<Integer> taken = new HashSet<Integer>();
        
        for ( String ext : SharingSettings.getDefaultExtensions() ) {
            int index;
            if ((index = find(StringArraySetting.decode(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue()), ext)) != -1) {
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
        
        assertFalse(SharingSettings.EXTENSIONS_TO_SHARE.getValue().contains(";;"));
        assertFalse(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().contains(";;"));
        assertFalse(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().contains(";;"));
        assertTrue(SharingSettings.EXTENSIONS_TO_SHARE.getValue().length() == 0);
        assertTrue(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().length() == 0);
        assertFalse(SharingSettings.EXTENSIONS_MIGRATE.getValue());
        


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
