package com.limegroup.gnutella;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.OSUtils;
import org.limewire.util.PrivilegedAccessor;

/* This extends BaseTestCase on purpose!  We don't want the overhead of LimeTestCase! */
public class LimeCoreGlueTest extends BaseTestCase {
    
    public LimeCoreGlueTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LimeCoreGlueTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    // This test requires that the ClassLoader is implemented as we expect it.
    // If a ClassLoader implementation changes, this test needs to change.
    public void testPreinstallDoesntLoadExtraClasses() throws Exception {
        
        List<Class> loaded = getLoadedClasses();
        
        assertFalse("CommonUtils can't already be loaded!", 
                    containsClass(loaded, "org.limewire.util.CommonUtils"));
        
        LimeCoreGlue.preinstall();
        
        List<Class> nextLoaded = getLoadedClasses();
        
        List<String> expected = new LinkedList<String>(Arrays.asList(new String[]
                            {"com.limegroup.gnutella.LimeCoreGlue", 
                             "org.limewire.setting.RemoteSettingManager",
                             "org.limewire.io.LocalSocketAddressProvider",
                             "org.limewire.security.AddressSecurityToken$SettingsProvider", 
                             "com.limegroup.gnutella.util.LimeWireUtils", 
                             "org.limewire.util.CommonUtils", 
                             "org.limewire.util.FileUtils", 
                             "org.limewire.util.OSUtils"}));
        if(OSUtils.isWindows() || OSUtils.isMacOSX())
            expected.add("org.limewire.util.SystemUtils");
        
        removeClasses(nextLoaded, expected);
        
        assertEquals("loaded more classes than expected" +
                     " -- make sure nothing is using CommonUtils.getUserSettingsDir too early!",
                     loaded, nextLoaded);
    }
    
    @SuppressWarnings("unchecked")
    private List<Class> getLoadedClasses() throws Exception {
        ClassLoader loader = LimeCoreGlueTest.class.getClassLoader();
        List<Class> list = new ArrayList<Class>((List<Class>)PrivilegedAccessor.getValue(loader, "classes"));
        for(Iterator<Class> i = list.iterator(); i.hasNext(); ) {
            Class next = i.next();
            if(!next.getName().startsWith("org.limewire.") && !next.getName().startsWith("com.limegroup"))
                i.remove();
        }
        return list;
    }
    
    private boolean containsClass(List<Class> classes, String expected) {
        for(Class clazz : classes)
            if(clazz.getName().equals(expected))
                return true;
        return false;
    }
    
    private void removeClasses(List<Class> classes, List<String> expected) {
        for(Iterator<Class> i = classes.iterator(); i.hasNext(); ) {
            Class next = i.next();
            if(expected.contains(next.getName())) {
                i.remove();
                expected.remove(next.getName());
            }
            
            if(expected.isEmpty())
                break;
        }
        if(!expected.isEmpty())
            fail("didn't find all expected classes: " + expected);
    }
}
