package org.limewire.http;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

public class HttpTestUtils {

    public static byte[] writeRandomData(File file, int size)
            throws IOException {
        byte[] data = new byte[size];
        Random r = new Random();
        r.nextBytes(data);
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            raf.write(data);
        } finally {
            raf.close();
        }
        return data;
    }

    public static void writeData(File file, String data)
            throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            raf.write(data.getBytes("US-ASCII"));
        } finally {
            raf.close();
        }
    }

}
