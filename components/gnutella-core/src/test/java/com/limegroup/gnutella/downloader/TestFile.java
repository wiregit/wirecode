package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;

public class TestFile {
    private static final int A_INT=0x4F1BBCDC;
    private static URN myHash = null;

    public static byte getByte(int i) {
        //Generates a consistent stream of psuedorandom bytes

        //Not very random
        //return (byte)(new Random(i).nextInt(256));

        //Very random, too slow
        //Random rand=new Random(0);
        //for (int j=0; j<i; j++) {
        //    rand.nextInt(256);
        //}
        //return (byte)rand.nextInt(256);

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
                tmpFile.deleteOnExit();
                RandomAccessFile raf = new RandomAccessFile(tmpFile, "rw");
                for(int i=0; i<TestFile.length(); i++)
                    raf.writeByte(TestFile.getByte(i));
                raf.close();
                myHash = URN.createSHA1Urn(tmpFile);
            } catch ( InterruptedException e) {
                ErrorService.error(e);
            } catch ( IOException e ) {
                ErrorService.error(e);
            }
        } 
        return myHash;
    }
    
    public static void main(String[] args) {
        System.out.println( hash().toString() );
    }
}
