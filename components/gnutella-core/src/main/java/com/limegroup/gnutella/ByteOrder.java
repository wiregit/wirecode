package com.limegroup.gnutella;

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
     *   assuming x is interpreted in a little endian number (i.e., 
     *   x[offset] is LSB) 
     */
    public static short leb2short(byte[] x, int offset) {
	short x0=(short)x[offset];
	short x1=(short)(x[offset+1]<<8);
	return (short)(x0+x1);
    }

    /** 
     * Little-endian bytes to int
     * 
     * @requires x.length-offset>=4
     * @effects returns the value of x[offset..offset+4] as an int, 
     *   assuming x is interpreted in a little endian number (i.e., 
     *   x[offset] is LSB) 
     */
    public static int leb2int(byte[] x, int offset) {
	int x0=x[offset];
	int x1=x[offset+1]<<8;
	int x2=x[offset+2]<<16;
	int x3=x[offset+3]<<24;
	return x0+x1+x2+x3;
    }

    /**
     * Short to little-endian bytes: writes x to buf[offset...]
     */
    public static void short2leb(short x, byte[] buf, int offset) {
	buf[offset]=(byte)(x & (short)0x00FF);
	buf[offset+1]=(byte)((short)(x>>8) & (short)0x00FF);
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
    }
}
