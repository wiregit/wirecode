package com.limegroup.gnutella.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Tests the class that makes sure the user has a valid save directory.
 */
public class SaveDirectoryHandlerTest extends LimeTestCase {

    public SaveDirectoryHandlerTest(String name) {
        super(name);
    }

    
    /**
     * Tests the method for checking whether or not the save directory is 
     * valid.
     * 
     * @throws Exception if an error occurs
     */
    public void testIsSaveDirectoryValid() throws Exception {
        Method validityCheck = 
            PrivilegedAccessor.getMethod(SaveDirectoryHandler.class,  
                "isSaveDirectoryValid", new Class[] {File.class});
        
        File testFile = 
            new File("this_should_not_exist_but_we'll_delete_it_anyway");
        
        if(testFile.isFile() && !testFile.delete()) {
            fail("could not delete test file");
        }
        Object[] params = new Object[1];
        
        // make sure it doesn't accept files that don't exist
        params[0] = testFile;
        boolean valid = 
            ((Boolean)validityCheck.invoke(SaveDirectoryHandler.class, 
                params)).booleanValue();
        
        assertFalse("should not be a valid directory", valid);
        
        // make sure it doesn't accept null files
        params[0] = null;
        valid = 
            ((Boolean)validityCheck.invoke(SaveDirectoryHandler.class, 
                params)).booleanValue();
        assertFalse("should not be a valid directory", valid);
        
        // make sure it doesn't accept files that exist but are not directories.
        OutputStream os = new FileOutputStream(testFile);
        os.write(7);
        os.close();
        os.flush();
        
        // Make sure it really exists at this point.
        assertTrue("file should exist", testFile.exists());
        params[0] = testFile;
        valid = 
            ((Boolean)validityCheck.invoke(SaveDirectoryHandler.class, 
                params)).booleanValue();
        assertFalse("should not be a valid directory", valid);
        
        // Delete the file...
        assertTrue("could not delete test file", testFile.delete());
        
        // And make it a directory.
        assertTrue("could not make it a directory", testFile.mkdirs());
        
        params[0] = testFile;
        valid = 
            ((Boolean)validityCheck.invoke(SaveDirectoryHandler.class, 
                params)).booleanValue();
        assertTrue("should be a valid directory", valid);
        assertTrue("could not delete test directory", testFile.delete());
    }
}
