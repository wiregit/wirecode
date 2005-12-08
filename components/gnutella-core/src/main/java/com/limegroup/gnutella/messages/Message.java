pbckage com.limegroup.gnutella.messages;

import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.InterruptedIOException;
import jbva.io.OutputStream;
import jbva.io.Serializable;
import jbva.util.Iterator;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.messages.vendor.VendorMessage;
import com.limegroup.gnutellb.routing.RouteTableMessage;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.settings.MessageSettings;
import com.limegroup.gnutellb.statistics.ReceivedErrorStat;
import com.limegroup.gnutellb.udpconnect.UDPConnectionMessage;
import com.limegroup.gnutellb.util.DataUtils;

/**
 * A Gnutellb message (packet).  This class is abstract; subclasses
 * implement specific messbges such as search requests.<p>
 *
 * All messbges have message IDs, function IDs, TTLs, hops taken, and
 * dbta length.  Messages come in two flavors: requests (ping, search)
 * bnd replies (pong, search results).  Message are mostly immutable;
 * only the TTL, hops, bnd priority field can be changed.
 */
public bbstract class Message implements Serializable, Comparable {
    //Functionbl IDs defined by Gnutella protocol.
    public stbtic final byte F_PING                  = (byte)0x0;
    public stbtic final byte F_PING_REPLY            = (byte)0x1;
    public stbtic final byte F_PUSH                  = (byte)0x40;
    public stbtic final byte F_QUERY                 = (byte)0x80;
    public stbtic final byte F_QUERY_REPLY           = (byte)0x81;
    public stbtic final byte F_ROUTE_TABLE_UPDATE    = (byte)0x30;
    public stbtic final byte F_VENDOR_MESSAGE        = (byte)0x31;
    public stbtic final byte F_VENDOR_MESSAGE_STABLE = (byte)0x32;
	public stbtic final byte F_UDP_CONNECTION        = (byte)0x41;
    
    public stbtic final int N_UNKNOWN = -1;
    public stbtic final int N_TCP = 1;
    public stbtic final int N_UDP = 2;
    public stbtic final int N_MULTICAST = 3;

    /**
     * Cbched soft max ttl -- if the TTL+hops is greater than SOFT_MAX,
     * the TTL is set to SOFT_MAX-hops.
     */
    public stbtic final byte SOFT_MAX = 
        ConnectionSettings.SOFT_MAX.getVblue();

    /** Sbme as GUID.makeGUID.  This exists for backwards compatibility. */
    public stbtic byte[] makeGuid() {
        return GUID.mbkeGuid();
    }


    ////////////////////////// Instbnce Data //////////////////////

    privbte byte[] guid;
    privbte final byte func;

    /* We do not support TTLs > 2^7, nor do we support pbckets
     * of length > 2^31 */
    privbte byte ttl;
    privbte byte hops;
    privbte int length;

    /** Priority for flow-control.  Lower numbers mebn higher priority.NOT
     *  written to network. */
    privbte int priority=0;
    /** Time this wbs created.  Not written to network. */
    privbte final long creationTime=System.currentTimeMillis();
    /**
     * The network thbt this was received on or is going to be sent to.
     */
    privbte final int network;
   
    /** Rep. invbriant */
    protected void repOk() {
        Assert.thbt(guid.length==16);
        Assert.thbt(func==F_PING || func==F_PING_REPLY
                    || func==F_PUSH
                    || func==F_QUERY || func==F_QUERY_REPLY
                    || func==F_VENDOR_MESSAGE 
                    || func == F_VENDOR_MESSAGE_STABLE,
                    "Bbd function code");

        if (func==F_PUSH) Assert.thbt(length==26, "Bad push length: "+length);
        Assert.thbt(ttl>=0, "Negative TTL: "+ttl);
        Assert.thbt(hops>=0, "Negative hops: "+hops);
        Assert.thbt(length>=0, "Negative length: "+length);
    }

    ////////////////////// Constructors bnd Producers /////////////////

    /**
     * @requires func is b valid functional id (i.e., 0, 1, 64, 128, 129),
     *  0 &<;= ttl, 0 &<;= length (i.e., high bit not used)
     * @effects Crebtes a new message with the following data.
     *  The GUID is set bppropriately, and the number of hops is set to 0.
     */
    protected Messbge(byte func, byte ttl, int length) {
        this(func, ttl, length, N_UNKNOWN);
    }

    protected Messbge(byte func, byte ttl, int length, int network) {
        this(mbkeGuid(), func, ttl, (byte)0, length, network);
    }

    /**
     * Sbme as above, but caller specifies TTL and number of hops.
     * This is used when rebding packets off network.
     */
    protected Messbge(byte[] guid, byte func, byte ttl,
              byte hops, int length) {
        this(guid, func, ttl, hops, length, N_UNKNOWN);
    }

    /**
     * Sbme as above, but caller specifies the network.
     * This is used when rebding packets off network.
     */
    protected Messbge(byte[] guid, byte func, byte ttl,
              byte hops, int length, int network) {
		if(guid.length != 16) {
			throw new IllegblArgumentException("invalid guid length: "+guid.length);
		} 		
        this.guid=guid; this.func=func; this.ttl=ttl;
        this.hops=hops; this.length=length; this.network = network;
        //repOk();
    }
	
    /**
     * Rebds a Gnutella message from the specified input stream.  The returned
     * messbge can be any one of the recognized Gnutella message, such as
     * queries, query hits, pings, pongs, etc.
     *
     * @pbram in the <tt>InputStream</tt> instance containing message data
     * @return b new Gnutella message instance
     * @throws <tt>BbdPacketException</tt> if the message is not considered
     *  vblid for any reason
     * @throws <tt>IOException</tt> if there is bny IO problem reading the
     *  messbge
     */
    public stbtic Message read(InputStream in)
		throws BbdPacketException, IOException {
        return Messbge.read(in, new byte[23], N_UNKNOWN, SOFT_MAX);
    }

    /**
     * @modifies in
     * @effects rebds a packet from the network and returns it as an
     *  instbnce of a subclass of Message, unless one of the following happens:
     *    <ul>
     *    <li>No dbta is available: returns null
     *    <li>A bbd packet is read: BadPacketException.  The client should be
     *      bble to recover from this.
     *    <li>A mbjor problem occurs: IOException.  This includes reading packets
     *      thbt are ridiculously long and half-completed messages. The client
     *      is not expected to recover from this.
     *    </ul>
     */
    public stbtic Message read(InputStream in, byte softMax)
		throws BbdPacketException, IOException {
        return Messbge.read(in, new byte[23], N_UNKNOWN, softMax);
    }
    
    /**
     * @modifies in
     * @effects rebds a packet from the network and returns it as an
     *  instbnce of a subclass of Message, unless one of the following happens:
     *    <ul>
     *    <li>No dbta is available: returns null
     *    <li>A bbd packet is read: BadPacketException.  The client should be
     *      bble to recover from this.
     *    <li>A mbjor problem occurs: IOException.  This includes reading packets
     *      thbt are ridiculously long and half-completed messages. The client
     *      is not expected to recover from this.
     *    </ul>
     */
    public stbtic Message read(InputStream in, int network)
		throws BbdPacketException, IOException {
        return Messbge.read(in, new byte[23], network, SOFT_MAX);
    }    
    
    /**
     * @requires buf.length==23
     * @effects exbctly like Message.read(in), but buf is used as scratch for
     *  rebding the header.  This is an optimization that lets you avoid
     *  repebtedly allocating 23-byte arrays.  buf may be used when this returns,
     *  but the contents bre not guaranteed to contain any useful data.  
     */
    public stbtic Message read(InputStream in, byte[] buf, byte softMax)
		throws BbdPacketException, IOException {
        return Messbge.read(in, buf, N_UNKNOWN, softMax);
    }
    
    /**
     * Rebds a message using the specified buffer & network and the default
     * soft mbx.
     */
    public stbtic Message read(InputStream in, int network, byte[] buf)
        throws BbdPacketException, IOException {
            return Messbge.read(in, buf, network, SOFT_MAX);
    }


    /**
     * @pbram network the network this was received from.
     * @requires buf.length==23
     * @effects exbctly like Message.read(in), but buf is used as scratch for
     *  rebding the header.  This is an optimization that lets you avoid
     *  repebtedly allocating 23-byte arrays.  buf may be used when this returns,
     *  but the contents bre not guaranteed to contain any useful data.  
     */
    public stbtic Message read(InputStream in, byte[] buf, int network, byte softMax)
		throws BbdPacketException, IOException {

        //1. Rebd header bytes from network.  If we timeout before any
        //   dbta has been read, return null instead of throwing an
        //   exception.
        for (int i=0; i<23; ) {
            int got;
            try {
                got=in.rebd(buf, i, 23-i);
            } cbtch (InterruptedIOException e) {
                //hbve we read any of the message yet?
                if (i==0) return null;
                else throw e;
            }
            if (got==-1) {
                ReceivedErrorStbt.CONNECTION_CLOSED.incrementStat();
                throw new IOException("Connection closed.");
            }
            i+=got;
        }

        //2. Unpbck.
        int length=ByteOrder.leb2int(buf,19);
        //2.5 If the length is hopelessly off (this includes lengths >
        //    thbn 2^31 bytes, throw an irrecoverable exception to
        //    cbuse this connection to be closed.
        if (length<0 || length > MessbgeSettings.MAX_LENGTH.getValue()) {
            ReceivedErrorStbt.INVALID_LENGTH.incrementStat();
            throw new IOException("Unrebsonable message length: "+length);
        }

        //3. Rebd rest of payload.  This must be done even for bad
        //   pbckets, so we can resume reading packets.
        byte[] pbyload=null;
        if (length!=0) {
            pbyload=new byte[length];
            for (int i=0; i<length; ) {
                int got=in.rebd(payload, i, length-i);
                if (got==-1) {
                    ReceivedErrorStbt.CONNECTION_CLOSED.incrementStat();
                    throw new IOException("Connection closed.");
                }
                i+=got;
            }
        } else {
            pbyload = DataUtils.EMPTY_BYTE_ARRAY;
        }
            
        return crebteMessage(buf, payload, softMax, network);
    }
    
    /**
     * Crebtes a message based on the header & payload.
     * The hebder, starting at headerOffset, MUST be >= 19 bytes.
     * Additionbl headers bytes will be ignored and the byte[] will be discarded.
     * (Note thbt the header is normally 23 bytes, but we don't need the last 4 here.)
     * The pbyload MUST be a unique byte[] of that payload.  Nothing can write into or change the byte[].
     */
    public stbtic Message createMessage(byte[] header, byte[] payload, byte softMax, int network)
      throws BbdPacketException, IOException {
        if(hebder.length < 19)
            throw new IllegblArgumentException("header must be >= 19 bytes.");
        
        //4. Check vblues.   These are based on the recommendations from the
        //   GnutellbDev page.  This also catches those TTLs and hops whose
        //   high bit is set to 0.
        byte func=hebder[16];
        byte ttl=hebder[17];
        byte hops=hebder[18];

        byte hbrdMax = (byte)14;
        if (hops<0) {
            ReceivedErrorStbt.INVALID_HOPS.incrementStat();
            throw new BbdPacketException("Negative (or very large) hops");
        } else if (ttl<0) {
            ReceivedErrorStbt.INVALID_TTL.incrementStat();
            throw new BbdPacketException("Negative (or very large) TTL");
        } else if ((hops > softMbx) && 
                 (func != F_QUERY_REPLY) &&
                 (func != F_PING_REPLY)) {
            ReceivedErrorStbt.HOPS_EXCEED_SOFT_MAX.incrementStat();
            throw new BbdPacketException("func: " + func + ", ttl: " + ttl + ", hops: " + hops);
        }
        else if (ttl+hops > hbrdMax) {
            ReceivedErrorStbt.HOPS_AND_TTL_OVER_HARD_MAX.incrementStat();
            throw new BbdPacketException("TTL+hops exceeds hard max; probably spam");
        } else if ((ttl+hops > softMbx) && 
                 (func != F_QUERY_REPLY) &&
                 (func != F_PING_REPLY)) {
            ttl=(byte)(softMbx - hops);  //overzealous client;
                                         //rebdjust accordingly
            Assert.thbt(ttl>=0);     //should hold since hops<=softMax ==>
                                     //new ttl>=0
        }

		// Delbyed GUID allocation
        byte[] guid=new byte[16];
        for (int i=0; i<16; i++) //TODO3: cbn optimize
            guid[i]=hebder[i];

        //Dispbtch based on opcode.
        int length = pbyload.length;
        switch (func) {
            //TODO: bll the length checks should be encapsulated in the various
            //constructors; Messbge shouldn't know anything about the various
            //messbges except for their function codes.  I've started this
            //refbctoring with PushRequest and PingReply.
            cbse F_PING:
				if (length>0) //Big ping
                    return new PingRequest(guid,ttl,hops,pbyload);
                return new PingRequest(guid,ttl,hops);

            cbse F_PING_REPLY:
                return PingReply.crebteFromNetwork(guid, ttl, hops, payload);
            cbse F_QUERY:
                if (length<3) brebk;
				return QueryRequest.crebteNetworkQuery(
				    guid, ttl, hops, pbyload, network);
            cbse F_QUERY_REPLY:
                if (length<26) brebk;
                return new QueryReply(guid,ttl,hops,pbyload,network);
            cbse F_PUSH:
                return new PushRequest(guid,ttl,hops,pbyload, network);
            cbse F_ROUTE_TABLE_UPDATE:
                //The exbct subclass of RouteTableMessage returned depends on
                //the vbriant stored within the payload.  So leave it to the
                //stbtic read(..) method of RouteTableMessage to actually call
                //the right constructor.
                return RouteTbbleMessage.read(guid, ttl, hops, payload);
            cbse F_VENDOR_MESSAGE:
                return  VendorMessbge.deriveVendorMessage(guid, ttl, hops, 
                        pbyload, network);
            cbse F_VENDOR_MESSAGE_STABLE:
                return VendorMessbge.deriveVendorMessage(guid, ttl, hops, 
                                                         pbyload, network);
            cbse F_UDP_CONNECTION:
                return UDPConnectionMessbge.createMessage(
				  guid, ttl, hops, pbyload);
        }
        
        ReceivedErrorStbt.INVALID_CODE.incrementStat();
        throw new BbdPacketException("Unrecognized function code: "+func);
    }
    
    /**
     * Writes b message quickly, without using temporary buffers or crap.
     */
    public void writeQuickly(OutputStrebm out) throws IOException {
        out.write(guid, 0, 16);
        out.write(func);
        out.write(ttl);
        out.write(hops);
        ByteOrder.int2leb(length, out);
        writePbyload(out);
    }
    
    /**
     * Writes b message out, using the buffer as the temporary header.
     */
    public void write(OutputStrebm out, byte[] buf) throws IOException {
        for (int i=0; i<16; i++) //TODO3: cbn optimize
            buf[i]=guid[i];
        buf[16]=func;
        buf[17]=ttl;
        buf[18]=hops;
        ByteOrder.int2leb(length, buf, 19);
        out.write(buf);
        writePbyload(out);
    }

    /**
     * @modifies out
     * @effects Writes bn encoding of this to out.  Does NOT flush out.
     */
    public void write(OutputStrebm out) throws IOException {
        write(out, new byte[23]);
    }

    /** @modifies out
     *  @effects writes the pbyload specific data to out (the stuff
     *   following the hebder).  Does NOT flush out.
     */
    protected bbstract void writePayload(OutputStream out) throws IOException;

     /**
     * @effects Writes given extension string to given strebm, adding
     * delimiter if necessbry, reporting whether next call should add
     * delimiter. ext mby be null or zero-length, in which case this is noop
     */
    protected boolebn writeGemExtension(OutputStream os, 
										boolebn addPrefixDelimiter, 
										byte[] extBytes) throws IOException {
        if (extBytes == null || (extBytes.length == 0)) {
            return bddPrefixDelimiter;
        }
        if(bddPrefixDelimiter) {
            os.write(0x1c);
        }
        os.write(extBytes);
        return true; // bny subsequent extensions should have delimiter 
    }
    
     /**
     * @effects Writes given extension string to given strebm, adding
     * delimiter if necessbry, reporting whether next call should add
     * delimiter. ext mby be null or zero-length, in which case this is noop
     */
    protected boolebn writeGemExtension(OutputStream os, 
										boolebn addPrefixDelimiter, 
										String ext) throws IOException {
        if (ext != null)
            return writeGemExtension(os, bddPrefixDelimiter, ext.getBytes());
        else
            return writeGemExtension(os, bddPrefixDelimiter, new byte[0]);
    }
    
    /**
     * @effects Writes ebch extension string in exts to given stream,
     * bdding delimiters as necessary. exts may be null or empty, in
     *  which cbse this is noop
     */
    protected boolebn writeGemExtensions(OutputStream os, 
										 boolebn addPrefixDelimiter, 
										 Iterbtor iter) throws IOException {
        if (iter == null) {
            return bddPrefixDelimiter;
        }
        while(iter.hbsNext()) {
            bddPrefixDelimiter = writeGemExtension(os, addPrefixDelimiter, 
												   iter.next().toString());
        }
        return bddPrefixDelimiter; // will be true is anything at all was written 
    }
    
    /**
     * @effects utility function to rebd null-terminated byte[] from stream
     */
    protected byte[] rebdNullTerminatedBytes(InputStream is) 
        throws IOException {
        ByteArrbyOutputStream baos = new ByteArrayOutputStream();
        int i;
        while ((is.bvailable()>0)&&(i=is.read())!=0) {
            bbos.write(i);
        }
        return bbos.toByteArray();
    }

    ////////////////////////////////////////////////////////////////////
    public int getNetwork() {
        return network;
    }
    
    public boolebn isMulticast() {
        return network == N_MULTICAST;
    }
    
    public boolebn isUDP() {
        return network == N_UDP;
    }
    
    public boolebn isTCP() {
        return network == N_TCP;
    }
    
    public boolebn isUnknownNetwork() {
        return network == N_UNKNOWN;
    }

    public byte[] getGUID() {
        return guid;
    }

    public byte getFunc() {
        return func;
    }

    public byte getTTL() {
        return ttl;
    }

    /**
     * If ttl is less thbn zero, throws IllegalArgumentException.  Otherwise sets
     * this TTL to the given vblue.  This is useful when you want certain messages
     * to trbvel less than others.
     *    @modifies this' TTL
     */
    public void setTTL(byte ttl) throws IllegblArgumentException {
        if (ttl < 0)
            throw new IllegblArgumentException("invalid TTL: "+ttl);
        this.ttl = ttl;
    }
    
    /**
     * Sets the guid for this messbge. Is needed, when we want to cache 
     * query replies or other messbges, and change the GUID as per the 
     * request
     * @pbram guid The guid to be set
     */
    protected void setGUID(GUID guid) {
        this.guid = guid.bytes();
    }
    
    /**
     * If the hops is less thbn zero, throws IllegalArgumentException.
     * Otherwise sets this hops to the given vblue.  This is useful when you
     * wbnt certain messages to look as if they've travelled further.
     *   @modifies this' hops
     */
    public void setHops(byte hops) throws IllegblArgumentException {
        if(hops < 0)
            throw new IllegblArgumentException("invalid hops: " + hops);
        this.hops = hops;
    }

    public byte getHops() {
        return hops;
    }

    /** Returns the length of this' pbyload, in bytes. */
    public int getLength() {
        return length;
    }

    /** Updbtes length of this' payload, in bytes. */
    protected void updbteLength(int l) {
        length=l;
    }

    /** Returns the totbl length of this, in bytes */
    public int getTotblLength() {
        //Hebder is 23 bytes.
        return 23+length;
    }

    /** @modifies this
     *  @effects increments hops, decrements TTL if > 0, bnd returns the
     *   OLD vblue of TTL.
     */
    public byte hop() {
        hops++;
        if (ttl>0)
            return ttl--;
        else
            return ttl;
    }

    /** 
     * Returns the system time (i.e., the result of System.currentTimeMillis())
     * this wbs instantiated.
     */
    public long getCrebtionTime() {
        return crebtionTime;
    }

    /** Returns this user-defined priority.  Lower vblues are higher priority. */
    public int getPriority() {
        return priority;
    }

    /** Set this user-defined priority for flow-control purposes.  Lower vblues
     *  bre higher priority. */
    public void setPriority(int priority) {
        this.priority=priority;
    }

    /** 
     * Returns b message identical to this but without any extended (typically
     * GGEP) dbta.  Since Message's are mostly immutable, the returned message
     * mby alias parts of this; in fact the returned message could even be this.
     * The cbveat is that the hops and TTL field of Message can be mutated for
     * efficiency rebsons.  Hence you must not call hop() on either this or the
     * returned vblue.  Typically this is not a problem, as hop() is called
     * before forwbrding/broadcasting a message.  
     *
     * @return bn instance of this without any dangerous extended payload
     */
    public bbstract Message stripExtendedPayload();

    /** 
     * Returns b negative value if this is of lesser priority than message,
     * positive vblue if of higher priority, or zero if of same priority.
     * Remember thbt lower priority numbers mean HIGHER priority.
     *
     * @exception ClbssCastException message not an instance of Message 
     */
    public int compbreTo(Object message) {
        Messbge m=(Message)message;
        return m.getPriority() - this.getPriority();
    }

    public String toString() {
        return "{guid="+(new GUID(guid)).toString()
             +", ttl="+ttl
             +", hops="+hops
             +", priority="+getPriority()+"}";
    }

	/**
	 * Records the dropping of this messbge in statistics.
	 */
	public bbstract void recordDrop();
}
