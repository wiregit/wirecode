package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.*;
import java.util.*;
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

        //Get query string
        String query=((QueryRequest)m).getQuery();
        if (query.length() < MAX_HIGHBITS) //An optimization
            return true;
        byte[] bytes=query.getBytes();
        if (bytes.length==0)               //Not really needed
            return true;
        
        //Counts bytes with high bit set.
        int highbits=0;
        for (int i=0; i<bytes.length && highbits<MAX_HIGHBITS; i++) {
            if (((bytes[i] & 0x80)!=0)) { //1000 0000
                highbits++;
            }
        }
        
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
