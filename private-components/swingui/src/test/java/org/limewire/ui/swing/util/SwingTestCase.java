package org.limewire.ui.swing.util;

import java.io.File;

import org.limewire.util.BaseTestCase;
import org.limewire.util.TestUtils;

/**
 * Convience methods for testing Swing components. 
 */
public abstract class SwingTestCase extends BaseTestCase {

    protected static File baseDir;
    
    public SwingTestCase(String name) {
        super(name);
    }
    
    /**
     * Called statically before any settings.
     */
    public static void beforeAllTestsSetUp() throws Throwable {        
        setupUniqueDirectories();
    }

    /**
     * Sets this test up to have unique directories.
     */
    protected static void setupUniqueDirectories() throws Exception {
        
        if( baseDir == null ) {
            baseDir = createNewBaseDirectory( _testClass.getName() );
        }

        baseDir.mkdirs();

        baseDir.deleteOnExit();
    }
    
    /**
     * Creates a new directory prepended by the given name.
     */
    protected static File createNewBaseDirectory(String name) throws Exception {
        File t = getTestDirectory();
        File f = new File(t, name);
        
        int append = 1;
        while ( f.exists() ) {
            f = new File(t, name + "_" + append);
            append++;
        }
        
        return f.getCanonicalFile();
    }
    
    /**
     * Get tests directory from a marker resource file.
     */
    protected static File getTestDirectory() throws Exception {
        return new File(getRootDir(), "testData");
    }   
    
    protected static File getRootDir() throws Exception {
        // Get a marker file.
        File f = TestUtils.getResourceFile("com/limegroup/gnutella/Backend.java");
        f = f.getCanonicalFile();
                 //gnutella       // limegroup    // com         // tests       // .
        return f.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();        
    }

}
