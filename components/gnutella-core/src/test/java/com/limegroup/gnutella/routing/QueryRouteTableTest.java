package com.limegroup.gnutella.routing;

import java.util.BitSet;
import java.io.*;
import java.util.zip.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;
import junit.framework.*;

public class QueryRouteTableTest extends TestCase {
    public QueryRouteTableTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(QueryRouteTableTest.class);
    }

    public void assertTrue(boolean test, String out) {
        assertTrue(out, test);
    }

    /** May return null!
     */ 
    private BitSet getBitTable(QueryRouteTable source) {
        BitSet retSet = null;
        try {
            retSet = (BitSet) PrivilegedAccessor.getValue(source,
                                                          "bitTable");
        }
        catch (Exception ignored) {}
        return retSet;
    }

    /** If it can't get anything, will return 0.
     */ 
    private int getBitTableLength(QueryRouteTable source) {
        int retLength = 0;
        try {
            Integer intObj = 
               (Integer)PrivilegedAccessor.getValue(source,
                                                    "bitTableLength");
            retLength = intObj.intValue();
        }
        catch (Exception ignored) {}
        return retLength;
    }


    /** If it can't get anything, will return 0.
     */ 
    private void setUncompressor(QueryRouteTable source, Inflater inflater) {
        try {
            PrivilegedAccessor.setValue(source, "uncompressor", inflater);
        }
        catch (Exception ignored) {}
    }

    
    private byte[] invokeCompress(QueryRouteTable source, byte[] chunk) {
        byte[] retBytes = new byte[0];
        try {
            retBytes = 
            (byte[]) PrivilegedAccessor.invokeMethod(source, "compress", chunk);
        }        
        catch (Exception ignored) {}
        return retBytes;
    }


    private byte[] invokeUncompress(QueryRouteTable source, byte[] chunk) {
        byte[] retBytes = new byte[0];
        try {
            retBytes = 
            (byte[]) PrivilegedAccessor.invokeMethod(source, "uncompress", chunk);
        }        
        catch (Exception ignored) {}
        return retBytes;
    }


    private byte[] invokeHalve(byte[] chunk) {
        byte[] retBytes = new byte[0];
        try {
            retBytes = 
            (byte[]) PrivilegedAccessor.invokeMethod(QueryRouteTable.class, 
                                                     "halve", chunk);
        }        
        catch (Exception ignored) {}
        return retBytes;
    }


    private byte[] invokeUnhalve(byte[] chunk) {
        byte[] retBytes = new byte[0];
        try {
            retBytes = 
            (byte[]) PrivilegedAccessor.invokeMethod(QueryRouteTable.class, 
                                                     "unhalve", chunk);
        }        
        catch (Exception ignored) {}
        return retBytes;
    }


    public void testLegacy() {
        //TODO: test handle bad packets (sequences, etc)

        //-1. Just for sanity's sake....
        assertTrue(QueryRouteTable.KEYWORD_PRESENT == (byte)-6);
        assertTrue(QueryRouteTable.KEYWORD_ABSENT  == (byte)6);

        //0. compress/uncompress.  First we make a huge array with lots of
        //random bytes but also long strings of zeroes.  This means that
        //compression will work, but not too well.  Then we take the compressed
        //value and dice it up randomly.  It's critical to make sure that
        //decompress works incrementally without blocking.
        QueryRouteTable dummy=new QueryRouteTable();
        setUncompressor(dummy, new Inflater());
        byte[] data=new byte[10000];
        Random rand=new Random();
        rand.nextBytes(data);
        for (int i=100; i<7000; i++) {
            data[i]=(byte)0;
        }
        byte[] dataCompressed=invokeCompress(dummy, data);
        assertTrue(dataCompressed.length<data.length);
        
        try {
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            for (int i=0; i<dataCompressed.length; ) {
                int length=Math.min(rand.nextInt(100), dataCompressed.length-i);
                byte[] chunk=new byte[length];
                System.arraycopy(dataCompressed, i, chunk, 0, length);
                byte[] chunkRead=invokeUncompress(dummy, chunk);
                baos.write(chunkRead);
                i+=length;
            }
            baos.flush();
            assertTrue(Arrays.equals(data, baos.toByteArray()),
                        "Compress/uncompress loop failed");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //0.1. halve/unhalve
        assertTrue(QueryRouteTable.extendNibble((byte)0x03)==0x03);
        assertTrue(QueryRouteTable.extendNibble((byte)0x09)==(byte)0xF9);
        byte[] big={(byte)1, (byte)7, (byte)-1, (byte)-8};
        byte[] small={(byte)0x17, (byte)0xF8};
        assertTrue(Arrays.equals(QueryRouteTable.halve(big), small));
        assertTrue(Arrays.equals(QueryRouteTable.unhalve(small), big));

        QueryRouteTable qrt=new QueryRouteTable(1000);
        qrt.add("good book");
        assertTrue(qrt.entries()==2);
        qrt.add("bad");   //{good, book, bad}
        assertTrue(qrt.entries()==3);
        qrt.add("bad");   //{good, book, bad}
        assertTrue(qrt.entries()==3);

        //1. Simple keyword tests (add, contains)
        //we have moved to 1-bit entry per hash, so either absent or present....
        assertTrue(! qrt.contains(new QueryRequest((byte)4, 0, "garbage",
                                                   false)));
        assertTrue(qrt.contains(new QueryRequest((byte)2, 0, "bad", false)));
        assertTrue(qrt.contains(new QueryRequest((byte)3, 0, "bad", false)));
        assertTrue(qrt.contains(new QueryRequest((byte)4, 0, "bad", false)));
        assertTrue(qrt.contains(new QueryRequest((byte)2, 0, "good bad", 
                                                 false)));
        assertTrue(! qrt.contains(new QueryRequest((byte)3, 0, "good bd", 
                                                   false)));
        assertTrue(qrt.contains(new QueryRequest((byte)3, 0, 
                                                  "good bad book", false)));
        assertTrue(! qrt.contains(new QueryRequest((byte)3, 0, 
                                                    "good bad bok", false)));

        //2. addAll tests
        QueryRouteTable qrt2=new QueryRouteTable(1000);
        assertTrue(qrt2.entries()==0);
        qrt2.add("new");
        qrt2.add("book");
        qrt2.addAll(qrt);     //{book, good, new, bad}
        QueryRouteTable qrt3=new QueryRouteTable(1000);
        assertTrue(qrt2.entries()==4);
        qrt3.add("book");
        qrt3.add("good");
        qrt3.add("new");
        qrt3.add("bad");
        assertTrue(qrt2.equals(qrt3));
        assertTrue(qrt3.equals(qrt2));

        //3. encode-decode test--with compression
        //qrt={good, book, bad}
        qrt2=new QueryRouteTable(1000);
        for (Iterator iter=qrt.encode(null); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            try { 
                qrt2.update(m); 
            } catch (BadPacketException e) {
            }
            if (m instanceof PatchTableMessage)
                assertTrue(((PatchTableMessage)m).getCompressor()
                    ==PatchTableMessage.COMPRESSOR_DEFLATE);
        }
        assertTrue(qrt2.equals(qrt), "Got \n    "+qrt2+"\nexpected\n    "+qrt);

        qrt.add("bad");
        qrt.add("other"); //qrt={good, book, bad, other}
        assertTrue(! qrt2.equals(qrt));
        for (Iterator iter=qrt.encode(qrt2); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            try { 
                qrt2.update(m); 
            } catch (BadPacketException e) {
            }
            if (m instanceof PatchTableMessage)
                assertTrue(((PatchTableMessage)m).getCompressor()
                    ==PatchTableMessage.COMPRESSOR_DEFLATE);
        }
        assertTrue(qrt2.equals(qrt));
        assertTrue(qrt.entries()==qrt2.entries());

        Iterator iter=qrt2.encode(qrt);
        assertTrue(! iter.hasNext());                     //test optimization

        iter=(new QueryRouteTable(1000).encode(null));  //blank table
        assertTrue(iter.next() instanceof ResetTableMessage);
        assertTrue(! iter.hasNext());
        
        //4. encode-decode test--without compression.  (We know compression
        //won't work because the table is very small and filled with random 
        //bytes.)
        qrt=new QueryRouteTable(10);
        rand=new Random();
        for (int i=0; i<getBitTableLength(qrt); i++) 
            if (rand.nextBoolean())
                getBitTable(qrt).set(i);
        getBitTable(qrt).set(0);
        qrt2=new QueryRouteTable(10);
        assertTrue(! qrt2.equals(qrt));

        for (iter=qrt.encode(qrt2); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            try { 
                qrt2.update(m); 
            } catch (BadPacketException e) {
            }
            if (m instanceof PatchTableMessage)
                assertTrue(((PatchTableMessage)m).getCompressor()
                    ==PatchTableMessage.COMPRESSOR_NONE);
        }
        assertTrue(qrt2.equals(qrt));

        //4b. Encode/decode tests with multiple patched messages.
        qrt=new QueryRouteTable(5000);
        rand=new Random();
        for (int i=0; i<getBitTableLength(qrt); i++)
            if (rand.nextBoolean())
                getBitTable(qrt).set(i);
        qrt2=new QueryRouteTable(5000);
        assertTrue(! qrt2.equals(qrt));

        for (iter=qrt.encode(qrt2); iter.hasNext(); ) {
            RouteTableMessage m=(RouteTableMessage)iter.next();
            try { 
                qrt2.update(m); 
            } catch (BadPacketException e) {
            }
        }
        assertTrue(qrt2.equals(qrt));

        //5. Interpolation/extrapolation glass-box tests.  Remember that +1 is
        //added to everything!
        qrt=new QueryRouteTable(4);  // 1 4 5 X ==> 2 6
        qrt2=new QueryRouteTable(2);
        getBitTable(qrt).set(0);
        getBitTable(qrt).set(1);
        getBitTable(qrt).set(2);
        getBitTable(qrt).clear(3);
        qrt2.addAll(qrt);
        assertTrue(getBitTable(qrt2).get(0));
        assertTrue(getBitTable(qrt2).get(1));

        //This also tests tables with different TTL problem.  (The 6 is qrt
        //is interepreted as infinity in qrt2, not a 7.)
        qrt=new QueryRouteTable(2);  // 1 X ==> 2 2 X X
        qrt2=new QueryRouteTable(4);
        getBitTable(qrt).set(0);
        getBitTable(qrt).clear(1);
        qrt2.addAll(qrt);
        assertTrue(getBitTable(qrt2).get(0));
        assertTrue(getBitTable(qrt2).get(1));
        assertTrue(!getBitTable(qrt2).get(2));
        assertTrue(!getBitTable(qrt2).get(3));

        qrt=new QueryRouteTable(4);  // 1 2 4 X ==> 2 3 5
        qrt2=new QueryRouteTable(3);
        getBitTable(qrt).set(0);
        getBitTable(qrt).set(1);
        getBitTable(qrt).set(2);
        getBitTable(qrt).clear(3);
        qrt2.addAll(qrt);
        assertTrue(getBitTable(qrt2).get(0));
        assertTrue(getBitTable(qrt2).get(1));
        assertTrue(getBitTable(qrt2).get(2));
        assertTrue(qrt2.entries()==3);

        qrt=new QueryRouteTable(3);  // 1 4 X ==> 2 2 5 X
        qrt2=new QueryRouteTable(4);
        getBitTable(qrt).set(0);
        getBitTable(qrt).set(1);
        getBitTable(qrt).clear(2);
        qrt2.addAll(qrt);
        assertTrue(getBitTable(qrt2).get(0));
        assertTrue(getBitTable(qrt2).get(1));
        assertTrue(getBitTable(qrt2).get(2));
        assertTrue(!getBitTable(qrt2).get(3));
        assertTrue(qrt2.entries()==3);

        //5b. Black-box test for addAll.
        qrt=new QueryRouteTable(128);
        qrt.add("good book");
        qrt.add("bad");   //{good/1, book/1, bad/3}
        qrt2=new QueryRouteTable(512);
        qrt2.addAll(qrt);
        assertTrue(qrt2.contains(new QueryRequest((byte)4, 0, "bad", false)));
        assertTrue(qrt2.contains(new QueryRequest((byte)4, 0, "good", false)));
        qrt2=new QueryRouteTable(32);
        qrt2.addAll(qrt);
        assertTrue(qrt2.contains(new QueryRequest((byte)4, 0, "bad", false)));
        assertTrue(qrt2.contains(new QueryRequest((byte)4, 0, "good", false)));

        //6. Test sequence numbers.
        qrt=new QueryRouteTable();   //a. wrong sequence after reset
        ResetTableMessage reset=null;
        PatchTableMessage patch=new PatchTableMessage((short)2, (short)2,
            PatchTableMessage.COMPRESSOR_DEFLATE, (byte)8, new byte[10], 0, 10);
        try {
            qrt.update(patch);
            assertTrue(false);
        } catch (BadPacketException e) { 
        }

        qrt=new QueryRouteTable();  //b. message sizes don't match
        try {
            reset=new ResetTableMessage(1024, (byte)2);
            qrt.update(reset);
            patch=new PatchTableMessage((short)1, (short)3,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
        } catch (BadPacketException e) {
            assertTrue(false);
        }
        try {
            patch=new PatchTableMessage((short)2, (short)4,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
            assertTrue(false);
        } catch (BadPacketException e) { 
        }

        qrt=new QueryRouteTable();  //c. message sequences don't match
        try {
            patch=new PatchTableMessage((short)1, (short)3,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
        } catch (BadPacketException e) {
            assertTrue(false);
        }
        try {
            patch=new PatchTableMessage((short)3, (short)3,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
            assertTrue(false);
        } catch (BadPacketException e) {
        }        

        qrt=new QueryRouteTable();  //d. sequence interrupted by reset
        try {
            patch=new PatchTableMessage((short)1, (short)3,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
            reset=new ResetTableMessage(1024, (byte)2);
            qrt.update(reset);
        } catch (BadPacketException e) {
            assertTrue(false);
        }
        try {
            patch=new PatchTableMessage((short)1, (short)6,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
        } catch (BadPacketException e) {
            assertTrue(false);
        }

        qrt=new QueryRouteTable();  //e. Sequence too big
        try {
            patch=new PatchTableMessage((short)1, (short)2,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
            patch=new PatchTableMessage((short)2, (short)2,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
        } catch (BadPacketException e) {
            assertTrue(false);
        }
        try {
            patch=new PatchTableMessage((short)3, (short)2,
                PatchTableMessage.COMPRESSOR_NONE, (byte)8, new byte[10], 0, 10);
            qrt.update(patch);
            assertTrue(false);
        } catch (BadPacketException e) {
        }
    }
}
