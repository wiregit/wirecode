package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.Arrays;

/**
 * Various static routines for solving endian problems.
 */
public class ByteOrder {
    /**
     * Returns the reverse of x.
     */
    public static byte[] reverse(byte[] x) {
        int n=x.length;
        byte[] ret=new byte[n];
        for (int i=0; i<n; i++) 
            ret[i]=x[n-i-1];
        return ret;
    }

    /** 
     *  Little-endian bytes to short
     * 
     * @requires x.length-offset>=2
     * @effects returns the value of x[offset..offset+2] as a short, 
     *   assuming x is interpreted as a signed little endian number (i.e., 
     *   x[offset] is LSB).  If you want to interpret it as an unsigned number,
     *   call ubytes2int on the result.
     */
    public static short leb2short(byte[] x, int offset) {
        int x0=(x[offset]) & 0x000000FF;
        int x1=(x[offset+1]<<8);
        return (short)(x1|x0);
    }
    
    /** 
     *  Little-endian bytes to short - stream version 
     * 
     */
    public static short leb2short(InputStream is) throws IOException {
        int x0=is.read() & 0x000000FF;
        int x1=is.read() <<8;
        return (short)(x1|x0);
    }

    /** 
     * Little-endian bytes to int
     * 
     * @requires x.length-offset>=4
     * @effects returns the value of x[offset..offset+4] as an int, 
     *   assuming x is interpreted as a signed little endian number (i.e., 
     *   x[offset] is LSB) If you want to interpret it as an unsigned number,
     *   call ubytes2int on the result.
     */
    public static int leb2int(byte[] x, int offset) {
        //Must mask value after left-shifting, since case from byte
        //to int copies most significant bit to the left!
        int x0=x[offset] & 0x000000FF;
        int x1=(x[offset+1]<<8) & 0x0000FF00;
        int x2=(x[offset+2]<<16) & 0x00FF0000;
        int x3=(x[offset+3]<<24);
        return x3|x2|x1|x0;
    }
    
    /** 
     * Little-endian bytes to int - stream version
     * 
     */
    public static int leb2int(InputStream is) throws IOException{
        //Must mask value after left-shifting, since case from byte
        //to int copies most significant bit to the left!
        int x0=is.read() & 0x000000FF;
        int x1=(is.read()<<8) & 0x0000FF00;
        int x2=(is.read()<<16) & 0x00FF0000;
        int x3=(is.read()<<24);
        return x3|x2|x1|x0;
    }

    /** 
     * Little-endian bytes to int.  Unlike leb2int(x, offset), this version can
     * read fewer than 4 bytes.  If n<4, the returned value is never negative.
     * 
     * @param x the source of the bytes
     * @param offset the index to start reading bytes
     * @param n the number of bytes to read, which must be between 1 and 4, 
     *  inclusive
     * @return the value of x[offset..offset+N] as an int, assuming x is 
     *  interpreted as an unsigned little-endian number (i.e., x[offset] is LSB). 
     * @exception IllegalArgumentException n is less than 1 or greater than 4
     * @exception IndexOutOfBoundsException offset<0 or offset+n>x.length
     */
    public static int leb2int(byte[] x, int offset, int n) 
            throws IndexOutOfBoundsException, IllegalArgumentException {
        if (n<1 || n>4)
            throw new IllegalArgumentException("No bytes specified");

        //Must mask value after left-shifting, since case from byte
        //to int copies most significant bit to the left!
        int x0=x[offset] & 0x000000FF;
        int x1=0;
        int x2=0;
        int x3=0;
        if (n>1) {
            x1=(x[offset+1]<<8) & 0x0000FF00;
            if (n>2) {
                x2=(x[offset+2]<<16) & 0x00FF0000;
                if (n>3)
                    x3=(x[offset+3]<<24);               
            }
        }
        return x3|x2|x1|x0;
    }


    /**
     * Short to little-endian bytes: writes x to buf[offset...]
     */
    public static void short2leb(short x, byte[] buf, int offset) {
        buf[offset]=(byte)(x & (short)0x00FF);
        buf[offset+1]=(byte)((short)(x>>8) & (short)0x00FF);
    }

    /**
     * Short to little-endian bytes: writes x to given stream
     */
    public static void short2leb(short x, OutputStream os) throws IOException {
        os.write((byte)(x & (short)0x00FF));
        os.write((byte)((short)(x>>8) & (short)0x00FF));
    }

    /**
     * Int to little-endian bytes: writes x to buf[offset..]
     */
    public static void int2leb(int x, byte[] buf, int offset) {
        buf[offset]=(byte)(x & 0x000000FF);
        buf[offset+1]=(byte)((x>>8) & 0x000000FF);
        buf[offset+2]=(byte)((x>>16) & 0x000000FF);
        buf[offset+3]=(byte)((x>>24) & 0x000000FF);
    }

    /**
     * Returns the minimum number of bytes needed to encode x in little-endian 
     * format, assuming x is non-negative.  Note that leb2int(int2leb(x))==x.
     * @param x a non-negative integer
     * @exception IllegalArgumentException x is negative
     */
    public static byte[] int2minLeb(int x) throws IllegalArgumentException {
        if (x<0)
            throw new IllegalArgumentException();

        ByteArrayOutputStream baos=new ByteArrayOutputStream(4);
        do {
            baos.write(x & 0xFF);
            x>>=8;
        } while (x!=0);
        return baos.toByteArray();
    }

    /**
     * Interprets the value of x as an unsigned byte, and returns 
     * it as integer.  For example, ubyte2int(0xFF)==255, not -1.
     */
    public static int ubyte2int(byte x) {
        return ((int)x) & 0x000000FF;
    }

    /**
     * Interprets the value of x as am unsigned two-byte number
     */
    public static int ubytes2int(short x) {
        return ((int)x) & 0x0000FFFF;
    }


    /**
     * Interprets the value of x as an unsigned two-byte number
     */
    public static long ubytes2long(int x) {
        return ((long)x) & 0x00000000FFFFFFFFl;
    }

    /**
     * Returns the int value that is closest to l.  That is, if l can fit into a
     * 32-bit unsigned number, returns (int)l.  Otherwise, returns either 
     * Integer.MAX_VALUE or Integer.MIN_VALUE as appropriate.
     */
    public static int long2int(long l) {
        if (l>=Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else if (l<=Integer.MIN_VALUE)
            return Integer.MIN_VALUE;
        else
            return (int)l;
    }

    /** Unit test */
    public static void main(String args[]) {
        byte[] x1={(byte)0x2, (byte)0x1};  //{x1[0], x1[1]}
        short result1=leb2short(x1,0);
        Assert.that(result1==(short)258, "result1="+result1 );  //256+2;
        byte[] x1b=new byte[2];
        short2leb(result1, x1b, 0);
        for (int i=0; i<2; i++) 
            Assert.that(x1b[i]==x1[i]);

        byte[] x2={(byte)0x2, (byte)0, (byte)0, (byte)0x1};
        //2^24+2 = 16777216+2 = 16777218
        int result2=leb2int(x2,0);
        Assert.that(result2==16777218, "result2="+result2);

        byte[] x2b=new byte[4];
        int2leb(result2, x2b, 0);
        for (int i=0; i<4; i++) 
            Assert.that(x2b[i]==x2[i]);
    
        byte[] x3={(byte)0x00, (byte)0xF3, (byte)0, (byte)0xFF};
        int result3=leb2int(x3,0);
        Assert.that(result3==0xFF00F300, Integer.toHexString(result3));

        byte[] x4={(byte)0xFF, (byte)0xF3};
        short result4=leb2short(x4,0);
        Assert.that(result4==(short)0xF3FF, Integer.toHexString(result4));

        byte in=(byte)0xFF; //255 if unsigned, -1 if signed.
        int out=(int)in;
        Assert.that(out==-1, out+"");
        out=ubyte2int(in);
        Assert.that(out==255, out+"");
        out=ubyte2int((byte)1);
        Assert.that(out==1, out+"");

        short in2=(short)0xFFFF;
        Assert.that(in2<0,"L122");
        Assert.that(ubytes2int(in2)==0x0000FFFF, "L123");
        Assert.that(ubytes2int(in2)>0, "L124");
    
        int in3=(int)0xFFFFFFFF;
        Assert.that(in3<0, "L127");
        Assert.that(ubytes2long(in3)==0x00000000FFFFFFFFl, "L128");
        Assert.that(ubytes2long(in3)>0, "L129");

        byte[] buf={(byte)0xFF, (byte)0xFF};
        in2=leb2short(buf,0);
        Assert.that(in2==-1, "L133: "+Integer.toHexString(in2));
        int out2=ubytes2int(in2);
        Assert.that(out2==0x0000FFFF, "L134: "+out);

        byte[] buf2={(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
        in3=leb2int(buf2,0);
        Assert.that(in3==-1, "L139");
        long out4=ubytes2long(in3);
        Assert.that(out4==0x00000000FFFFFFFFl, "L141");
        
        Assert.that(long2int(5l)==5);
        Assert.that(long2int(-10l)==-10);
        Assert.that(long2int(0l)==0);
        Assert.that(long2int(0xABFFFFFFFFl)==0x7FFFFFFF);  //Integer.MAX_VALUE
        Assert.that(long2int(-0xABFFFFFFFFl)==0x80000000); //Integer.MIN_VALUE
        
        testInt2MinLeb();
        testLeb2Int();
    }

    public static void testInt2MinLeb() {
        try {
            int2minLeb(-1);
            Assert.that(false);
        } catch (IllegalArgumentException e) { }

        Assert.that(Arrays.equals(int2minLeb(0), new byte[] {(byte)0}));
        Assert.that(Arrays.equals(int2minLeb(1), new byte[] {(byte)1}));
        Assert.that(Arrays.equals(int2minLeb(7), new byte[] {(byte)7}));
        Assert.that(Arrays.equals(int2minLeb(72831), 
            new byte[] {(byte)0x7f, (byte)0x1c, (byte)0x1}));
        Assert.that(Arrays.equals(int2minLeb(731328764), 
            new byte[] {(byte)0xFC, (byte)0x30, (byte)0x97, (byte)0x2B}));
        //TODO: consider renaming this method int2leb
    }

    public static void testLeb2Int() {
        Assert.that(leb2int(new byte[] {(byte)0}, 0, 1)==0);
        Assert.that(leb2int(new byte[] {(byte)1}, 0, 1)==1);
        Assert.that(leb2int(new byte[] {(byte)7}, 0, 1)==7);
        Assert.that(leb2int(new byte[] {(byte)0x7f, (byte)0x1c, (byte)0x1}, 
                            0, 
                            3)==0x11c7f);
        Assert.that(leb2int(new byte[] {(byte)0x7f, (byte)0x1c, (byte)0x1}, 
                            1, 
                            1)==0x1c);
        //TODO: expand tests to cover exceptional cases and negative values
    }
}
