package com.limegroup.gnutella.downloader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.tigertree.HashTree;

public class TestFile {
    private static final int A_INT=0x4F1BBCDC;
    private static URN myHash = null;
    private static HashTree myTree = null;
    static {
        hash();
        tree();
    }

    public static byte getByte(int i) {
        //Generates a consistent stream of psuedorandom bytes
        //This has cycles, but its ok.  Stolen from HashFunction.
        int bits = 7;
        long prod = (long)i * (long)A_INT;
        long ret = prod << 32;
        ret = ret >>> (32 + (32 - bits));
        return (byte)ret;
    }

    public static int length() {
        return 1000000; //1MB;
        //return 100000;    //100 KB;
    }

    public static synchronized URN hash() {
        if( myHash == null ) {
            try {
                File tmpFile = File.createTempFile("tst", "tmp");
                writeFile(tmpFile);
                myHash = URN.createSHA1Urn(tmpFile);
            } catch(Throwable e ) {
                throw new RuntimeException(e);
            }
        } 
        return myHash;
    }
    
    public static synchronized HashTree tree() {
        if( myTree == null ) {
            try {
                URN hash = hash();
                File tmpFile = File.createTempFile("tst2", "tmp");
                writeFile(tmpFile);
                myTree = createHashTree(tmpFile, hash);
            } catch(Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return myTree;
    }
    
    private static void writeFile(File tmpFile) throws IOException {
        tmpFile.deleteOnExit();
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(tmpFile));
        for(int i=0; i<TestFile.length(); i++)
            os.write(TestFile.getByte(i));
        os.flush();
        os.close();
    }
    
    private static HashTree createHashTree(File file, URN sha1)
      throws Throwable {
        Object ret = null;
        try {
            ret = PrivilegedAccessor.invokeMethod(
                HashTree.class, "createHashTree", 
                new Object[] { new Long(file.length()), new FileInputStream(file),
                            sha1 },
                new Class[] { long.class, InputStream.class, URN.class }
            );
        } catch(InvocationTargetException ite) {
            throw ite.getCause();
        }
        return (HashTree)ret;
    }    
    
    
    public static void main(String[] args) {
        System.out.println( hash().toString() );
    }
}
