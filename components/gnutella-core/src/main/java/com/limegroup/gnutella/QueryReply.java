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
 * can extract the vendor code, push flag, and busy flag. These methods may
 * throw BadPacketException if the metadata cannot be extracted.  Note that
 * BadPacketException does not mean that other data (namely responses) cannot be
 * read; MissingDataException might have been a better name.  
 * 
 * This class also encapsulates xml metadata.  See the description of the QHD 
 * below for more details.
 * 
 * Modified by Sumeet Thadani (5/22/2001) to handle Rich Queries
 * Modified by Susheel Daswani (8/20/2001) to handle the new xml QHD format.
 * 
 */
public class QueryReply extends Message implements Serializable{
    //Rep rationale: because most queries aren't directed to us (we'll just
    //forward them) we extract the responses lazily as needed.
    //When they are extracted, however, it makes sense to store the parsed
    //data in the responses field.
    //
    //WARNING: see note in Message about IP addresses.

    // some parameters about xml, namely the max size of a xml collection string.
    public static final int XML_MAX_SIZE = 65535;
    private static final String XML_MAX_SIZE_STRING = "65535";
    
    /** 2 bytes for public area, 2 bytes for xml length.
     */
    static final int COMMON_PAYLOAD_LEN = 4;

    private byte[] payload;
    /** True if the responses and metadata have been extracted. */
    private volatile boolean parsed=false;        
    /** If parsed, the response records for this, or null if they could not
     *  be parsed. */
    private volatile Response[] responses=null;

    /** If parsed, the responses vendor string, if defined, or null
     *  otherwise. */
    private volatile String vendor=null;
    /** If parsed, one of TRUE (push needed), FALSE, or UNDEFINED. */
    private volatile int pushFlag=UNDEFINED;
    /** If parsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    private volatile int busyFlag=UNDEFINED;
    /** If parsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    private volatile int uploadedFlag=UNDEFINED;
    /** If parsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    private volatile int measuredSpeedFlag=UNDEFINED;
    
    private static final int TRUE=1;
    private static final int FALSE=0;
    private static final int UNDEFINED=-1;

    /** The mask for extracting the push flag from the QHD common area. */
    private static final byte PUSH_MASK=(byte)0x01;
    /** The mask for extracting the busy flag from the QHD common area. */
    private static final byte BUSY_MASK=(byte)0x04;
    /** The mask for extracting the busy flag from the QHD common area. */
    private static final byte UPLOADED_MASK=(byte)0x08;
    /** The mask for extracting the busy flag from the QHD common area. */
    private static final byte SPEED_MASK=(byte)0x10;

    /** The xml chunk that contains metadata about xml responses*/
    private String _xmlCollectionString = "";


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
        this(guid, ttl, port, ip, speed, responses, clientGUID, "",
             false, false, false, false, false);
    }


    /** 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code and the given busy and push flags.  Note that this
     * constructor has no support for undefined push or busy bits.
     *
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots.  
     * @param finishedUpload true iff this server has successfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     */
    public QueryReply(byte[] guid, byte ttl, 
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID,
            boolean needsPush, boolean isBusy,
            boolean finishedUpload, boolean measuredSpeed) {
        this(guid, ttl, port, ip, speed, responses, clientGUID, "",
             true, needsPush, isBusy, finishedUpload, measuredSpeed);
    }


    /** 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code and the given busy and push flags.  Note that this
     * constructor has no support for undefined push or busy bits.
     *
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots.  
     * @param finishedUpload true iff this server has successfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param xmlCollectionString The (non-null) String containing aggregated
     * and indexed information regarding file metadata.  In terms of byte-size, 
     * this should not be bigger than 65535 bytes.  Anything larger will be 
     * truncated.
     */
    public QueryReply(byte[] guid, byte ttl, 
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID, String xmlCollectionString,
            boolean needsPush, boolean isBusy,
            boolean finishedUpload, boolean measuredSpeed) {
        this(guid, ttl, port, ip, speed, responses, clientGUID, 
             xmlCollectionString, true, needsPush, isBusy, 
             finishedUpload, measuredSpeed);
        _xmlCollectionString = xmlCollectionString;
    }



    /** 
     * Internal constructor.  Only creates QHD if includeQHD==true.  
     */
    private QueryReply(byte[] guid, byte ttl, 
             int port, byte[] ip, long speed, Response[] responses,
             byte[] clientGUID, String xmlCollectionString,
             boolean includeQHD, boolean needsPush, boolean isBusy,
             boolean finishedUpload, boolean measuredSpeed) {
        super(guid, Message.F_QUERY_REPLY, ttl, (byte)0,
              11 +                             // 11 bytes of header
              rLength(responses) +             // file records size
              qhdLength(includeQHD, xmlCollectionString) + 
                                               // conditional xml-style QHD len
              16);                             // 16-byte footer
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
            byte[] nameBytes = r.getNameBytes();
            System.arraycopy(nameBytes, 0, payload, i, nameBytes.length);
            i+=nameBytes.length;
            //Write first null terminator.
            payload[i++]=(byte)0;
            //add the second null terminator
            payload[i++]=(byte)0;
        }

        //Write QHD if desired
        if (includeQHD) {
            //a) vendor code.  This is hardcoded here for simplicity,
            //efficiency, and to prevent character decoding problems.
            payload[i++]=(byte)76; //'L'
            payload[i++]=(byte)73; //'I'
            payload[i++]=(byte)77; //'M'
            payload[i++]=(byte)69; //'E'

            //b) payload length
            payload[i++]=(byte)COMMON_PAYLOAD_LEN;

            //c) PART 1: common area flags and controls.  See format in
            //parseResults2.
            payload[i++]=(byte)((needsPush ? PUSH_MASK : 0) 
                | BUSY_MASK 
                | UPLOADED_MASK 
                | SPEED_MASK);
            payload[i++]=(byte)(PUSH_MASK
                | (isBusy ? BUSY_MASK : 0) 
                | (finishedUpload ? UPLOADED_MASK : 0)
                | (measuredSpeed ? SPEED_MASK : 0));

            //c) PART 2: size of XMLCollectionString + 1.
            int xmlSize = (xmlCollectionString.getBytes()).length + 1;
            if (xmlSize > XML_MAX_SIZE)
                xmlSize = XML_MAX_SIZE;  // yes, truncate!
            ByteOrder.short2leb(((short) xmlSize), payload, i);
            i += 2;

            //d) actual xml.
            byte[] xmlCollectionBytes = xmlCollectionString.getBytes();
            System.arraycopy(xmlCollectionBytes, 0, 
                             payload, i, xmlSize-1);
            // adjust i...
            i += xmlSize-1;
            // write null after xml, as specified
            payload[i++] = (byte)0;
        }        

        //Write footer at payload[i...i+16-1]
        for (int j=0; j<16; j++) {
            payload[i+j]=clientGUID[j];
        }
    }


    /**
    * Copy constructor.  Creates a new query reply from the passed query
    * Reply. The new one is same as the passed one, but with different specified
    * GUID.<p>
    *
    * Note: The payload is not really copied, but the reference in the newly
    * constructed query reply, points to the one in the passed reply.  But since
    * the payload cannot be mutated, it shouldn't make difference if different
    * query replies maintain reference to same payload
    *
    * @param guid The new GUID for the reply
    * @param reply The query reply from where to copy the fields into the
    *  new constructed query reply 
    */
    public QueryReply(byte[] guid, QueryReply reply){
        //call the super constructor with new GUID
        super(guid, Message.F_QUERY_REPLY, reply.getTTL(), reply.getHops(),
                                                            reply.getLength());
        //set the payload field
        this.payload = reply.payload;
    }

    /** Returns the number of bytes necessary to represent responses
     *  in the payload .
     */
    private static int rLength(Response[] responses) {
        int ret=0;
        for (int i=0; i<responses.length; i++)
            //8 bytes for index and size, plus name and two null terminators
            //response.getNameBytesSize() returns the size of the 
            //file name in bytes
            ret += 8+responses[i].getNameBytesSize()+1/*regular part*/+1;
        return ret;
    }

    /** Returns the number of bytes necessary to represent the QHD in the
     *  payload.  Needs to take account of size of XML and whether or not to
     *  even include a QHD.
     */
    private static int qhdLength(boolean includeQHD, 
                                 String xmlCollectionString) {
        int retInt = 0;
        if (includeQHD) {
            retInt += 4; // 'LIME'
            retInt += 1; // 1 byte for size of public area
            retInt += COMMON_PAYLOAD_LEN; 
            // size of xml string, max XML_MAX_SIZE
            int numBytes = (xmlCollectionString.getBytes()).length;
            if ((numBytes + 1) > XML_MAX_SIZE)
                retInt += XML_MAX_SIZE;
            else
                retInt += (numBytes + 1);
        }
        return retInt;
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


    /** Return the associated xml metadata string if the queryreply
     *  contained one.
     */
    public String getXMLCollectionString() {
        return _xmlCollectionString;
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


    /** Returns a List that will yield the results, each as an
     *  instance of the Response class.  Throws BadPacketException if
     *  this data couldn't be extracted.  */
    public List getResultsAsList() throws BadPacketException {
        parseResults();
        if (responses==null)
            throw new BadPacketException();
        List list=Arrays.asList(responses);
        return list;
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
     * Returns false if the flag is present but not set.  Throws
     * BadPacketException if the flag couldn't be extracted, either because it
     * is missing or corrupted.  
     */
    public boolean getNeedsPush() throws BadPacketException {
        parseResults();

        switch (pushFlag) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for push flag: "+pushFlag);
            return false;
        }
    }

    /** 
     * Returns true if this has no more download slots.  Returns false if the
     * busy bit is present but not set.  Throws BadPacketException if the flag
     * couldn't be extracted, either because it is missing or corrupted.  
     */
    public boolean getIsBusy() throws BadPacketException {
        parseResults();

        switch (busyFlag) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for busy flag: "+pushFlag);
            return false;
        }
    }

    /** 
     * Returns true if this has successfully uploaded a complete file (bit set).
     * Returns false if the bit is not set.  Throws BadPacketException if the
     * flag couldn't be extracted, either because it is missing or corrupted.  
     */
    public boolean getHadSuccessfulUpload() throws BadPacketException {
        parseResults();

        switch (uploadedFlag) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for uploaded flag: "+pushFlag);
            return false;
        }
    }

    /** 
     * Returns true if the speed in this QueryReply was measured (bit set).
     * Returns false if it was set by the user (bit unset).  Throws
     * BadPacketException if the flag couldn't be extracted, either because it
     * is missing or corrupted.  
     */
    public boolean getIsMeasuredSpeed() throws BadPacketException {
        parseResults();

        switch (measuredSpeedFlag) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for measured speed flag: "+pushFlag);
            return false;
        }
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

                //Search for remaining null terminator.
                int k = j+1;//next byte after the first null
                for ( ; ; k++) {
                    if (k>=payload.length-16)//k is already in the GUID
                        throw new BadPacketException("Missing null terminator "
                                                     +"filename");
                    if (payload[k]==(byte)0)
                        break;
                }
                responses[responses.length-left]=
                                          new Response(index,size,name);
                i=k+1;//The byte after the second null
            }

            //All set.  Accept parsed results.
            this.responses=responses;
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        } catch (BadPacketException e) {
            return;
        }                
        
        //2. Extract BearShare-style metainformation, if any.  Any exceptions
        //are silently caught.  The definitive reference for this format is at
        //http://www.clip2.com/GnutellaProtocol04.pdf.  Briefly, the format is 
        //      vendor code           (4 bytes, case insensitive)
        //      common payload length (4 byte, unsigned, always>0)
        //      common payload        (length given above.  See below.)
        //      vendor payload        (length until clientGUID)
        //The normal 16 byte clientGUID follows, of course.
        //
        //The first byte of the common payload has a one in its 0'th bit* if we
        //should try a push.  However, if there is a second byte, and if the
        //0'th bit of this byte is zero, the 0'th bit of the first byte should
        //actually be interpreted as MAYBE.  Unfortunately LimeWire 1.4 failed
        //to set this bit in the second byte, so it should be ignored when 
        //parsing, though set on writing.
        //
        //The remaining bits of the first byte of the common payload area tell
        //whether the corresponding bits in the optional second byte is defined.
        //The idea behind having two bits per flag is to distinguish between
        //YES, NO, and MAYBE.  These bits are as followed:
        //      bit 1*  undefined, for historical reasons
        //      bit 2   1 iff server is busy
        //      bit 3   1 iff server has successfully completed an upload
        //      bit 4   1 iff server's reported speed was actually measured, not
        //              simply set by the user.
        //
        //*Here, we use 0-(N-1) numbering.  So "0'th bit" refers to the least
        //significant bit.
        /* ----------------------------------------------------------------
         * QHD UPDATE 8/17/01
         * Here is an updated QHD spec.
         * 
         * Byte 0-3 : Vendor Code
         * Byte 4   : Public area size (COMMON_PAYLOAD_LEN)
         * Byte 5-6 : Public area (as described above)
         * Byte 7-8 : Size of XML + 1 (for a null), you need to count backward
         * from the client GUID.
         * Byte 9-beginning of xml : (new) private area
         * Byte (payload.length - 16 - xmlSize (above)) - 
                (payload.length - 16 - 1) : XML!!
         * Byte (payload.length - 16 - 1) : NULL
         * Last 16 Bytes: client GUID.
         */
        try {
			if (i >= (payload.length-16)) {   //see above
                throw new BadPacketException("No QHD");
            }
            //Attempt to verify.  Results are not copied to this until verified.
            String vendorT=null;
            int pushFlagT=UNDEFINED;
            int busyFlagT=UNDEFINED;
            int uploadedFlagT=UNDEFINED;
            int measuredSpeedFlagT=UNDEFINED;

            //a) extract vendor code
            try {
                //Must use ISO encoding since characters are more than two
                //bytes on other platforms.  TODO: test on different installs!
                vendorT=new String(payload, i, 4, "ISO-8859-1");
                Assert.that(vendorT.length()==4,
                            "Vendor length wrong.  Wrong character encoding?");
            } catch (UnsupportedEncodingException e) {
                Assert.that(false, "No support for ISO-8859-1 encoding");
            }
            i+=4;

            //b) extract payload length
            int length=ByteOrder.ubyte2int(payload[i]);
            if (length<=0)
                throw new BadPacketException("Common payload length zero.");
            i++;
            if ((i + length) > (payload.length-16)) // 16 is trailing GUID size
                throw new BadPacketException("Common payload length imprecise!");

            //c) extract push and busy bits from common payload
            //Note: technically, you should look at the second byte [sic] to
            //see if the push flag of the first byte is set.  (Again note
            //that this is the reverse of the other bits.)  However, older
            //LimeWire's don't set this.  So we always assume that the push
            //bit is defined.
            pushFlagT = (payload[i]&PUSH_MASK)==1 ? TRUE : FALSE;
            if (length > 1) {   //BearShare 2.2.0+
                byte control=payload[i];
                byte flags=payload[i+1];
                if ((control & BUSY_MASK)!=0)
                    busyFlagT = (flags&BUSY_MASK)!=0 ? TRUE : FALSE;
                if ((control & UPLOADED_MASK)!=0)
                    uploadedFlagT = (flags&UPLOADED_MASK)!=0 ? TRUE : FALSE;
                if ((control & SPEED_MASK)!=0)
                    measuredSpeedFlagT = (flags&SPEED_MASK)!=0 ? TRUE : FALSE;
                i+=2; // increment used bytes appropriately...
            }

            if (length > 2) { // expecting XML.
                //d) we need to get the xml stuff.  
                //first we should get its size, then we have to look 
                //backwards and get the actual xml...
                int a, b, temp;
                temp = ByteOrder.ubyte2int(payload[i++]);
                a = temp;
                temp = ByteOrder.ubyte2int(payload[i++]);
                b = temp << 8;
                int xmlSize = a | b;
                if (xmlSize > 1)
                    _xmlCollectionString = new String(payload, 
                                                      payload.length-16-xmlSize, 
                                                      xmlSize-1);
                else
                    _xmlCollectionString = "";
            }
            
            //All set.  Accept parsed values.
            Assert.that(vendorT!=null);
            this.vendor=vendorT.toUpperCase();
            this.pushFlag=pushFlagT;
            this.busyFlag=busyFlagT;
            this.uploadedFlag=uploadedFlagT;
            this.measuredSpeedFlag=measuredSpeedFlagT;

            debug("QR.parseResults2(): returning w/o exception.");
        } catch (BadPacketException e) {
            debug("QR.parseResults2(): bpe = " + e);
            return;
        } catch (IndexOutOfBoundsException e) {
            debug("QR.parseResults2(): index exception = " + e);
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

    
    public static boolean debugOn = false;
    public static void debug(String out) {
        if (debugOn) 
            System.out.println(out);
    }


    /** Unit test.  TODO: these badly need to be factored. */
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

        //Test case added by Sumeet Thadani to check the metadata part
        //Test case modified by Susheel Daswani to check the metadata part
        payload=new byte[11+11+(4+1+4+5)+16];
        payload[0]=1;                    //Number of results
        payload[11+8]=(byte)65;          //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)76;   //The character 'L'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)QueryReply.COMMON_PAYLOAD_LEN;
        payload[11+11+4+1+2]=(byte)5; //size of xml lsb
        payload[11+11+4+1+3]=(byte)0; // size of xml msb
        payload[11+11+4+1+4]=(byte)'S';   //The character 'L'
        payload[11+11+4+1+4+1]=(byte)'U';   //The character 'L'
        payload[11+11+4+1+4+2]=(byte)'S';   //The character 'M'
        payload[11+11+4+1+4+3]=(byte)'H';   //The character 'E'
        payload[11+11+4+1+4+4]=(byte)0;   //null terminator
        qr=new QueryReply(new byte[16], (byte)5, (byte)0,
                          payload);
        try {
            Iterator iter=qr.getResults();
            Response r = (Response)iter.next();
            Assert.that(r.getNameBytesSize()==1,"Sumeet test a");
            Assert.that(r.getMetaBytesSize()==0,"Sumeet test b");
            byte[] name = r.getNameBytes();
            Assert.that(name[0]=='A',"sumeet test c");
            Assert.that(r.getName().equals("A"),"Sumeet test1");
            Assert.that(qr.getXMLCollectionString().equals("SUSH"),
                        "Susheel test1");
        }catch(BadPacketException e){
            System.out.println("MetaResponse not created well!");
        }

        //Normal case: basic metainfo with no vendor data
        payload=new byte[11+11+(4+1+4+4)+16];
        payload[0]=1;            //Number of results
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)QueryReply.COMMON_PAYLOAD_LEN;  //The size of public area
        payload[11+11+4+1]=(byte)0xB1; //set push flag (and other stuff)
        payload[11+11+4+1+2]=(byte)4;  // set xml length
        payload[11+11+4+1+3]=(byte)0;
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
        payload=new byte[11+11+(4+1+4+20000)+16];
        payload[0]=1;            //Number of results
        payload[11+8]=(byte)65;  //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)76;   //The character 'L'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)QueryReply.COMMON_PAYLOAD_LEN;
        payload[11+11+4+1]=(byte)0xF0; //no push flag (and other crap)
        payload[11+11+4+1+2]=(byte)32; //size of xml lsb
        payload[11+11+4+1+3]=(byte)78; // size of xml msb
        for (int i = 0; i < 20000; i++)
            payload[11+11+4+1+4+i] = 'a';
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


        ///////////// BearShare 2.2.0 QHD (busy bits and friends) ///////////


        //Normal case: busy bit undefined and push bits unset.
        //(We don't bother testing undefined and set.  Who cares?)
        payload=new byte[11+11+(4+1+4+1)+16];
        payload[0]=1;                //Number of results
        payload[11+8]=(byte)65;      //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4+0]=(byte)QueryReply.COMMON_PAYLOAD_LEN;
        payload[11+11+4+1]=(byte)0x0; //no data known
        payload[11+11+4+1+1]=(byte)0x0; 
        payload[11+11+4+1+2]=(byte)1;         
        qr=new QueryReply(new byte[16], (byte)5, (byte)0,
                          payload);
        try {
            String vendor=qr.getVendor();
            Assert.that(vendor.equals("LIME"), vendor);
            vendor=qr.getVendor();
            Assert.that(vendor.equals("LIME"), vendor);
        } catch (BadPacketException e) {
            System.out.println(e.toString());
            Assert.that(false);
        }                                        
        try {
            Assert.that(!qr.getNeedsPush());
        } catch (BadPacketException e) { }
        try {
            qr.getIsBusy();
            Assert.that(false);
        } catch (BadPacketException e) { }
        try {
            qr.getHadSuccessfulUpload();
            Assert.that(false);
        } catch (BadPacketException e) { }
        try {
            qr.getIsMeasuredSpeed();
            Assert.that(false);
        } catch (BadPacketException e) { }
       

        //Normal case: busy and push bits defined and set
        payload=new byte[11+11+(4+1+4+1)+16];
        payload[0]=1;                //Number of results
        payload[11+8]=(byte)65;      //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4]=(byte)QueryReply.COMMON_PAYLOAD_LEN;    //common payload size
        payload[11+11+4+1]=(byte)0x1d;  //111X1 
        payload[11+11+4+1+1]=(byte)0x1c;  //111X0
        payload[11+11+4+1+2]=(byte)1;  // no xml, just a null, so 1
        qr=new QueryReply(new byte[16], (byte)5, (byte)0,
                          payload);
        try {
            String vendor=qr.getVendor();
            Assert.that(vendor.equals("LIME"), vendor);
            Assert.that(qr.getNeedsPush()==true);
            Assert.that(qr.getNeedsPush()==true);
            Assert.that(qr.getIsBusy()==true);
            Assert.that(qr.getIsBusy()==true);
            Assert.that(qr.getIsMeasuredSpeed()==true);
            Assert.that(qr.getIsMeasuredSpeed()==true);
            Assert.that(qr.getHadSuccessfulUpload()==true);
            Assert.that(qr.getHadSuccessfulUpload()==true);
        } catch (BadPacketException e) {
            System.out.println(e.toString());
            Assert.that(false);
        }                                        

          
        //Normal case: busy and push bits defined and unset
        payload=new byte[11+11+(4+1+4+0)+16];
        payload[0]=1;                //Number of results
        payload[11+8]=(byte)65;      //The character 'A'
        payload[11+11+0]=(byte)76;   //The character 'L'
        payload[11+11+1]=(byte)105;  //The character 'i'
        payload[11+11+2]=(byte)77;   //The character 'M'
        payload[11+11+3]=(byte)69;   //The character 'E'
        payload[11+11+4]=(byte)QueryReply.COMMON_PAYLOAD_LEN;
        payload[11+11+4+1]=(byte)0x1c;  //111X1 
        payload[11+11+4+1+1]=(byte)0x0;  //111X0
        payload[11+11+4+1+2]=(byte)1;  // no xml, just a null, so 1
        qr=new QueryReply(new byte[16], (byte)5, (byte)0,
                          payload);
        try {
            String vendor=qr.getVendor();
            Assert.that(vendor.equals("LIME"), vendor);
            Assert.that(qr.getNeedsPush()==false);
            Assert.that(qr.getIsBusy()==false);
            Assert.that(qr.getIsMeasuredSpeed()==false);
            Assert.that(qr.getHadSuccessfulUpload()==false);
        } catch (BadPacketException e) {
            System.out.println(e.toString());
            Assert.that(false);
        }  

        //Create extended QHD from scratch
        responses=new Response[2];
        responses[0]=new Response(11,22,"Sample.txt");
        responses[1]=new Response(0x2FF2,0xF11F,"Another file  ");
        qr=new QueryReply(guid, (byte)5,
                          0xFFFF, ip, u4, responses,
                          guid,
                          false, true, true, false);
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
            Assert.that(qr.getVendor().equals("LIME"));
            Assert.that(qr.getNeedsPush()==false);
            Assert.that(qr.getIsBusy()==true);
            Assert.that(qr.getHadSuccessfulUpload()==true);
            Assert.that(qr.getIsMeasuredSpeed()==false);
        } catch (BadPacketException e) {
            Assert.that(false);
        } catch (NoSuchElementException e) {
            Assert.that(false);
        }

        //Create extended QHD from scratch with different bits set
        responses=new Response[2];
        responses[0]=new Response(11,22,"Sample.txt");
        responses[1]=new Response(0x2FF2,0xF11F,"Another file  ");
        qr=new QueryReply(guid, (byte)5,
                          0xFFFF, ip, u4, responses,
                          guid,
                          true, false, false, true);
        try {
            Assert.that(qr.getVendor().equals("LIME"));
            Assert.that(qr.getNeedsPush()==true);
            Assert.that(qr.getIsBusy()==false);
            Assert.that(qr.getHadSuccessfulUpload()==false);
            Assert.that(qr.getIsMeasuredSpeed()==true);
        } catch (BadPacketException e) {
            Assert.that(false);
        } catch (NoSuchElementException e) {
            Assert.that(false);
        }
        //And check raw bytes....
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try {
            qr.write(out);
        } catch (IOException e) {
            Assert.that(false);
        }
        byte[] bytes=out.toByteArray();
        //Length includes header, query hit header and footer, responses, and QHD
        Assert.that(bytes.length==(23+11+16)+(8+10+2)+(8+14+2)+(4+1+QueryReply.COMMON_PAYLOAD_LEN+1));
        Assert.that(bytes[bytes.length-16-5]==0x1d); //11101
        Assert.that(bytes[bytes.length-16-4]==0x11); //10001


        //Create from scratch with no bits set
        responses=new Response[2];
        responses[0]=new Response(11,22,"Sample.txt");
        responses[1]=new Response(0x2FF2,0xF11F,"Another file  ");
        qr=new QueryReply(guid, (byte)5,
                          0xFFFF, ip, u4, responses,
                          guid);
        try {
            qr.getVendor();
            Assert.that(false);
        } catch (BadPacketException e) { }
        try {
            qr.getNeedsPush();
            Assert.that(false);
        } catch (BadPacketException e) { }
        try {
            qr.getIsBusy();
            Assert.that(false);
        } catch (BadPacketException e) { }
        try {
            qr.getHadSuccessfulUpload();
            Assert.that(false);
        } catch (BadPacketException e) { }
        try {
            qr.getIsMeasuredSpeed();
            Assert.that(false);
        } catch (BadPacketException e) { }
    } //end unit test
    */
} //end QueryReply
    
