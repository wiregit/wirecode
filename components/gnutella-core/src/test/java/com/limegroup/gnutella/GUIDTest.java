package com.limegroup.gnutella;

import junit.framework.*;
import com.sun.java.util.collections.*;

/**
 * Unit tests for GUID.
 */
public class GUIDTest extends TestCase {
    private byte[] bytes;
    private byte[] b1;
    private byte[] b2;
    private GUID g1;
    private GUID g2;
    private short tag;
    private short s1;
    private short s2;

    public GUIDTest(String name) {
        super(name);
    }

    public void setUp() {
        //Prevents NullPointerException's in testIsX.
        //Otherwise not needed.
        bytes=new byte[16];
    }

    public void testEquals() {
        b1=new byte[16];
        b2=new byte[16];
        for (int i=0; i<16; i++) {
            b1[i]=(byte)i;
            b2[i]=(byte)i;
        }
        g1=new GUID(b1);
        g2=new GUID(b1);
        assertTrue(g1.equals(g2));
        assertTrue(g2.equals(g1));
        assertTrue(g1.hashCode()==g2.hashCode());
    
        Hashtable t=new Hashtable();
        String out=null;
        t.put(g1,"test");
        assertTrue("Contains 1", t.containsKey(g1));
        out=(String)t.get(g1);
        assertTrue("Null test 1", out!=null);
        assertTrue("Get test 1", out.equals("test"));

        assertTrue("Contains 2", t.containsKey(g2));
        out=(String)t.get(g2);
        assertTrue("Null test 2", out!=null);
        assertTrue("Get test 2", out.equals("test"));
    }

    public void testHexStrings() {
        String hexString="FF010A00000000000000000000000001";
        bytes=new byte[16];
        bytes[0]=(byte)255;
        bytes[1]=(byte)1;
        bytes[2]=(byte)10;
        bytes[15]=(byte)1;

        String s=(new GUID(bytes)).toHexString();
        assertTrue(s.equals(hexString));
        byte[] bytes2=GUID.fromHexString(s);
        assertTrue(Arrays.equals(bytes2,bytes));

        try {
            GUID.fromHexString("aa01");
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            GUID.fromHexString("ff010a0000000000000000000000000z");
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }
    
    public void testMakeGUID() {
        //Try new GUID generator.
        for (int i=0; i<50; i++) {
            bytes=GUID.makeGuid();     g1=new GUID(bytes);
            assertTrue(bytes[8]==(byte)0xFF);
            assertTrue(bytes[15]==(byte)0x00);
            assertTrue(g1.isNewGUID());
            assertTrue(g1.isLimeGUID());
            assertTrue(! g1.isWindowsGUID());
            //System.out.println(g1);
        }               
    }
     
    public void testIsWindowsGUID() {
        //Try isWindowsGUID (again)
        bytes[8]=(byte)0xc0;   g1=new GUID(bytes);
        assertTrue(! g1.isWindowsGUID());
        bytes[8]=(byte)0x80;   g1=new GUID(bytes);
        assertTrue(g1.isWindowsGUID());
    }        

    public void testIsNewGUID() { 
        //Try isNewGUID (again)
        bytes[8]=(byte)0xFE;   g1=new GUID(bytes);
        assertTrue(! g1.isNewGUID());
        bytes[8]=(byte)0xFF;  bytes[15]=(byte)0x00;   g1=new GUID(bytes);
        assertTrue(g1.isNewGUID());
    }

    public void testIsLimeGUID() {
        //Try isLimeGUID (again)
        bytes[9]=(byte)0xFF;   g1=new GUID(bytes);
        assertTrue(! g1.isLimeGUID());
        bytes[4]=(byte)0x02;
        bytes[5]=(byte)0x01;
        bytes[6]=(byte)0x00;        
        bytes[7]=(byte)0x05;
        //Note everything is LITTLE endian.
        //(0x0102+2)*(0x0500+3)=0x0104*0x0503=0x5170C ==> 0x0517
        bytes[9]=(byte)0x17;
        bytes[10]=(byte)0x05;
        g1=new GUID(bytes);
        short s1=ByteOrder.leb2short(bytes, 4);
        short s2=ByteOrder.leb2short(bytes, 6);
        short tag=GUID.tag(s1, s2);
        assertTrue(Integer.toHexString(s1), s1==(short)0x0102);
        assertTrue(Integer.toHexString(s2), s2==(short)0x0500);
        assertTrue(Integer.toHexString(tag), tag==(short)0x0517);
        assertTrue(g1.isLimeGUID());
        //System.out.println(g1);
    }

    public void testRequery0GUID() {
        // Test version 0 requery marking
        //Note everything is LITTLE endian.
        //(0x0102+2)*(0x0517+3)=0x0104*0x051A=0x52E68 ==> 0x052E
        bytes=new byte[16];
        bytes[0]=(byte)0x02;
        bytes[1]=(byte)0x01;
        bytes[9]=(byte)0x17;
        bytes[10]=(byte)0x05;
        bytes[13]=(byte)0x2E;
        bytes[14]=(byte)0x05;
        s1=ByteOrder.leb2short(bytes, 0);
        s2=ByteOrder.leb2short(bytes, 9);
        tag=GUID.tag(s1,s2);
        assertTrue(Integer.toHexString(s1), s1==(short)0x0102);
        assertTrue(Integer.toHexString(s2), s2==(short)0x0517);
        assertTrue(Integer.toHexString(tag), tag==(short)0x052E);
        g1 = new GUID(bytes);
        assertTrue(g1.isLimeRequeryGUID());
        assertTrue(g1.isLimeRequeryGUID(0));
        assertTrue(! g1.isLimeRequeryGUID(1));
        assertTrue(! g1.isLimeRequeryGUID(2));
    }

    public void testRequery1GUID() {
        // Test version 1 requery marking
        //Note everything is LITTLE endian.
        //(0x0102+2)*(0x0517+3)=0x0104*0x051A=0x52E68 ==> 0x052E
        bytes=new byte[16];
        bytes[0]=(byte)0x02;
        bytes[1]=(byte)0x01;
        bytes[2]=(byte)0x17;
        bytes[3]=(byte)0x05;
        bytes[13]=(byte)0x2E;
        bytes[14]=(byte)0x05;
        s1=ByteOrder.leb2short(bytes, 0);
        s2=ByteOrder.leb2short(bytes, 2);
        tag=GUID.tag(s1,s2);
        assertTrue(Integer.toHexString(s1), s1==(short)0x0102);
        assertTrue(Integer.toHexString(s2), s2==(short)0x0517);
        assertTrue(Integer.toHexString(tag), tag==(short)0x052E);
        g1 = new GUID(bytes);
        assertTrue(g1.isLimeRequeryGUID());
        assertTrue(g1.isLimeRequeryGUID(1));
        assertTrue(! g1.isLimeRequeryGUID(0));
        assertTrue(! g1.isLimeRequeryGUID(2));
    }

    public void testRequery3GUID() {
        // Test new (version 2) requery marking
        //Note everything is LITTLE endian.
        //(0x0102+2)*(0x0517+3)=0x0104*0x051A=0x52E68 ==> 0x052E
        bytes=new byte[16];
        bytes[0]=(byte)0x02;
        bytes[1]=(byte)0x01;
        bytes[11]=(byte)0x17;
        bytes[12]=(byte)0x05;
        bytes[13]=(byte)0x2E;
        bytes[14]=(byte)0x05;
        s1=ByteOrder.leb2short(bytes, 0);
        s2=ByteOrder.leb2short(bytes, 11);
        tag=GUID.tag(s1,s2);
        assertTrue(Integer.toHexString(s1), s1==(short)0x0102);
        assertTrue(Integer.toHexString(s2), s2==(short)0x0517);
        assertTrue(Integer.toHexString(tag), tag==(short)0x052E);
        g1 = new GUID(bytes);
        assertTrue(g1.isLimeRequeryGUID());
        assertTrue(! g1.isLimeRequeryGUID(0));
        assertTrue(! g1.isLimeRequeryGUID(1));
        assertTrue(g1.isLimeRequeryGUID(2));

        // Test LimeRequeryGUID construction....
        bytes = GUID.makeGuidRequery();
        GUID gReq = new GUID(bytes);
        //System.out.println(gReq);
        assertTrue(gReq.isLimeGUID());
        assertTrue(gReq.isLimeRequeryGUID());
        assertTrue(gReq.isLimeRequeryGUID(2));
        assertTrue(! gReq.isLimeRequeryGUID(0));
        assertTrue(! gReq.isLimeRequeryGUID(1));
    }

    public void testHashAndCompare() {
        //Test hashcode, compareTo for same
        java.util.Random r=new java.util.Random();       
        b1=new byte[16];
        r.nextBytes(b1);
        b2=new byte[16];
        System.arraycopy(b1,0,b2,0,16);
        g1=new GUID(b1);
        g2=new GUID(b2);
        assertTrue(g1.compareTo(g2)==0);
        assertTrue(g2.compareTo(g1)==0);
        assertTrue(g1.hashCode()==g2.hashCode());
        //System.out.println("Hash: "+Integer.toHexString(g1.hashCode()));

        b2[7]+=1;
        g2=new GUID(b2);
        assertTrue(g1.compareTo(g2)<0);
        assertTrue((new GUID.GUIDComparator()).compare(g1, g2) < 0);
        assertTrue((new GUID.GUIDComparator()).compare(g2, g1) > 0);
        assertTrue((new GUID.GUIDByteComparator()).compare(b1, b2) < 0);
        assertTrue((new GUID.GUIDByteComparator()).compare(b2, b1) > 0);
        assertTrue(g2.compareTo(g1)>0);
        assertTrue(g1.hashCode()!=g2.hashCode());  //not strictly REQUIRED
        //System.out.println("Hash: "+Integer.toHexString(g2.hashCode()));
    }
}
