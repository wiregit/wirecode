package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;

/**
 * A query reply.  Contains information about the responding host in
 * addition to an array of responses.  For efficiency reasons, bad query
 * reply packets may not be discovered until the getResponses
 * methods are called.
 */
public class QueryReply extends Message implements Serializable{
    //Rep rationale: because most queries aren't directed to us (we'll just
    //forward them) we extract the responses lazily as needed.
    //When they are extracted, however, it makes sense to store the parsed
    //data in the responses field.
    //
    //WARNING: see note in Message about IP addresses.

    private byte[] payload;
    /** The response records in this, or null if they have not yet been
     *  extracted from payload. */
    private Response[] responses=null;

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
            //This getBytes method is deprecated because it does
            //not properly convert Unicode (it throws away the high
            //byte.  But this is not a problem since most Gnutella
            //clients probably do not understand Unicode!
            name.getBytes(0, name.length(), payload, i);
            i+=name.length();
            //Write double null terminator.
            payload[i]=(byte)0;
            payload[i+1]=(byte)0;
            i+=2;
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
        if (responses==null) {
            parseResults();
            Assert.that(responses!=null);
        }
        List list=Arrays.asList(responses);
        return list.iterator();
    }

    /** @modifies this.responses
     *  @effects extracts response from payload and stores in responses. */
    private void parseResults() throws BadPacketException {
        //index into payload to look for next response
        int i=11;
        //number of records left to get
        int left=getResultCount();
        responses=new Response[left];

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
                //See http://gnutelladev.wego.com/go/wego.discussion.message?groupId=139406&view=message&curMsgId=319258&discId=140845&index=-1&action=view

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
            }

            //We used to ensure that there was no information between the
            //last result record and the client GUID:
            //
            //     if (i<payload.length-16) throw new BadPacketException();
            //
            //But this space can be used for meta information, so we allow
            //these packets.  The metainformation is currently ignored.
            
            if (i>payload.length-16)
                throw new BadPacketException("Missing null terminator "
                                             +"filename");
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new BadPacketException();
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
        } catch (Exception e) {
            Assert.that(false);
            e.printStackTrace();
        }


        //Weird case: metadata between null characters.
        payload=new byte[11+11+1+16];
        payload[0]=1;            //Number of results
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+10]=(byte)66; //The metadata 'B'
        qr=new QueryReply(new byte[16], (byte)5, (byte)0,
                          payload);
        try {
            Iterator iter=qr.getResults();
            Response response=(Response)iter.next();
            Assert.that(response.getName().equals("A"),
                        "'"+response.getName()+"'");
            Assert.that(! iter.hasNext());
        } catch (Exception e) {
            Assert.that(false);
            e.printStackTrace();
        }

        //Weird case: one byte metadata between last record and client GUID.
        payload=new byte[11+11+1+16];
        payload[0]=1;                    //Number of results
        payload[11+8]=(byte)65;          //The character 'A'
        payload[11+11+1+0]=(byte)0xFF;   //The first byte of client GUID
        payload[11+11+1+15]=(byte)0x11;  //The last byte of client GUID
        qr=new QueryReply(new byte[16], (byte)5, (byte)0,
                          payload);
        try {
            Iterator iter=qr.getResults();
            Response response=(Response)iter.next();
            Assert.that(response.getName().equals("A"),
                        "'"+response.getName()+"'");
            Assert.that(! iter.hasNext());
        } catch (Exception e) {
            Assert.that(false);
            e.printStackTrace();
        }
        byte[] clientGUID=qr.getClientGUID();
        Assert.that(clientGUID[0]==(byte)0xFF);
        Assert.that(clientGUID[15]==(byte)0x11);

        //Weird case: 3 bytes metadata between last record and client GUID.
        payload=new byte[11+11+10+16];
        payload[0]=1;                    //Number of results
        payload[11+8]=(byte)65;          //The character 'A'
        payload[11+11+10+0]=(byte)0xFF;  //The first byte of client GUID
        payload[11+11+10+15]=(byte)0x11; //The last byte of client GUID
        qr=new QueryReply(new byte[16], (byte)5, (byte)0,
                          payload);
        try {
            Iterator iter=qr.getResults();
            Response response=(Response)iter.next();
            Assert.that(response.getName().equals("A"),
                        "'"+response.getName()+"'");
            Assert.that(! iter.hasNext());
        } catch (Exception e) {
            Assert.that(false);
            e.printStackTrace();
        }
        clientGUID=qr.getClientGUID();
        Assert.that(clientGUID[0]==(byte)0xFF);
        Assert.that(clientGUID[15]==(byte)0x11);

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
    }
    */
}
