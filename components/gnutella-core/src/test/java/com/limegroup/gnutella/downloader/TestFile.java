package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import java.io.*;

public class TestFile {
    private static final int A_INT=0x4F1BBCDC;

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

    public static URN hash() {
        //IMPORTANT: this code is calculated with the main() method below and
        //"cached" here for convenience.  If you change any of the above
        //methods, you must change the hash.
        try {
            return URN.createSHA1Urn( 
                "urn:sha1:MTSUIEFABDVUDXZMJEBQWNI6RVYHTNIJ");
        } catch (IOException e) {
            Assert.that(false, "Legal hash rejected");
            return null;
        }
    }

    /*
    public static void main(String args[]) {
        try {
            File testFile = new File("test.txt");
            RandomAccessFile raf = new RandomAccessFile(testFile,"rw");
            for(int i=0; i<TestFile.length(); i++)
                raf.writeByte(TestFile.getByte(i));
            raf.close();
            URN testHash = URN.createSHA1Urn(testFile);
            System.out.println("Hash: "+testHash);
        } catch (InterruptedException e) {
            System.out.println("Interrupted calculating hash.");
        } catch (IOException e) { 
            System.out.println("Couldn't calculate hash!");
        }
    }
    */
}
