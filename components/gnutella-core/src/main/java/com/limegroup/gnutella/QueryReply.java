package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;

/**
 * A query reply.  Contains information about the responding host in addition to
 * an array of responses.  These responses are not parsed until the getResponses
 * method is called.  For efficiency reasons, bad query reply packets may not be
 * discovered until the getResponses methods are called.<p>
 *
 * This class has partial support for BearShare-style query reply trailers.  You
 * can extract the vendor code and push flags, but you can't create them.  These
 * methods may throw BadPacketException if the metadata cannot be extracted.
 * Note that BadPacketException does not mean that other data (namely responses)
 * cannot be read; MissingDataException might have been a better name.
 */
public class QueryReply extends Message implements Serializable{
    //Rep rationale: because most queries aren't directed to us (we'll just
    //forward them) we extract the responses lazily as needed.
    //When they are extracted, however, it makes sense to store the parsed
    //data in the responses field.
    //
    //WARNING: see note in Message about IP addresses.

    private byte[] payload;
    /** True if the responses and metadata have been extracted. */
    private volatile boolean parsed=false;        
    /** If parsed, the response records for this, or null if they could not
     *  be parsed. */
    private volatile Response[] responses=null;
    /** If parsed, the responses vendor string, if defined, or null
     *  otherwise. */
    private volatile String vendor=null;
    /** If parsed and vendor!=null, true iff the push flag is set. */
    private volatile boolean pushFlagSet;
    

    /** Creates a new query reply.  The number of responses is responses.length
     *
     *  @requires  0 < port < 2^16 (i.e., can fit in 2 unsigned bytes),
     *    ip.length==4 and ip is in <i>BIG-endian</i> byte order,
     *    0 < speed < 2^32 (i.e., can fit in 4 unsigned bytes),
     *    responses.length < 2^8 (i.e., can fit in 1 unsigned byte),
     *    clientGUID.length==16
     */
    public QueryReply(byte[] guid, byte ttl,
              int port, byte[] ip, long speed, Response[] responses,
              byte[] clientGUID) {
        super(guid, Message.F_QUERY_REPLY, ttl, (byte)0,
              //11 bytes for header, plus file records, plus 16-byte footer
              11+rLength(responses)+16);
        Assert.that((port&0xFFFF0000)==0);
        Assert.that(ip.length==4);
        Assert.that((speed&0xFFFFFFFF00000000l)==0);
        final int n=responses.length;
        Assert.that(n<256);

        payload=new byte[getLength()];
        //Write beginning of payload.
        //Downcasts are ok, even if they go negative
        payload[0]=(byte)n;
        ByteOrder.short2leb((short)port,payload,1);
        payload[3]=ip[0];
        payload[4]=ip[1];
        payload[5]=ip[2];
        payload[6]=ip[3];
        ByteOrder.int2leb((int)speed,payload,7);

        //Write each response at index i
        int i=11;
        for (int left=n; left>0; left--) {
            Response r=responses[n-left];
            ByteOrder.int2leb((int)r.getIndex(),payload,i);
            ByteOrder.int2leb((int)r.getSize(),payload,i+4);
            i+=8;
            String name=r.getName();
            byte[] nameBytes = r.getName().getBytes();
            System.arraycopy(nameBytes, 0, payload, i, nameBytes.length);
            i+=nameBytes.length;
            //Write double null terminator.
            payload[i++]=(byte)0;
            payload[i++]=(byte)0;
        }

        //Write footer at payload[i...i+16-1]
        for (int j=0; j<16; j++) {
            payload[i+j]=clientGUID[j];
        }
    }

    /**
    * Creates a new query reply from the passed query Reply. The new one is
    * same as the passed one, but with different specified GUID
    * @param guid The new GUID for the reply
    * @param reply The query reply from where to copy the fields into the
    * new constructed query reply
    * Note: The payload is not really copied, but the reference in the newly
    * constructed query reply, points to the one in the passed reply.
    * but since the payload is not meant to be
    * mutated, it shouldnt make difference if different query replies
    * maintain reference to same payload
    */
    public QueryReply(byte[] guid, QueryReply reply){
        //call the super constructor with new GUID
        super(guid, Message.F_QUERY_REPLY, reply.getTTL(), reply.getHops(),
                                                            reply.getLength());
        //set the payload field
        this.payload = reply.payload;
    }

    /** Returns the number of bytes necessary to represent responses
     * in the payload */
    private static int rLength(Response[] responses) {
        int ret=0;
        for (int i=0; i<responses.length; i++)
            //8 bytes for index and size, plus name and two null terminators
            ret += 8+responses[i].getName().length()+2;
        return ret;
    }

    /** Creates a new query reply with data read from the network. */
    public QueryReply(byte[] guid, byte ttl, byte hops,
              byte[] payload) {
        super(guid, Message.F_QUERY_REPLY, ttl, hops, payload.length);
        this.payload=payload;
        //repOk();                               
    }

    public void writePayload(OutputStream out) throws IOException {
        out.write(payload);
    }

    /** Return the number of results N in this query. */
    public short getResultCount() {
        //The result of ubyte2int always fits in a short, so downcast is ok.
        return (short)ByteOrder.ubyte2int(payload[0]);
    }

    public int getPort() {
        return ByteOrder.ubytes2int(ByteOrder.leb2short(payload,1));
    }

    /** Returns the IP address of the responding host in standard
     *  dotted-decimal format, e.g., "192.168.0.1" */
    public String getIP() {
        byte[] ip=new byte[4];
        ip[0]=payload[3];
        ip[1]=payload[4];
        ip[2]=payload[5];
        ip[3]=payload[6];
        return ip2string(ip); //takes care of signs
    }

    public long getSpeed() {
        return ByteOrder.ubytes2long(ByteOrder.leb2int(payload,7));
    }

    /** Returns an iterator that will yield the results, each as an
     *  instance of the Response class.  Throws BadPacketException if
     *  this data couldn't be extracted.  */
    public Iterator getResults() throws BadPacketException {
        parseResults();
        if (responses==null)
            throw new BadPacketException();
        List list=Arrays.asList(responses);
        return list.iterator();
    }

    /** 
     * Returns the name of this' vendor, all capitalized.  Throws
     * BadPacketException if the data couldn't be extracted, either because it
     * is missing or corrupted. 
     */
    public String getVendor() throws BadPacketException {
        parseResults();
        if (vendor==null)
            throw new BadPacketException();
        return vendor;
    }

    /** 
     * Returns true if this's push flag is set, i.e., a push download is needed.
     * Returns false if the flag is present but not set isn't set.  Throws
     * BadPacketException if the flag couldn't be extracted, either because it
     * is missing or corrupted.
     */
    public boolean getNeedsPush() throws BadPacketException {
        parseResults();
        if (vendor==null)
            throw new BadPacketException();
        return pushFlagSet;
    }


    /** @modifies this.responses, this.pushFlagSet, this.vendor, parsed
     *  @effects tries to extract responses from payload and store in responses. 
     *    Tries to extract metadata and store in vendor and pushFlagSet.
     *    You can tell if data couldn't be extracted by looking if responses
     *    or vendor is null.
     */
    private void parseResults() {
        if (parsed)
            return;
        parseResults2();
        parsed=true;
    }

    private void parseResults2() {
        //index into payload to look for next response
        int i=11;

        //1. Extract responses.  These are not copied to this.responses until
        //they are verified.  Note, however that the metainformation need not be
        //verified for these to be acceptable.  Also note that exceptions are
        //silently caught.
        int left=getResultCount();          //number of records left to get
        Response[] responses=new Response[left];
        try {
            //For each record...
            for ( ; left > 0; left--) {
                long index=ByteOrder.ubytes2long(ByteOrder.leb2int(payload,i));
                long size=ByteOrder.ubytes2long(ByteOrder.leb2int(payload,i+4));
                i+=8;

                //The file name is supposed to be terminated by a double null
                //terminator.  But Gnotella inserts meta-information between
                //these null characters.  So we have to handle this.
                //
                //See http://gnutelladev.wego.com/go/
                //         wego.discussion.message?groupId=139406&
                //         view=message&curMsgId=319258&discId=140845&
                //         index=-1&action=view

                //Search for first single null terminator.
                int j=i;
                for ( ; ; j++) {
                    if (payload[j]==(byte)0)
                        break;
                }

                //payload[i..j-1] is name.  This excludes the null terminator.
                String name=new String(payload,i,j-i);
                responses[responses.length-left]=new Response(index,size,name);

                //Search for remaining null terminator.
                for ( j=j+1; ; j++) {
                    if (payload[j]==(byte)0)
                        break;
                }
                i=j+1;
                if (i>payload.length-16)
                    throw new BadPacketException("Missing null terminator "
                                                 +"filename");
            }

            //All set.  Accept parsed results.
            this.responses=responses;
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        } catch (BadPacketException e) {
            return;
        }                

        //2. Extract BearShare-style metainformation, if any.  Any
        //exceptions are silently caught. The format is 
        //      vendor code           (4 bytes, case insensitive)
        //      common payload length (1 byte, unsigned, always>0)
        //      common payload        (length given above.  See below.)
        //      vendor payload        (length until clientGUID)
        //The normal 16 byte clientGUID follows, of course.
        //
        //Currently the common payload consists of a single byte
        //whose low bit is one if we should try a push.
        try {
			if (i > (payload.length-16-4-2)) {   //see above
                throw new BadPacketException("No metadata");
            }
            //Attempt to verify.  Results are not copied to this until verified.
            String vendorT=null;
            boolean pushFlagSetT;
            //Apparently the US-ASCII encoding is NOT supported by all
            //platforms.  But since we do not use the vendor string yet, we
            //ignore it.
//              try {
//                  //Must use ASCII encoding since characters are more than two
//                  //bytes on other platforms!
//                  vendorT=new String(payload, i, 4, "US-ASCII");
//                  Assert.that(vendorT.length()==4,
//                              "Vendor length wrong.  Wrong character encoding?");
//              } catch (UnsupportedEncodingException e) {
//                  Assert.that(false, "No support for ASCII encoding.");
//              }
            i+=4;
            int length=ByteOrder.ubyte2int(payload[i]);
            if (length==0)
                throw new BadPacketException("Common payload length zero.");
            i++;
            pushFlagSetT = (payload[i]&0x1)==1;
            i+=length;
            if (i>payload.length-16)
                throw new BadPacketException(
                    "Common payload length too large.");

            //All set.  Accept parsed values.
            this.pushFlagSet=pushFlagSetT;
//              Assert.that(vendorT!=null);
//              this.vendor=vendorT.toUpperCase();
        } catch (BadPacketException e) {
            return;
        } catch (IndexOutOfBoundsException e) {
            return;
        } 
    }

    /** Returns the 16 byte client ID (i.e., the "footer") of the
     *  responding host.  */
    public byte[] getClientGUID() {
        byte[] result=new byte[16];
        //Copy the last 16 bytes of payload to result.  Note that there may
        //be metainformation before the client GUID.  So it is not correct
        //to simply count after the last result record.
        int length=super.getLength();
        System.arraycopy(payload, length-16, result, 0, 16);
        return result;
    }

    public String toString() {
        return "QueryReply("+getResultCount()+" hits, "+super.toString()+")";
    }

    /** Unit test */
    /*
    public static void main(String args[]) {
        byte[] ip={(byte)0xFF, (byte)0, (byte)0, (byte)0x1};
        long u4=0x00000000FFFFFFFFl;
        byte[] guid=new byte[16]; guid[0]=(byte)1; guid[15]=(byte)0xFF;
        Response[] responses=new Response[0];
        QueryReply qr=new QueryReply(guid, (byte)5,
                                     0xF3F1, ip, 1, responses,
                                     guid);
        Assert.that(qr.getSpeed()==1);
        Assert.that(qr.getPort()==0xF3F1, Integer.toHexString(qr.getPort()));
        try {
            Assert.that(!qr.getResults().hasNext());
        } catch (BadPacketException e) {
            Assert.that(false);
        }
        responses=new Response[2];
        responses[0]=new Response(11,22,"Sample.txt");
        responses[1]=new Response(0x2FF2,0xF11F,"Another file  ");
        qr=new QueryReply(guid, (byte)5,
                          0xFFFF, ip, u4, responses,
                          guid);
        Assert.that(qr.getIP().equals("255.0.0.1"));
        Assert.that(qr.getPort()==0xFFFF);
        Assert.that(qr.getSpeed()==u4);
        Assert.that(Arrays.equals(qr.getClientGUID(),guid));
        try {
            Iterator iter=qr.getResults();
            Response r1=(Response)iter.next();
            Assert.that(r1.equals(responses[0]));
            Response r2=(Response)iter.next();
            Assert.that(r2.equals(responses[1]));
            Assert.that(!iter.hasNext());
        } catch (BadPacketException e) {
            Assert.that(false);
        } catch (NoSuchElementException e) {
            Assert.that(false);
        }

        ////////////////////  Contruct from Raw Bytes /////////////

        //Normal case: double null-terminated result
        byte[] payload=new byte[11+11+16];
        payload[0]=1;            //Number of results
        payload[11+8]=(byte)65;  //The character 'A'
        qr=new QueryReply(new byte[16], (byte)5, (byte)0,
                          payload);
        try {
            Iterator iter=qr.getResults();
            Response response=(Response)iter.next();
            Assert.that(response.getName().equals("A"),
                        "'"+response.getName()+"'");
            Assert.that(! iter.hasNext());
        } catch (BadPacketException e) {
            Assert.that(false);
        }
        try {
            qr.getVendor();    //undefined => exception
            Assert.that(false);
        } catch (BadPacketException e) { }
        try {
            qr.getNeedsPush(); //undefined => exception
            Assert.that(false);
        } catch (BadPacketException e) { }


        //Bad case: not enough space for client GUID.  We can get
        //the client GUID, but not the results.
        payload=new byte[11+11+15];
        payload[0]=1;                    //Number of results
        payload[11+8]=(byte)65;          //The character 'A'
        qr=new QueryReply(new byte[16], (byte)5, (byte)0,
                          payload);
        try {
            Iterator iter=qr.getResults();
            Assert.that(false);
        } catch (BadPacketException e) { }
        try {
            Iterator iter=qr.getResults();
            Assert.that(false);
        } catch (BadPacketException e) { }
        try {
            qr.getVendor();
            Assert.that(false);
        } catch (BadPacketException e) { }

        //Normal case: basic metainfo with no vendor data
        payload=new byte[11+11+(4+2+0)+16];
        payload[0]=1;            //Number of results
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)1;
        payload[11+11+4+1]=(byte)0xB1; //set push flag (and other stuff)
        qr=new QueryReply(new byte[16], (byte)5, (byte)0,
                          payload);
        try {
            String vendor=qr.getVendor();
            Assert.that(vendor.equals("LIME"), vendor);
            vendor=qr.getVendor();
            Assert.that(vendor.equals("LIME"), vendor);
            Assert.that(qr.getNeedsPush()==true);
        } catch (BadPacketException e) {
            System.out.println(e.toString());
            Assert.that(false);
        }
        
        //Normal case: basic metainfo with extra vendor data
        payload=new byte[11+11+(4+2+3)+16];
        payload[0]=1;            //Number of results
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)76;   //The character 'L'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)1;
        payload[11+11+4+1]=(byte)0xF0; //no push flag (and other crap)
        payload[11+11+4+2+0]=(byte)0xFF; //garbage data (ignored)
        qr=new QueryReply(new byte[16], (byte)5, (byte)0,
                          payload);
        try {
            String vendor=qr.getVendor();
            Assert.that(vendor.equals("LLME"), vendor);
            vendor=qr.getVendor();
            Assert.that(vendor.equals("LLME"), vendor);
            Assert.that(qr.getNeedsPush()==false);
        } catch (BadPacketException e) {
            Assert.that(false);
            e.printStackTrace();
        }

        //Weird case.  No common data.  (Don't allow.)
        payload=new byte[11+11+(4+1+2)+16];
        payload[0]=1;            //Number of results
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+11+4+1+0]=(byte)1;
        qr=new QueryReply(new byte[16], (byte)5, (byte)0,
                          payload);
        try {
            qr.getNeedsPush();
            Assert.that(false);
        } catch (BadPacketException e) { }
        try { 
            qr.getVendor();
            Assert.that(false);
        } catch (BadPacketException e) { }

        //Bad case.  Common payload length lies.
        payload=new byte[11+11+(4+2+0)+16];
        payload[0]=1;            //Number of results
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)2;
        qr=new QueryReply(new byte[16], (byte)5, (byte)0,
                          payload);
        try {
            qr.getResults();
        } catch (BadPacketException e) {
            Assert.that(false);
        }
        try {
            qr.getVendor();
            Assert.that(false);
        } catch (BadPacketException e) { }            
    }
    */
}
