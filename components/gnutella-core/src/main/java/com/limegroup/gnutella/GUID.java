package com.limegroup.gnutella;

import com.sun.java.util.collections.*;

/**
 * A 16-bit globally unique ID.  Immutable.<p>
 *
 * This provides minimal functionality now, but I hope to integrate it
 * with the rest of the system in the future when I refactor code. 
 */

public class GUID /* implements Comparable */ {
    private static int SZ=16;

    //invariant: bytes.length==SZ
    private byte[] bytes;

    public GUID(byte[] bytes) {
	Assert.that(bytes.length==SZ);
	this.bytes=bytes;
    }

    public boolean equals(Object o) {
//  	System.out.println("Calling equals on "+o.toString()
//  			 +" and "+this.toString());
	if (! (o instanceof GUID))
	    return false;
	byte[] bytes2=((GUID)o).bytes();
	for (int i=0; i<SZ; i++) 
	    if (bytes[i]!=bytes2[i])
		return false;
	return true;
    }

    public int hashCode() {
	//Just add them up
	int sum=0;
	for (int i=0; i<SZ; i++)
	    sum+=bytes[i];
	return sum;
    }

//      public int compareTo(Object o) {
//  	if (! (o instanceof GUID))
//  	    throw new ClassCastException();
//  	GUID other=(GUID)o;
//  	if (this.equals(other))
//  	    return 0;
//  	else if (this.hashCode()<other.hashCode())
//  	    return -1;
//  	else
//  	    return 1;
//      }	    

    /** Warning: this exposes the rep!  Do not modify returned value. */
    public byte[] bytes() {
	return bytes;
    }

    public String toString() {
	return toHexString();
//  	StringBuffer buf=new StringBuffer();
//  	for (int i=0; i<2; i++)
//  	    buf.append(bytes[i]+".");	  
//  	buf.append("..");
//  	for (int i=14; i<SZ-1; i++) 
//  	    buf.append(bytes[0]+".");
//  	buf.append(bytes[SZ-1]+"");
//  	return buf.toString();
    }

    /** 
     *  Create a hex version of a GUID for compact display and storage
     *  Note that the client guid should be read in with the 
     *  Integer.parseByte(String s, int radix)  call like this in reverse
     */
    public String toHexString() {
	StringBuffer buf=new StringBuffer();
	String       str;
	int val;
	for (int i=0; i<SZ; i++)
	{
	    //Treating each byte as an unsigned value ensures
	    //that we don't str doesn't equal things like 0xFFFF...
	    val = ByteOrder.ubyte2int(bytes[i]);
	    str = Integer.toHexString(val);	  
	    while ( str.length() < 2 )
		str = "0" + str;
	    buf.append( str );	  
	}
	return buf.toString().toUpperCase();
    }


    /** 
     *  Create a GUID bytes from a hex string version.
     *  Throws IllegalArgumentException if sguid is
     *  not of the proper format.
     */
    public static byte[] fromHexString(String sguid)
    throws IllegalArgumentException {
	byte bytes[] = new byte[SZ];
	try {
	    for (int i=0; i<SZ; i++)
		{
		    bytes[i] = 
			(byte)Integer.parseInt(sguid.substring(i*2,(i*2)+2), 16);
		}
	    return bytes;
	} catch (NumberFormatException e) {
	    throw new IllegalArgumentException();
	} catch (IndexOutOfBoundsException e) {
	    throw new IllegalArgumentException();
	}
    }

//      public static void main(String args[]) {
//  	byte[] b1=new byte[16];
//  	byte[] b2=new byte[16];
//  	for (int i=0; i<16; i++) {
//  	    b1[i]=(byte)i;
//  	    b2[i]=(byte)i;
//  	}
//  	GUID g1=new GUID(b1);
//  	GUID g2=new GUID(b1);
//  	Assert.that(g1.equals(g2));
//  	Assert.that(g2.equals(g1));
//  	Assert.that(g1.hashCode()==g2.hashCode());

//  	Hashtable t=new Hashtable();	
//  	String out=null;
//  	t.put(g1,"test");
//  	Assert.that(t.containsKey(g1),"Contains 1");
//  	out=(String)t.get(g1);
//  	Assert.that(out!=null, "Null test 1");
//  	Assert.that(out.equals("test"), "Get test 1");

//  	Assert.that(t.containsKey(g2),"Contains 2");
//  	out=(String)t.get(g2);
//  	Assert.that(out!=null, "Null test 2");
//  	Assert.that(out.equals("test"), "Get test 2");

//  	String hexString="FF010A00000000000000000000000001";
//  	byte[] bytes=new byte[16];
//  	bytes[0]=(byte)255;
//  	bytes[1]=(byte)1;
//  	bytes[2]=(byte)10;
//  	bytes[15]=(byte)1;

//  	String s=(new GUID(bytes)).toHexString();
//  	Assert.that(s.equals(hexString));
//  	byte[] bytes2=GUID.fromHexString(s);	
//  	Assert.that(Arrays.equals(bytes2,bytes));

//  	try {
//  	    GUID.fromHexString("aa01");
//  	    Assert.that(false);
//  	} catch (IllegalArgumentException e) {
//  	    Assert.that(true);
//  	}

//  	try {
//  	    GUID.fromHexString("ff010a0000000000000000000000000z");
//  	    Assert.that(false);
//  	} catch (IllegalArgumentException e) {
//  	    Assert.that(true);
//  	}
//      }
}

	
