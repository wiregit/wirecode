package com.limegroup.gnutella;

import java.util.*;

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
	int val;
	for (int i=0; i<SZ; i += 4)
	{
	    val = bytes[i];
	    val = (val * 256) + bytes[i+1];
	    val = (val * 256) + bytes[i+2];
	    val = (val * 256) + bytes[i+3];
	    buf.append( Integer.toHexString(val) );	  
	}
	return buf.toString();
    }

    public static void main(String args[]) {
	byte[] b1=new byte[16];
	byte[] b2=new byte[16];
	for (int i=0; i<16; i++) {
	    b1[i]=(byte)i;
	    b2[i]=(byte)i;
	}
	GUID g1=new GUID(b1);
	GUID g2=new GUID(b1);
	Assert.that(g1.equals(g2));
	Assert.that(g2.equals(g1));
	Assert.that(g1.hashCode()==g2.hashCode());

	Hashtable t=new Hashtable();	
	String out=null;
	t.put(g1,"test");
	Assert.that(t.containsKey(g1),"Contains 1");
	out=(String)t.get(g1);
	Assert.that(out!=null, "Null test 1");
	Assert.that(out.equals("test"), "Get test 1");

	Assert.that(t.containsKey(g2),"Contains 2");
	out=(String)t.get(g2);
	Assert.that(out!=null, "Null test 2");
	Assert.that(out.equals("test"), "Get test 2");
    }
}

	
