package com.limegroup.gnutella;

import junit.framework.*;
import java.io.*;

public class FileManagerTest extends TestCase {

	private static File _testDir;

    public FileManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(FileManagerTest.class);
    }

	//<<<<<<< FileManagerTest.java
    //File directory=null;
    //public void setUp() {        
	//  directory=new File("FileManagerTest_dir");
	//  directory.mkdirs();
    //}

    //public void tearDown() {
	//  directory.delete();
    //}
	//=======
	protected void setUp() {
		String userDir = System.getProperty("user.dir");
		_testDir = new File(userDir, "FileManagerTest");
		_testDir.mkdirs();
		_testDir.deleteOnExit();
	}

	//>>>>>>> 1.1.14.1

    /** Unit test.  REQUIRES JAVA2 FOR createTempFile Note that many tests are
     *  STRONGER than required by the specifications for simplicity.  For
     *  example, we assume an order on the values returned by getSharedFiles. */
    public void testLegacy() {
        //Test some of add/remove capability
        File f1=null;
        File f2=null;
        File f3=null;
        File f4=null;
        File f5=null;
        File f6=null;
        try {
            f1=createNewTestFile(1);
            File directory=FileManager.getParentFile(f1);
            FileManager fman=new FileManager();
            File[] files=fman.getSharedFiles(directory);
            //assertTrue(files==null);
			assertNotNull("directory should not be null", directory);
			assertTrue("directory should be a directory", directory.isDirectory());

            //One file
            SettingsManager settings=SettingsManager.instance();
            settings.setExtensions("XYZ");
            settings.setDirectories(new File[] {directory});
            //Since we don't have a non-blocking loadSettings method, we just
            //wait a little time and cross our fingers.
            fman.loadSettings(false);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { }
            f2=createNewTestFile(3);
            f3=createNewTestFile(11);

            assertEquals("The number of shared files should be 1", 1, fman.getNumFiles());
            assertTrue(fman.getSize()+"", fman.getSize()==1);
            Response[] responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            assertEquals("there should only be one response", 1, responses.length);
            assertTrue("should have not been able to remove file", 
					   !fman.removeFileIfShared(f3));
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            assertEquals("there should only be one response", 1, responses.length);
            //assertTrue(responses.length==1);
            assertTrue(fman.getSize()==1);
            assertTrue(fman.getNumFiles()==1);
            fman.get(0);
            files=fman.getSharedFiles(directory);
            assertTrue(files.length==1);
            assertTrue(files[0]+" differs from "+f1, files[0].equals(f1));
            files=fman.getSharedFiles(FileManager.getParentFile(directory));
            assertTrue(files==null);

            //Two files
            assertTrue(fman.addFileIfShared(new File("C:\\bad.ABCDEF"))==false);
            assertTrue(fman.addFileIfShared(f2)==true);
            assertTrue(fman.getNumFiles()+"", fman.getNumFiles()==2);
            assertTrue(fman.getSize()+"", fman.getSize()==4);
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            assertTrue(responses[0].getIndex()!=responses[1].getIndex());
            for (int i=0; i<responses.length; i++) {
                assertTrue(responses[i].getIndex()==0
                               || responses[i].getIndex()==1);
            }
            files=fman.getSharedFiles(directory);
            assertTrue(files.length==2);
            assertTrue(files[0]+" differs from "+f1, files[0].equals(f1));
            assertTrue(files[1]+" differs from "+f2, files[1].equals(f2));

            //Remove file that's shared.  Back to 1 file.                        
            assertTrue(fman.removeFileIfShared(f3)==false);
            assertTrue(fman.removeFileIfShared(f2)==true);
            assertTrue(fman.getSize()==1);
            assertTrue(fman.getNumFiles()==1);
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            assertTrue(responses.length==1);
            files=fman.getSharedFiles(directory);
            assertTrue(files.length==1);
            assertTrue(files[0]+" differs from "+f1, files[0].equals(f1));

            //Add a new second file, with new index.
            assertTrue(fman.addFileIfShared(f3)==true);
            assertTrue("size of files: "+fman.getSize(), fman.getSize()==12);
            assertTrue("# files: "+fman.getNumFiles(), fman.getNumFiles()==2);
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            assertTrue("response: "+responses.length, responses.length==2);
            assertTrue(responses[0].getIndex()!=1);
            assertTrue(responses[1].getIndex()!=1);
            fman.get(0);
            fman.get(2);
            try {
                fman.get(1);
                assertTrue(false);
            } catch (IndexOutOfBoundsException e) { }

            responses=fman.query(new QueryRequest((byte)3,0,"*unit*"));
            assertTrue("response: "+responses.length, responses.length==2);

            files=fman.getSharedFiles(directory);
            assertTrue(files.length==2);
            assertTrue(files[0]+" differs from "+f1, files[0].equals(f1));
            assertTrue(files[1]+" differs from "+f3, files[1].equals(f3));
            files=fman.getSharedFiles(null);
            assertTrue(files.length==2);
            assertTrue(files[0]+" differs from "+f1, files[0].equals(f1));
            assertTrue(files[1]+" differs from "+f3, files[1].equals(f3));


            //Rename files
            assertTrue(fman.renameFileIfShared(f2, f2)==false);
            assertTrue(fman.renameFileIfShared(f1, f2)==true);
            files=fman.getSharedFiles(directory);
            assertTrue(files.length==2);
            assertTrue(files[0].equals(f3));
            assertTrue(files[1].equals(f2));
            assertTrue(
                fman.renameFileIfShared(f2,
                                        new File("C\\garbage.XSADF"))==false);
            files=fman.getSharedFiles(directory);
            assertTrue(files.length==1);
            assertTrue(files[0].equals(f3));

            //Try to add a huge file.  (It will be ignored.)
            f4=createFakeTestFile(Integer.MAX_VALUE+1l);
            assertTrue(fman.addFileIfShared(f4)==false);
            assertTrue(fman.getNumFiles()==1);
            assertTrue(fman.getSize()==11);
            //Add really big files.
            f5=createFakeTestFile(Integer.MAX_VALUE-1);
            f6=createFakeTestFile(Integer.MAX_VALUE);
            assertTrue(fman.addFileIfShared(f5)==true);
            assertTrue(fman.addFileIfShared(f6)==true);
            assertTrue(fman.getNumFiles()==3);
            assertTrue(fman.getSize()==Integer.MAX_VALUE);
            responses=fman.query(new QueryRequest((byte)3, (byte)0, "*.*"));
            assertTrue(responses.length==3);
            assertTrue(responses[0].getName().equals(f3.getName()));
            assertTrue(responses[1].getName().equals(f5.getName()));
            assertTrue(responses[2].getName().equals(f6.getName()));
        } finally {
            if (f1!=null) f1.delete();
            if (f2!=null) f2.delete();
            if (f3!=null) f3.delete();
            if (f4!=null) f4.delete();
            if (f5!=null) f5.delete();
            if (f6!=null) f6.delete();
        }
    }


    static File createNewTestFile(int size) {
        try {		   
			File file = File.createTempFile("FileManager_unit_test", ".XYZ", _testDir);
			file.deleteOnExit();
            OutputStream out=new FileOutputStream(file);
            out.write(new byte[size]);
            out.flush();
            out.close();

            //Needed for comparisons between "C:\Progra~1" and "C:\Program Files".			
            return FileManager.getCanonicalFile(file);
        } catch (Exception e) {
			fail("unexpected exception in createNewTestFile: "+e);
            return null; //never executed
        }
    }

    /** Same a createNewTestFile but doesn't actually allocate the requested
     *  number of bytes on disk.  Instead returns a subclass of File. */
    File createFakeTestFile(long size) {
        File real=createNewTestFile(1);
        return new HugeFakeFile(real.getParentFile(), real.getName(), size);       
    }

    private static class HugeFakeFile extends File {
        long length;

        public HugeFakeFile(File directory, String name, long length) {
            super(directory, name);
            this.length=length;
        }

        public long length() {
            return length;
        }
    }
}
