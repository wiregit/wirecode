package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.statistics.*;
import java.io.*;
import java.net.*;
import java.util.Locale;
import java.util.StringTokenizer;
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
 */
public class QueryReply extends Message implements Serializable{
    //Rep rationale: because most queries aren't directed to us (we'll just
    //forward them) we extract the responses lazily as needed.
    //When they are extracted, however, it makes sense to store the parsed
    //data in the responses field.
    //
    //WARNING: see note in Message about IP addresses.

    // some parameters about xml, namely the max size of a xml collection string.
    public static final int XML_MAX_SIZE = 32768;
    
    /** 2 bytes for public area, 2 bytes for xml length.
     */
    public static final int COMMON_PAYLOAD_LEN = 4;

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
    /** If parsed, one of TRUE (client supports chat), FALSE, or UNDEFINED. */
    private volatile int supportsChat=UNDEFINED;
     /** If parsed, one of TRUE (client supports browse host), 
      * FALSE, or UNDEFINED. */
    private volatile int supportsBrowseHost=FALSE;
     /** If parsed, one of TRUE (reply sent in response to a multicast query), 
      * FALSE, or UNDEFINED. */
    private volatile int replyToMulticast=FALSE;
    
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
    /** The mask for extracting the GGEP flag from the QHD common area. */
    private static final byte GGEP_MASK=(byte)0x20;

    /** The mask for extracting the chat flag from the QHD private area. */
    private static final byte CHAT_MASK=(byte)0x01;
    
    /** The xml chunk that contains metadata about xml responses*/
    private byte[] _xmlBytes = new byte[0];

	/** The raw ip address of the host returning the hit.*/
	private byte[] _address = new byte[4];

    /** the PushProxy info for this hit.
     */
    private PushProxyInterface[] _proxies;

    /** Our static and final instance of the GGEPUtil helper class.
     */
    private static final GGEPUtil _ggepUtil = new GGEPUtil();

    /** Creates a new query reply.  The number of responses is responses.length
     *  The Browse Host GGEP extension is ON by default.  
     *
     *  @requires  0 < port < 2^16 (i.e., can fit in 2 unsigned bytes),
     *    ip.length==4 and ip is in <i>BIG-endian</i> byte order,
     *    0 < speed < 2^32 (i.e., can fit in 4 unsigned bytes),
     *    responses.length < 2^8 (i.e., can fit in 1 unsigned byte),
     *    clientGUID.length==16
     */
    public QueryReply(byte[] guid, byte ttl,
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID, boolean isMulticastReply) {
        this(guid, ttl, port, ip, speed, responses, clientGUID, new byte[0],
             false, false, false, false, false, false, true, isMulticastReply,
             new PushProxyInterface[0]);
    }


    /** 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code and the given busy and push flags.  Note that this
     * constructor has no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON by default.  
     *
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots.  
     * @param finishedUpload true iff this server has successfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param supportsChat true iff the host currently allows chatting.
     */
    public QueryReply(byte[] guid, byte ttl, 
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID,
            boolean needsPush, boolean isBusy,
            boolean finishedUpload, boolean measuredSpeed,boolean supportsChat,
            boolean isMulticastReply) {
        this(guid, ttl, port, ip, speed, responses, clientGUID, new byte[0],
             true, needsPush, isBusy, finishedUpload,
             measuredSpeed,supportsChat,
             true, isMulticastReply, new PushProxyInterface[0]);
    }


    /** 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code and the given busy and push flags.  Note that this
     * constructor has no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON by default.  
     *
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots.  
     * @param finishedUpload true iff this server has successfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param xmlBytes The (non-null) byte[] containing aggregated
     * and indexed information regarding file metadata.  In terms of byte-size, 
     * this should not be bigger than 65535 bytes.  Anything larger will result
     * in an Exception being throw.  This String is assumed to consist of
     * compressed data.
     * @param supportsChat true iff the host currently allows chatting.
     * @exception IllegalArgumentException Thrown if 
     * xmlBytes.length > XML_MAX_SIZE
     */
    public QueryReply(byte[] guid, byte ttl, 
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID, byte[] xmlBytes,
            boolean needsPush, boolean isBusy,
            boolean finishedUpload, boolean measuredSpeed,boolean supportsChat,
            boolean isMulticastReply) 
        throws IllegalArgumentException {
        this(guid, ttl, port, ip, speed, responses, clientGUID, 
             xmlBytes, needsPush, isBusy,  finishedUpload, measuredSpeed, 
             supportsChat, isMulticastReply, new PushProxyInterface[0]);
    }

    /** 
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD.  The QHD with
     * the LIME vendor code and the given busy and push flags.  Note that this
     * constructor has no support for undefined push or busy bits.
     * The Browse Host GGEP extension is ON by default.  
     *
     * @param needsPush true iff this is firewalled and the downloader should
     *  attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload slots.  
     * @param finishedUpload true iff this server has successfully finished an 
     *  upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *  user
     * @param xmlBytes The (non-null) byte[] containing aggregated
     * and indexed information regarding file metadata.  In terms of byte-size, 
     * this should not be bigger than 65535 bytes.  Anything larger will result
     * in an Exception being throw.  This String is assumed to consist of
     * compressed data.
     * @param supportsChat true iff the host currently allows chatting.
     * @param proxies an array of PushProxy interfaces.  will be included in 
     * the replies GGEP extension.
     * @exception IllegalArgumentException Thrown if 
     * xmlBytes.length > XML_MAX_SIZE
     */
    public QueryReply(byte[] guid, byte ttl, 
            int port, byte[] ip, long speed, Response[] responses,
            byte[] clientGUID, byte[] xmlBytes,
            boolean needsPush, boolean isBusy,
            boolean finishedUpload, boolean measuredSpeed,boolean supportsChat,
            boolean isMulticastReply, PushProxyInterface[] proxies) 
        throws IllegalArgumentException {
        this(guid, ttl, port, ip, speed, responses, clientGUID, 
             xmlBytes, true, needsPush, isBusy, 
             finishedUpload, measuredSpeed,supportsChat, true, isMulticastReply,
             proxies);
        if (xmlBytes.length > XML_MAX_SIZE)
            throw new IllegalArgumentException("XML bytes too big: "+xmlBytes.length);
        _xmlBytes = xmlBytes;        
    }


    /** Creates a new query reply with data read from the network. */
    public QueryReply(byte[] guid, byte ttl, byte hops,byte[] payload) 
		throws BadPacketException {
        super(guid, Message.F_QUERY_REPLY, ttl, hops, payload.length);
        this.payload=payload;
		if(!NetworkUtils.isValidPort(getPort())) {
			throw new BadPacketException("invalid port");
		}
		
		setAddress();
        //repOk();                               
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
		setAddress();
    }

    /** 
     * Internal constructor.  Only creates QHD if includeQHD==true.  
     */
    private QueryReply(byte[] guid, byte ttl, 
             int port, byte[] ip, long speed, Response[] responses,
             byte[] clientGUID, byte[] xmlBytes,
             boolean includeQHD, boolean needsPush, boolean isBusy,
             boolean finishedUpload, boolean measuredSpeed,
             boolean supportsChat, boolean supportsBH,
             boolean isMulticastReply, PushProxyInterface[] proxies) {
        super(guid, Message.F_QUERY_REPLY, ttl, (byte)0,
              0,                               // length, update later
              16);                             // 16-byte footer
        // you aren't going to send this.  it will throw an exception above in
        // the appropriate constructor....
        if (xmlBytes.length > XML_MAX_SIZE)
            return;  

        final int n = responses.length;
		if((port & 0xFFFF0000) != 0) {
			throw new IllegalArgumentException("invalid port: "+port);
		} else if(ip.length != 4) {
			throw new IllegalArgumentException("invalid ip length: "+ip.length);
		} else if((speed & 0xFFFFFFFF00000000l) != 0) {
			throw new IllegalArgumentException("invalid speed: "+speed);
		} else if(n >= 256) {
			throw new IllegalArgumentException("invalid num responses: "+n);
		}

        // set up proxies
        _proxies = proxies;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            //Write beginning of payload.
            //Downcasts are ok, even if they go negative
            baos.write(n);
            ByteOrder.short2leb((short)port, baos);
            baos.write(ip, 0, ip.length);
            ByteOrder.int2leb((int)speed, baos);
            
            //Write each response
            for (int left=n; left>0; left--) {
                Response r=responses[n-left];
                r.writeToStream(baos);
            }
            
            //Write QHD if desired
            if (includeQHD) {
                //a) vendor code.  This is hardcoded here for simplicity,
                //efficiency, and to prevent character decoding problems.  If you
                //change this, be sure to change CommonUtils.QHD_VENDOR_NAME as
                //well.
                baos.write(76); //'L'
                baos.write(73); //'I'
                baos.write(77); //'M'
                baos.write(69); //'E'
                
                //b) payload length
                baos.write(COMMON_PAYLOAD_LEN);
                
                // size of standard, no options, ggep block...
                int ggepLen=
                    _ggepUtil.getQRGGEP(false, false, 
                                        new PushProxyInterface[0]).length;
                
                //c) PART 1: common area flags and controls.  See format in
                //parseResults2.
                byte flags=
                    (byte)((needsPush && !isMulticastReply ? PUSH_MASK : 0) 
                           | BUSY_MASK 
                           | UPLOADED_MASK 
                           | SPEED_MASK
                           | GGEP_MASK);
                byte controls=
                    (byte)(PUSH_MASK
                           | (isBusy && !isMulticastReply ? BUSY_MASK : 0) 
                           | (finishedUpload ? UPLOADED_MASK : 0)
                           | (measuredSpeed || isMulticastReply ? SPEED_MASK : 0)
                           | (supportsBH || isMulticastReply ? 
                              GGEP_MASK : (ggepLen > 0 ? GGEP_MASK : 0)) );

                baos.write(flags);
                baos.write(controls);
                
                //d) PART 2: size of xmlBytes + 1.
                int xmlSize = xmlBytes.length + 1;
                if (xmlSize > XML_MAX_SIZE)
                    xmlSize = XML_MAX_SIZE;  // yes, truncate!
                ByteOrder.short2leb(((short) xmlSize), baos);
                
                //e) private area: one byte with flags 
                //for chat support
                byte chatSupport=(byte)(supportsChat ? CHAT_MASK : 0);
                baos.write(chatSupport);
                
                //f) the GGEP block
                byte[] ggepBytes = _ggepUtil.getQRGGEP(supportsBH,
                                                       isMulticastReply,
                                                       _proxies);
                baos.write(ggepBytes, 0, ggepBytes.length);
                
                //g) actual xml.
                baos.write(xmlBytes, 0, xmlBytes.length);
                
                // write null after xml, as specified
                baos.write(0);
            }

            //Write footer
            baos.write(clientGUID, 0, 16);
            
            // setup payload params
            payload = baos.toByteArray();
            updateLength(payload.length);
        }
        catch (IOException reallyBad) {
            reallyBad.printStackTrace();
        }

		setAddress();
    }

	/**
	 * Sets the IP address bytes.
	 */
	private void setAddress() {
		_address[0] = payload[3];
        _address[1] = payload[4];
        _address[2] = payload[5];
        _address[3] = payload[6];		
	}

    /**
     * Sets the guid for this message. Is needed, when we want to cache 
     * query replies or sfor some other reason want to change the GUID as 
     * per the guid of query request
     * @param guid The guid to be set
     */
    public void setGUID(GUID guid) {
        super.setGUID(guid);
    }
    
    /** Returns the number of bytes necessary to represent responses
     *  in the payload .
     */
    private static int rLength(Response[] responses) {
        int ret=0;
        for (int i=0; i<responses.length; i++) {
            ret += responses[i].getLength();
        }
        return ret;
    }

	// inherit doc comment
    public void writePayload(OutputStream out) throws IOException {
        out.write(payload);
		if(RECORD_STATS) {
			SentMessageStatHandler.TCP_QUERY_REPLIES.addMessage(this);
		}
    }

    /** Return the associated xml metadata string if the queryreply
     *  contained one.
     */
    public byte[] getXMLBytes() {
        parseResults();
        return _xmlBytes;
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
        return NetworkUtils.ip2string(_address); //takes care of signs
    }

    /**
     * Accessor the IP address in byte array form.
     *
     * @return the IP address for this query hit as an array of bytes
     */
    public byte[] getIPBytes() {
        return _address;
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
            throw new BadPacketException("results are null");
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

    /** 
     * Returns true iff the client supports chat.  Throws BadPacketException if
     * the flag couldn't be extracted, either because it is missing or
     * corrupted.  Typically this exception is treated the same way as returning
     * false.  
     */
    public boolean getSupportsChat() throws BadPacketException {
        parseResults();

        switch (supportsChat) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for supportsChat: "+supportsChat);
            return false;
        }
    }

    /** 
     * Returns true iff the client supports browse host feature.
     * @return true, if the client supports browse host feature,
     * false otherwise
     * @exception Throws BadPacketException if
     * the flag couldn't be extracted, either because it is missing or
     * corrupted.  Typically this exception is treated the same way as returning
     * false. 
     */
    public boolean getSupportsBrowseHost() throws BadPacketException {
        parseResults();

        switch (supportsBrowseHost) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for supportsBrowseHost: "
                + supportsBrowseHost);
            return false;
        }
    }
    
    /** 
     * Returns true iff the reply was sent in response to a multicast query.
     * @return true, iff the reply was sent in response to a multicast query,
     * false otherwise
     * @exception Throws BadPacketException if
     * the flag couldn't be extracted, either because it is missing or
     * corrupted.  Typically this exception is treated the same way as returning
     * false. 
     */
    public boolean isReplyToMulticastQuery() throws BadPacketException {
        parseResults();

        switch (replyToMulticast) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            Assert.that(false, "Bad value for replyToMulticast: "
                + replyToMulticast);
            return false;
        }
    }

    /**
     * @return null or a non-zero lenght array of PushProxy hosts.
     */
    public PushProxyInterface[] getPushProxies() {
        parseResults();
        return _proxies;
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

    /**
     * Parses the individual results for the hit.  If any one of the 
     * results is invalid, none of them will be initialized, and the
     * accessor methods for this class will all throw 
     * <tt>BadPacketException</tt>.  This is because a single invalid
     * response invalidates other invariants, such as the field for
     * the number of results matching the size of the result array.
     */
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
            InputStream bais = 
                new ByteArrayInputStream(payload,i,payload.length-i);
            //For each record...
            for ( ; left > 0; left--) {
                Response r = Response.createFromStream(bais);
                responses[responses.length-left] = r;
                i+=r.getLength();
            }
            //All set.  Accept parsed results.
            this.responses=responses;
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        } catch (IOException e) {
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
        // GGEP Stuff
        // Byte 5 and 6, if the 5th bit is set, signal that there is a GGEP
        // block.  The GGEP block will be after the common payload and will be
        // headed by the GGEP magic prefix (see the GGEP class for more details.
        //
        // If there is a GGEP block, then we look to see what is supported.
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
         * Byte 9   : private vendor flag
         * Byte 10-X: GGEP area
         * Byte X-beginning of xml : (new) private area
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
            int supportsChatT=UNDEFINED;
            int supportsBrowseHostT=UNDEFINED;
            int replyToMulticastT=UNDEFINED;
            PushProxyInterface[] proxies=null;
            
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
            // REMEMBER THAT THE PUSH BIT IS SET OPPOSITE THAN THE OTHERS.
            // (The 'I understand' is the second bit, the Yes/No is the first)
            if (length > 1) {   //BearShare 2.2.0+
                byte control=payload[i];
                byte flags=payload[i+1];
                if ((flags & PUSH_MASK)!=0)
                    pushFlagT = (control&PUSH_MASK)==1 ? TRUE : FALSE;                
                if ((control & BUSY_MASK)!=0)
                    busyFlagT = (flags&BUSY_MASK)!=0 ? TRUE : FALSE;
                if ((control & UPLOADED_MASK)!=0)
                    uploadedFlagT = (flags&UPLOADED_MASK)!=0 ? TRUE : FALSE;
                if ((control & SPEED_MASK)!=0)
                    measuredSpeedFlagT = (flags&SPEED_MASK)!=0 ? TRUE : FALSE;
                if ((control & GGEP_MASK)!=0 && (flags & GGEP_MASK)!=0) {
                    // GGEP processing
                    // iterate past flags...
                    int magicIndex = i + 2;
                    for (; 
                         (payload[magicIndex]!=GGEP.GGEP_PREFIX_MAGIC_NUMBER) &&
                         (magicIndex < payload.length);
                         magicIndex++)
                        ; // get the beginning of the GGEP stuff...
                    GGEP[] ggepBlocks = null;
                    try {
                        // if there are GGEPs, see if Browse Host supported...
                        // TODO: stop using GGEP.read(2) - move to GGEP.read(3)
                        // or fix up GGEP.read(2)
                        ggepBlocks = GGEP.read(payload, magicIndex);
                        if (_ggepUtil.allowsBrowseHost(ggepBlocks))
                            supportsBrowseHostT = TRUE;
                        if (_ggepUtil.replyToMulticastQuery(ggepBlocks))
                            replyToMulticastT = TRUE;
                        else
                            replyToMulticastT = FALSE;
                        proxies = _ggepUtil.getPushProxies(ggepBlocks);
                    }
                    catch (BadGGEPBlockException ignored) {
                    }
                }
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
                if (xmlSize > 1) {
                    int xmlInPayloadIndex = payload.length-16-xmlSize;
                    _xmlBytes = new byte[xmlSize-1];
                    System.arraycopy(payload, xmlInPayloadIndex,
                                     _xmlBytes, 0,
                                     (xmlSize-1));
                }
                else
                    _xmlBytes = new byte[0];
            }

            //Parse LimeWire's private area.  Currently only a single byte
            //whose LSB is 0x1 if we support chat, or 0x0 if we do.
            //Shareaza also supports our chat, don't disclude them...
            int privateLength=payload.length-i;
            if (privateLength>0 && (vendorT.equals("LIME") ||
                                    vendorT.equals("RAZA"))) {
                byte privateFlags = payload[i];
                supportsChatT = (privateFlags&CHAT_MASK)!=0 ? TRUE : FALSE;
            }

            if (i>payload.length-16)
                throw new BadPacketException(
                    "Common payload length too large.");
            
            //All set.  Accept parsed values.
            Assert.that(vendorT!=null);
            this.vendor=vendorT.toUpperCase(Locale.US);
            this.pushFlag=pushFlagT;
            this.busyFlag=busyFlagT;
            this.uploadedFlag=uploadedFlagT;
            this.measuredSpeedFlag=measuredSpeedFlagT;
            this.supportsChat=supportsChatT;
            this.supportsBrowseHost=supportsBrowseHostT;
            this.replyToMulticast=replyToMulticastT;
            this._proxies=proxies;

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

    /** Returns this, because it's always safe to send big replies. */
    public Message stripExtendedPayload() {
        return this;
    }

    public String toString() {
        return ("QueryReply::\r\n"+
				getResultCount()+" hits\r\n"+
				super.toString()+"\r\n"+
				"ip: "+getIP()+"\r\n");				
    }

    /** Return all the responses in this as an array of RemoteFileDescriptor.
     *   @param acceptedIncoming true if this has ever accepted an incoming
     *    connection.  This is used to calculate the quality of service
     *    (e.g., four stars) for RemoteFileDesc.
     *   @exception java.lang.Exception Thrown if attempt fails.
     */
    public RemoteFileDesc[] toRemoteFileDescArray(boolean acceptedIncoming) 
            throws BadPacketException {
        List responses = null;
        try { // get the responses, some data from them is needed...
            responses = getResultsAsList();
        }
        catch (BadPacketException bpe) {
            debug(bpe);
            throw bpe;
        }
    
        RemoteFileDesc[] retArray = new RemoteFileDesc[responses.size()];
        
        Iterator respIter = responses.iterator();
        int index = 0;
        // these will be used over and over....
        final String ip = getIP();
        final int port = getPort();
        final int qual = 
            calculateQualityOfService(!RouterService.acceptedIncomingConnection());
        final long speed = getSpeed();
        final byte[] clientGUID = getClientGUID();
        boolean supportsChat = false;
        boolean supportsBrowseHost = false;
        boolean isReplyToMulticast = false;
        try {
            isReplyToMulticast = isReplyToMulticastQuery();
            supportsChat = getSupportsChat();
            supportsBrowseHost = getSupportsBrowseHost();
        }
        catch (BadPacketException ignored) {} // don't let chat kill me....
        
        // construct RFDs....
        while (respIter.hasNext()) {
            Response currResp = (Response) respIter.next();
            retArray[index++] = new RemoteFileDesc(ip, port, 
                                                   currResp.getIndex(),
                                                   currResp.getName(),
                                                   (int) currResp.getSize(),
                                                   clientGUID, (int) speed,
                                                   supportsChat, 
                                                   qual,
												   supportsBrowseHost,
												   currResp.getDocument(),
												   currResp.getUrns(),
												   isReplyToMulticast,
                                                   _proxies);
        }
        
        return retArray;
    }


	/**
     * This method calculates the quality of service for a given host.  The
     * calculation is some function of whether or not the host is busy, whether
     * or not the host has ever received an incoming connection, etc.
     * 
     * Moved this code from SearchView to here permanently, so we avoid
     * duplication.  It makes sense from a data point of view, but this method
     * isn't really essential an essential method.
     *
     * @return a int from -1 to 3, with -1 for "never work" and 3 for "always
     * work".  Typically a return value of N means N+1 stars will be displayed
     * in the GUI.
     * @param iFirewalled switch to indicate if the client is firewalled or
     * not.  See RouterService.acceptingIncomingConnection or Acceptor for
     * details.  
     */
	public int calculateQualityOfService(boolean iFirewalled) {
        final int YES=1;
        final int MAYBE=0;
        final int NO=-1;
        
        /* Is the remote host busy? */
		int busy;
		try {
			busy=this.getIsBusy() ? YES : NO;
		} catch (BadPacketException e) {
			busy = MAYBE;
		}
		
		boolean isMCastReply;
		try {
		    isMCastReply = this.isReplyToMulticastQuery();
		} catch(BadPacketException e) {
		    isMCastReply = false;
		}		       

        /* Is the remote host firewalled? */
		int heFirewalled;
		
		if( isMCastReply ) {
		    iFirewalled = false;
		    heFirewalled = NO;
		} else if(NetworkUtils.isPrivateAddress(this.getIPBytes())) {
			heFirewalled = YES;
		} else {
			try {
				heFirewalled=this.getNeedsPush()? YES : NO;
			} catch (BadPacketException e) {
				heFirewalled = MAYBE;
			}
		}

        /* In the old days, busy hosts were considered bad.  Now they're ok (but
         * not great) because of alternate locations.  WARNING: before changing
         * this method, take a look at isFirewalledQuality! */
		if(Arrays.equals(_address, RouterService.getAddress())) {
			return 3;       // same address -- display it
        } else if (isMCastReply) {
            return 4;       // multicast, maybe busy (but doesn't matter)
        } else if (iFirewalled && heFirewalled==YES) {
            return -1;      //     both firewalled; transfer impossible
        } else if (busy==MAYBE || heFirewalled==MAYBE) {
            return 0;       //*    older client; can't tell
        } else if (busy==YES) {
            Assert.that(heFirewalled==NO || !iFirewalled);
            if (heFirewalled==YES)
                return 0;   //*    busy, push
            else
                return 1;   //**   busy, direct connect
        } else if (busy==NO) {
            Assert.that(heFirewalled==NO || !iFirewalled);
            if (heFirewalled==YES)
                return 2;   //***  not busy, push
            else
                return 3;   //**** not busy, direct connect
        } else {
            Assert.that(false, "Unexpected case!");
            return -1;
        }
	}
	
	/**
	 * Utility method for determining whether or not the given "quality"
	 * score for a <tt>QueryReply</tt> denotes that the host is firewalled
	 * or not.
	 *
	 * @param quality the quality, or score, in question
	 * @return <tt>true</tt> if the quality denotes that the host is 
	 * firewalled, otherwise <tt>false</tt> */
	public static boolean isFirewalledQuality(int quality) {
        return quality==0 || quality==2;
	}

	// inherit doc comment
	public void recordDrop() {
		if(RECORD_STATS) {
			DroppedSentMessageStatHandler.TCP_QUERY_REPLIES.addMessage(this);
		}
	}

    public final static boolean debugOn = false;
    public static void debug(String out) {
        if (debugOn) 
            System.out.println(out);
    }
    public static void debug(Exception e) {
        if (debugOn) 
            e.printStackTrace();
    }

    /** Handles all our GGEP stuff.  Caches potential GGEP blocks for efficiency.
     */
    static class GGEPUtil {

        /** The standard GGEP block for a LimeWire QueryReply.  
         *  Currently has no keys.
         */
        private final byte[] _standardGGEP;
        
        /** A GGEP block that has the 'Browse Host' extension.  Useful for Query
         *  Replies.
         */
        private final byte[] _bhGGEP;
        
        /** A GGEP block that has the 'Multicast Source' extension.  
         *  Useful for Query Replies for a Query from a multicast source.
         */
        private final byte[] _mcGGEP;
        
        /** A GGEP block that has everything a QR could possible need.
         */
        private final byte[] _comboGGEP;
        
        public GGEPUtil() {
            ByteArrayOutputStream oStream = new ByteArrayOutputStream();
            
            // the standard GGEP has nothing.
            try {
                GGEP standard = new GGEP(false);
                standard.write(oStream);
            }
            catch (IOException writeError) {
            }
            _standardGGEP = oStream.toByteArray();
            
            // a GGEP block with JUST BHOST
            oStream.reset();
            try {
                GGEP bhost = new GGEP(false);
                bhost.put(GGEP.GGEP_HEADER_BROWSE_HOST);
                bhost.write(oStream);
            }
            catch (IOException writeError) {
            }
            _bhGGEP = oStream.toByteArray();
            Assert.that(_bhGGEP != null);

            // a GGEP block with JUST MCAST
            oStream.reset();
            try {
                GGEP mcast = new GGEP(false);
                mcast.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
                mcast.write(oStream);
            }
            catch (IOException writeError) {
            }
            _mcGGEP = oStream.toByteArray();
            Assert.that(_mcGGEP != null);

            // a GGEP block with everything....
            oStream.reset();
            try {
                GGEP combo = new GGEP(false);
                combo.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
                combo.put(GGEP.GGEP_HEADER_BROWSE_HOST);
                combo.write(oStream);
            }
            catch (IOException writeError) {
            }
            _comboGGEP = oStream.toByteArray();
            Assert.that(_comboGGEP != null);
        }
        
        /** @return The appropriate byte[] corresponding to the GGEP block you
         * desire. 
         */
        public byte[] getQRGGEP(boolean supportsBH,
                                boolean isMulticastResponse,
                                PushProxyInterface[] proxies) {
            byte[] retGGEPBlock = _standardGGEP;
            if ((proxies != null) && (proxies.length > 0)) {
                final int MAX_PROXIES = 3;
                GGEP retGGEP = new GGEP();

                // write easy extensions if applicable
                if (supportsBH)
                    retGGEP.put(GGEP.GGEP_HEADER_BROWSE_HOST);
                if (isMulticastResponse)
                    retGGEP.put(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);

                // if a PushProxyInterface is valid, write up to MAX_PROXIES
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int numWritten = 0, index = 0;
                while ((index < proxies.length) && (numWritten < MAX_PROXIES)) {
                    String host = 
                        proxies[index].getPushProxyAddress().getHostAddress();
                    int port = proxies[index].getPushProxyPort();
                    try {
                        IPPortCombo combo = new IPPortCombo(host, port);
                        baos.write(combo.toBytes());
                        numWritten++;
                    }
                    catch (UnknownHostException bad) {
                    }
                    catch (IOException terrible) {
                        terrible.printStackTrace();
                    }
                    index++;
                }

                try {
                    // add the PushProxies
                    if (numWritten > 0)
                        retGGEP.put(GGEP.GGEP_HEADER_PUSH_PROXY,
                                    baos.toByteArray());
                    // set up return value
                    baos.reset();
                    retGGEP.write(baos);
                    retGGEPBlock = baos.toByteArray();
                }
                catch (IOException terrible) {
                    terrible.printStackTrace();
                }

            }
            else if (supportsBH && isMulticastResponse)
                retGGEPBlock = _comboGGEP;
            else if (supportsBH)
                retGGEPBlock = _bhGGEP;
            else if (isMulticastResponse)
                retGGEPBlock = _mcGGEP;
            return retGGEPBlock;
        }


        /** @return whether or not browse host support can be inferred from this
         * block of GGEPs.
         */
        public boolean allowsBrowseHost(GGEP[] ggeps) {
            boolean retBool = false;
            for (int i = 0; 
                 (ggeps != null) && (i < ggeps.length) && !retBool; 
                 i++) {
                Set headers = ggeps[i].getHeaders();
                retBool = headers.contains(GGEP.GGEP_HEADER_BROWSE_HOST);
            }
            return retBool;
        }

        /** @return whether or not it can be inferred that this reply is in
            response to a multicast query.
         */
        public boolean replyToMulticastQuery(GGEP[] ggeps) {
            boolean retBool = false;
            for (int i = 0; 
                 (ggeps != null) && (i < ggeps.length) && !retBool; 
                 i++) {
                Set headers = ggeps[i].getHeaders();
                retBool = headers.contains(GGEP.GGEP_HEADER_MULTICAST_RESPONSE);
            }
            return retBool;
        }

        
        /** @return non-zero-length array of PushProxyInterfaces or null,
         *  as described by the GGEP blocks.
         */
        public PushProxyInterface[] getPushProxies(GGEP[] ggeps) {
            List proxies = new ArrayList();
            for (int i = 0; (ggeps != null) && (i < ggeps.length); i++) {
                Set headers = ggeps[i].getHeaders();
                // if the block has a PUSH_PROXY value, get it, parse it,
                // and move to the next
                if (headers.contains(GGEP.GGEP_HEADER_PUSH_PROXY)) {
                    byte[] proxyBytes = null;
                    try {
                        proxyBytes = 
                            ggeps[i].getBytes(GGEP.GGEP_HEADER_PUSH_PROXY);
                    }
                    catch (BadGGEPPropertyException bad) {
                        bad.printStackTrace();  // unexpected
                        continue;
                    }

                    ByteArrayInputStream bais = 
                        new ByteArrayInputStream(proxyBytes);
                    while (bais.available() > 0) {
                        byte[] combo = new byte[6];
                        if (bais.read(combo, 0, combo.length) == combo.length) {
                            try {
                                proxies.add(new PushProxyContainer(combo));
                            }
                            catch (IllegalArgumentException malformedPair) {
                            }
                        }                        
                    }
                }
            }

            if (proxies.size() > 0) {
                PushProxyInterface[] retProxies = 
                    new PushProxyInterface[proxies.size()];
                retProxies = (PushProxyInterface[]) proxies.toArray(retProxies);
                return retProxies;
            }
            return null;
        }


    }

    /** A simple utility class for doling out PushProxy information.
     */
    public static class PushProxyContainer implements PushProxyInterface {
        IPPortCombo _combo;

        public PushProxyContainer(String hostAddress, int port) 
            throws UnknownHostException {
            _combo = new IPPortCombo(hostAddress, port);
        }

        public PushProxyContainer(byte[] fromNetwork)
            throws IllegalArgumentException {
            _combo = IPPortCombo.getCombo(fromNetwork);
        }

        public int getPushProxyPort() {
            return _combo.getPort();
        }
        public InetAddress getPushProxyAddress() {
            return _combo.getAddress();
        }

        public boolean equals(Object other) {
            if (other instanceof PushProxyContainer) {
                PushProxyContainer iface = (PushProxyContainer) other;
                return _combo.equals(iface._combo);
            }
            return false;
        }
        
    }

    /** Another utility class the encapsulates some complexity.
     *  Keep in mind that I very well could have used Endpoint here, but I
     *  decided against it mainly so I could do validity checking.
     *  This may be a bad decision.  I'm sure someone will let me know during
     *  code review.
     */
    private static class IPPortCombo {
        private int _port;
        private InetAddress _addr;
        
        public static final String DELIM = ":";

        /** @param fromNetwork 6 bytes - first 4 are IP, next 2 are port
         */
        public static IPPortCombo getCombo(byte[] fromNetwork) 
            throws IllegalArgumentException {
            if (fromNetwork.length != 6)
                throw new IllegalArgumentException("Weird Input");
            
            String host = NetworkUtils.ip2string(fromNetwork, 0);
            int port = ByteOrder.ubytes2int(ByteOrder.leb2short(fromNetwork, 4));

            try {
                return new IPPortCombo(host, port);
            }
            catch (UnknownHostException uhe) {
                throw new IllegalArgumentException("Unknown Host");
            }
        }

        public IPPortCombo(String hostAddress, int port) 
            throws UnknownHostException, IllegalArgumentException  {
            if (hostAddress.equals("0.0.0.0"))
                throw new IllegalArgumentException("Host is bad: 0.0.0.0");
            _addr = InetAddress.getByName(hostAddress);
            if (!NetworkUtils.isValidPort(port))
                throw new IllegalArgumentException("Bad Port");
            _port = port;
        }

        public int getPort() {
            return _port;
        }
        public InetAddress getAddress() {
            return _addr;
        }

        /** @return the ip and port encoded in 6 bytes (4 ip, 2 port).
         *  //TODO if IPv6 kicks in, this may fail, don't worry so much now.
         */
        public byte[] toBytes() {
            byte[] retVal = new byte[6];
            
            for (int i=0; i < 4; i++)
                retVal[i] = _addr.getAddress()[i];

            ByteOrder.short2leb((short)_port, retVal, 4);

            return retVal;
        }

        public boolean equals(Object other) {
            if (other instanceof IPPortCombo) {
                IPPortCombo combo = (IPPortCombo) other;
                return _addr.equals(combo._addr) && (_port == combo._port);
            }
            return false;
        }

    }


    private static final int min(int a, int b) {
        if (a < b)
            return a;
        else return b;
    }


} //end QueryReply
