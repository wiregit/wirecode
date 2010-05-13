package com.limegroup.gnutella;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;

public class AskInstallCheckerTest extends LimeTestCase {

    private AskInstallChecker checker;
    private File file;
    
    public AskInstallCheckerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AskInstallCheckerTest.class);
    }
    
    @Override
    protected void tearDown() throws Exception {
        if (file!=null) file.delete();
    }
       
    public void testFileDoesNotExist() throws Exception {
        file = null;
        checker = new AskInstallChecker(new AskInstallCheckerSettingsMock());
        
        assertEquals(AskInstallChecker.ASK_NOT_INSTALLED, checker.readToolbarResult());
    }
    
    public void testFileIsEmpty() throws Exception {
        file = createTestFile("");
        checker = new AskInstallChecker(new AskInstallCheckerSettingsMock());
        
        assertEquals(AskInstallChecker.ASK_NOT_INSTALLED, checker.readToolbarResult());
    }
    
    public void testAlreadyInstalled() throws Exception {
        file = createTestFile(AskInstallChecker.ASK_NOT_INSTALLED);
        checker = new AskInstallChecker(new AskInstallCheckerSettingsMock());
        
        assertEquals(AskInstallChecker.ASK_NOT_INSTALLED, checker.readToolbarResult());
    }
    
    public void testInstalled() throws Exception {
        file = createTestFile("/tbr");
        checker = new AskInstallChecker(new AskInstallCheckerSettingsMock());
        
        assertEquals(AskInstallChecker.ASK_INSTALLED, checker.readToolbarResult());
    }
    
    public void testInstalledAndSearch() throws Exception {
        file = createTestFile("/tbr /sa");
        checker = new AskInstallChecker(new AskInstallCheckerSettingsMock());
        
        assertEquals(AskInstallChecker.ASK_INSTALLED_SEARCH, checker.readToolbarResult());
    }
    
    public void testInstalledAndHomePage() throws Exception {
        file = createTestFile("/tbr /hpr");
        checker = new AskInstallChecker(new AskInstallCheckerSettingsMock());
        
        assertEquals(AskInstallChecker.ASK_INSTALLED_HOME, checker.readToolbarResult());
    }
    
    public void testInstalledAndSearchAndHomePage() throws Exception {
        file = createTestFile("/tbr /sa /hpr");
        checker = new AskInstallChecker(new AskInstallCheckerSettingsMock());
        
        assertEquals(AskInstallChecker.ASK_INSTALLED_SEARCH_HOME, checker.readToolbarResult());
    }
    
    public void testInstalledAndSearchAndHomePageIgnoresOrder() throws Exception {
        file = createTestFile("/hpr /sa /tbr");
        checker = new AskInstallChecker(new AskInstallCheckerSettingsMock());
        
        assertEquals(AskInstallChecker.ASK_INSTALLED_SEARCH_HOME, checker.readToolbarResult());
    }
    
    public void testInstalledAndSearchAndHomePageIngoresWhiteSpace() throws Exception {
        file = createTestFile("  /tbr    /sa \t /hpr");
        checker = new AskInstallChecker(new AskInstallCheckerSettingsMock());
        
        assertEquals(AskInstallChecker.ASK_INSTALLED_SEARCH_HOME, checker.readToolbarResult());
    }
    
    private File createTestFile(String textToWrite) throws Exception {
        File file = File.createTempFile("testAskResource", "", _scratchDir);
        file.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        writer.write(textToWrite);
        writer.flush();
        writer.close();
        
        return file;
    }
    
    private class AskInstallCheckerSettingsMock extends AskInstallCheckerSettings {
        @Override
        public File getFile() {
            return file;
        }
    }
}
