package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import java.util.Locale;

/** 
 * Blocks spammy "ping queries" from BearShare.
 * These are identified by long length, predominance of high bits,
 * and high TTLs.  We'll be nice and let short-lived messages through.
 */
public class BearShareFilter extends SpamFilter {
    final int MAX_HIGHBITS=20;
    
    public boolean allow(Message m) {
        if (! (m instanceof QueryRequest))
            return true;
        //Ok, we'll allow some messages to get through.
        if ((m.getHops()+m.getTTL()) <= 2)
            return true;

        //Edited by Sumeet Thadani (2/27/01)
        QueryRequest qReq = (QueryRequest)m;
		String query = qReq.getQuery();
		byte[] queryBytes = query.getBytes();
		int rawQueryLength = qReq.getQuery().length();
        //Not enough bytes in payload to be above threshold
        if (rawQueryLength < MAX_HIGHBITS)
            return true;
        int highbits=0;
        byte currByte;
        for(int i=0; i<rawQueryLength; i++){
			currByte = queryBytes[i];
            if((currByte & 0x80)!=0)
                highbits++;
        }
	//Note: This method has been changed. Initially it would get the 
	//the query string and then get appropriate bytes from it. That
	//would cause it to fail on some Japanese Macs. Now we use methods
	//in the QueryRequest that get the specified bytes directly.
        //End of code added by Sumeet Thadani

        return highbits<MAX_HIGHBITS;
    }           
    
    /*
    public static void main(String args[]) {
        BearShareFilter f=new BearShareFilter();
        QueryRequest qr=null;
        String query=null;
        byte[] bytes=null;

        Assert.that(f.allow(new PingRequest((byte)3)));        

        query="ok";
        qr=new QueryRequest((byte)3, (byte)0, query);        
        Assert.that(f.allow(qr));
        
        query=new String(new byte[30]);
        qr=new QueryRequest((byte)3, (byte)0, query);
        Assert.that(f.allow(qr));        

        bytes=new byte[30];
        for (int i=0; i<bytes.length; i++)
            bytes[i]=(byte)0xF1;
        bytes[7]=0;
        query=new String(bytes);
        qr=new QueryRequest((byte)3, (byte)0, query);
        Assert.that(! f.allow(qr));
        qr=new QueryRequest((byte)2, (byte)0, query);
        Assert.that(f.allow(qr));
    }
    */
}
