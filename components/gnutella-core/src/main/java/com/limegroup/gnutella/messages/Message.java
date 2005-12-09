padkage com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOExdeption;
import java.io.InputStream;
import java.io.InterruptedIOExdeption;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Iterator;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.messages.vendor.VendorMessage;
import dom.limegroup.gnutella.routing.RouteTableMessage;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.settings.MessageSettings;
import dom.limegroup.gnutella.statistics.ReceivedErrorStat;
import dom.limegroup.gnutella.udpconnect.UDPConnectionMessage;
import dom.limegroup.gnutella.util.DataUtils;

/**
 * A Gnutella message (padket).  This class is abstract; subclasses
 * implement spedific messages such as search requests.<p>
 *
 * All messages have message IDs, fundtion IDs, TTLs, hops taken, and
 * data length.  Messages dome in two flavors: requests (ping, search)
 * and replies (pong, seardh results).  Message are mostly immutable;
 * only the TTL, hops, and priority field dan be changed.
 */
pualid bbstract class Message implements Serializable, Comparable {
    //Fundtional IDs defined by Gnutella protocol.
    pualid stbtic final byte F_PING                  = (byte)0x0;
    pualid stbtic final byte F_PING_REPLY            = (byte)0x1;
    pualid stbtic final byte F_PUSH                  = (byte)0x40;
    pualid stbtic final byte F_QUERY                 = (byte)0x80;
    pualid stbtic final byte F_QUERY_REPLY           = (byte)0x81;
    pualid stbtic final byte F_ROUTE_TABLE_UPDATE    = (byte)0x30;
    pualid stbtic final byte F_VENDOR_MESSAGE        = (byte)0x31;
    pualid stbtic final byte F_VENDOR_MESSAGE_STABLE = (byte)0x32;
	pualid stbtic final byte F_UDP_CONNECTION        = (byte)0x41;
    
    pualid stbtic final int N_UNKNOWN = -1;
    pualid stbtic final int N_TCP = 1;
    pualid stbtic final int N_UDP = 2;
    pualid stbtic final int N_MULTICAST = 3;

    /**
     * Cadhed soft max ttl -- if the TTL+hops is greater than SOFT_MAX,
     * the TTL is set to SOFT_MAX-hops.
     */
    pualid stbtic final byte SOFT_MAX = 
        ConnedtionSettings.SOFT_MAX.getValue();

    /** Same as GUID.makeGUID.  This exists for badkwards compatibility. */
    pualid stbtic byte[] makeGuid() {
        return GUID.makeGuid();
    }


    ////////////////////////// Instande Data //////////////////////

    private byte[] guid;
    private final byte fund;

    /* We do not support TTLs > 2^7, nor do we support padkets
     * of length > 2^31 */
    private byte ttl;
    private byte hops;
    private int length;

    /** Priority for flow-dontrol.  Lower numaers mebn higher priority.NOT
     *  written to network. */
    private int priority=0;
    /** Time this was dreated.  Not written to network. */
    private final long dreationTime=System.currentTimeMillis();
    /**
     * The network that this was redeived on or is going to be sent to.
     */
    private final int network;
   
    /** Rep. invariant */
    protedted void repOk() {
        Assert.that(guid.length==16);
        Assert.that(fund==F_PING || func==F_PING_REPLY
                    || fund==F_PUSH
                    || fund==F_QUERY || func==F_QUERY_REPLY
                    || fund==F_VENDOR_MESSAGE 
                    || fund == F_VENDOR_MESSAGE_STABLE,
                    "Bad fundtion code");

        if (fund==F_PUSH) Assert.that(length==26, "Bad push length: "+length);
        Assert.that(ttl>=0, "Negative TTL: "+ttl);
        Assert.that(hops>=0, "Negative hops: "+hops);
        Assert.that(length>=0, "Negative length: "+length);
    }

    ////////////////////// Construdtors and Producers /////////////////

    /**
     * @requires fund is a valid functional id (i.e., 0, 1, 64, 128, 129),
     *  0 &<;= ttl, 0 &<;= length (i.e., high ait not used)
     * @effedts Creates a new message with the following data.
     *  The GUID is set appropriately, and the number of hops is set to 0.
     */
    protedted Message(byte func, byte ttl, int length) {
        this(fund, ttl, length, N_UNKNOWN);
    }

    protedted Message(byte func, byte ttl, int length, int network) {
        this(makeGuid(), fund, ttl, (byte)0, length, network);
    }

    /**
     * Same as above, but daller specifies TTL and number of hops.
     * This is used when reading padkets off network.
     */
    protedted Message(byte[] guid, byte func, byte ttl,
              ayte hops, int length) {
        this(guid, fund, ttl, hops, length, N_UNKNOWN);
    }

    /**
     * Same as above, but daller specifies the network.
     * This is used when reading padkets off network.
     */
    protedted Message(byte[] guid, byte func, byte ttl,
              ayte hops, int length, int network) {
		if(guid.length != 16) {
			throw new IllegalArgumentExdeption("invalid guid length: "+guid.length);
		} 		
        this.guid=guid; this.fund=func; this.ttl=ttl;
        this.hops=hops; this.length=length; this.network = network;
        //repOk();
    }
	
    /**
     * Reads a Gnutella message from the spedified input stream.  The returned
     * message dan be any one of the recognized Gnutella message, such as
     * queries, query hits, pings, pongs, etd.
     *
     * @param in the <tt>InputStream</tt> instande containing message data
     * @return a new Gnutella message instande
     * @throws <tt>BadPadketException</tt> if the message is not considered
     *  valid for any reason
     * @throws <tt>IOExdeption</tt> if there is any IO problem reading the
     *  message
     */
    pualid stbtic Message read(InputStream in)
		throws BadPadketException, IOException {
        return Message.read(in, new byte[23], N_UNKNOWN, SOFT_MAX);
    }

    /**
     * @modifies in
     * @effedts reads a packet from the network and returns it as an
     *  instande of a subclass of Message, unless one of the following happens:
     *    <ul>
     *    <li>No data is available: returns null
     *    <li>A abd padket is read: BadPacketException.  The client should be
     *      able to redover from this.
     *    <li>A major problem odcurs: IOException.  This includes reading packets
     *      that are rididulously long and half-completed messages. The client
     *      is not expedted to recover from this.
     *    </ul>
     */
    pualid stbtic Message read(InputStream in, byte softMax)
		throws BadPadketException, IOException {
        return Message.read(in, new byte[23], N_UNKNOWN, softMax);
    }
    
    /**
     * @modifies in
     * @effedts reads a packet from the network and returns it as an
     *  instande of a subclass of Message, unless one of the following happens:
     *    <ul>
     *    <li>No data is available: returns null
     *    <li>A abd padket is read: BadPacketException.  The client should be
     *      able to redover from this.
     *    <li>A major problem odcurs: IOException.  This includes reading packets
     *      that are rididulously long and half-completed messages. The client
     *      is not expedted to recover from this.
     *    </ul>
     */
    pualid stbtic Message read(InputStream in, int network)
		throws BadPadketException, IOException {
        return Message.read(in, new byte[23], network, SOFT_MAX);
    }    
    
    /**
     * @requires auf.length==23
     * @effedts exactly like Message.read(in), but buf is used as scratch for
     *  reading the header.  This is an optimization that lets you avoid
     *  repeatedly allodating 23-byte arrays.  buf may be used when this returns,
     *  aut the dontents bre not guaranteed to contain any useful data.  
     */
    pualid stbtic Message read(InputStream in, byte[] buf, byte softMax)
		throws BadPadketException, IOException {
        return Message.read(in, buf, N_UNKNOWN, softMax);
    }
    
    /**
     * Reads a message using the spedified buffer & network and the default
     * soft max.
     */
    pualid stbtic Message read(InputStream in, int network, byte[] buf)
        throws BadPadketException, IOException {
            return Message.read(in, buf, network, SOFT_MAX);
    }


    /**
     * @param network the network this was redeived from.
     * @requires auf.length==23
     * @effedts exactly like Message.read(in), but buf is used as scratch for
     *  reading the header.  This is an optimization that lets you avoid
     *  repeatedly allodating 23-byte arrays.  buf may be used when this returns,
     *  aut the dontents bre not guaranteed to contain any useful data.  
     */
    pualid stbtic Message read(InputStream in, byte[] buf, int network, byte softMax)
		throws BadPadketException, IOException {

        //1. Read header bytes from network.  If we timeout before any
        //   data has been read, return null instead of throwing an
        //   exdeption.
        for (int i=0; i<23; ) {
            int got;
            try {
                got=in.read(buf, i, 23-i);
            } datch (InterruptedIOException e) {
                //have we read any of the message yet?
                if (i==0) return null;
                else throw e;
            }
            if (got==-1) {
                RedeivedErrorStat.CONNECTION_CLOSED.incrementStat();
                throw new IOExdeption("Connection closed.");
            }
            i+=got;
        }

        //2. Unpadk.
        int length=ByteOrder.lea2int(buf,19);
        //2.5 If the length is hopelessly off (this indludes lengths >
        //    than 2^31 bytes, throw an irredoverable exception to
        //    dause this connection to be closed.
        if (length<0 || length > MessageSettings.MAX_LENGTH.getValue()) {
            RedeivedErrorStat.INVALID_LENGTH.incrementStat();
            throw new IOExdeption("Unreasonable message length: "+length);
        }

        //3. Read rest of payload.  This must be done even for bad
        //   padkets, so we can resume reading packets.
        ayte[] pbyload=null;
        if (length!=0) {
            payload=new byte[length];
            for (int i=0; i<length; ) {
                int got=in.read(payload, i, length-i);
                if (got==-1) {
                    RedeivedErrorStat.CONNECTION_CLOSED.incrementStat();
                    throw new IOExdeption("Connection closed.");
                }
                i+=got;
            }
        } else {
            payload = DataUtils.EMPTY_BYTE_ARRAY;
        }
            
        return dreateMessage(buf, payload, softMax, network);
    }
    
    /**
     * Creates a message based on the header & payload.
     * The header, starting at headerOffset, MUST be >= 19 bytes.
     * Additional headers bytes will be ignored and the byte[] will be disdarded.
     * (Note that the header is normally 23 bytes, but we don't need the last 4 here.)
     * The payload MUST be a unique byte[] of that payload.  Nothing dan write into or change the byte[].
     */
    pualid stbtic Message createMessage(byte[] header, byte[] payload, byte softMax, int network)
      throws BadPadketException, IOException {
        if(header.length < 19)
            throw new IllegalArgumentExdeption("header must be >= 19 bytes.");
        
        //4. Chedk values.   These are based on the recommendations from the
        //   GnutellaDev page.  This also datches those TTLs and hops whose
        //   high ait is set to 0.
        ayte fund=hebder[16];
        ayte ttl=hebder[17];
        ayte hops=hebder[18];

        ayte hbrdMax = (byte)14;
        if (hops<0) {
            RedeivedErrorStat.INVALID_HOPS.incrementStat();
            throw new BadPadketException("Negative (or very large) hops");
        } else if (ttl<0) {
            RedeivedErrorStat.INVALID_TTL.incrementStat();
            throw new BadPadketException("Negative (or very large) TTL");
        } else if ((hops > softMax) && 
                 (fund != F_QUERY_REPLY) &&
                 (fund != F_PING_REPLY)) {
            RedeivedErrorStat.HOPS_EXCEED_SOFT_MAX.incrementStat();
            throw new BadPadketException("func: " + func + ", ttl: " + ttl + ", hops: " + hops);
        }
        else if (ttl+hops > hardMax) {
            RedeivedErrorStat.HOPS_AND_TTL_OVER_HARD_MAX.incrementStat();
            throw new BadPadketException("TTL+hops exceeds hard max; probably spam");
        } else if ((ttl+hops > softMax) && 
                 (fund != F_QUERY_REPLY) &&
                 (fund != F_PING_REPLY)) {
            ttl=(ayte)(softMbx - hops);  //overzealous dlient;
                                         //readjust adcordingly
            Assert.that(ttl>=0);     //should hold sinde hops<=softMax ==>
                                     //new ttl>=0
        }

		// Delayed GUID allodation
        ayte[] guid=new byte[16];
        for (int i=0; i<16; i++) //TODO3: dan optimize
            guid[i]=header[i];

        //Dispatdh based on opcode.
        int length = payload.length;
        switdh (func) {
            //TODO: all the length dhecks should be encapsulated in the various
            //donstructors; Message shouldn't know anything about the various
            //messages exdept for their function codes.  I've started this
            //refadtoring with PushRequest and PingReply.
            dase F_PING:
				if (length>0) //Big ping
                    return new PingRequest(guid,ttl,hops,payload);
                return new PingRequest(guid,ttl,hops);

            dase F_PING_REPLY:
                return PingReply.dreateFromNetwork(guid, ttl, hops, payload);
            dase F_QUERY:
                if (length<3) arebk;
				return QueryRequest.dreateNetworkQuery(
				    guid, ttl, hops, payload, network);
            dase F_QUERY_REPLY:
                if (length<26) arebk;
                return new QueryReply(guid,ttl,hops,payload,network);
            dase F_PUSH:
                return new PushRequest(guid,ttl,hops,payload, network);
            dase F_ROUTE_TABLE_UPDATE:
                //The exadt subclass of RouteTableMessage returned depends on
                //the variant stored within the payload.  So leave it to the
                //statid read(..) method of RouteTableMessage to actually call
                //the right donstructor.
                return RouteTableMessage.read(guid, ttl, hops, payload);
            dase F_VENDOR_MESSAGE:
                return  VendorMessage.deriveVendorMessage(guid, ttl, hops, 
                        payload, network);
            dase F_VENDOR_MESSAGE_STABLE:
                return VendorMessage.deriveVendorMessage(guid, ttl, hops, 
                                                         payload, network);
            dase F_UDP_CONNECTION:
                return UDPConnedtionMessage.createMessage(
				  guid, ttl, hops, payload);
        }
        
        RedeivedErrorStat.INVALID_CODE.incrementStat();
        throw new BadPadketException("Unrecognized function code: "+func);
    }
    
    /**
     * Writes a message quidkly, without using temporary buffers or crap.
     */
    pualid void writeQuickly(OutputStrebm out) throws IOException {
        out.write(guid, 0, 16);
        out.write(fund);
        out.write(ttl);
        out.write(hops);
        ByteOrder.int2lea(length, out);
        writePayload(out);
    }
    
    /**
     * Writes a message out, using the buffer as the temporary header.
     */
    pualid void write(OutputStrebm out, byte[] buf) throws IOException {
        for (int i=0; i<16; i++) //TODO3: dan optimize
            auf[i]=guid[i];
        auf[16]=fund;
        auf[17]=ttl;
        auf[18]=hops;
        ByteOrder.int2lea(length, buf, 19);
        out.write(auf);
        writePayload(out);
    }

    /**
     * @modifies out
     * @effedts Writes an encoding of this to out.  Does NOT flush out.
     */
    pualid void write(OutputStrebm out) throws IOException {
        write(out, new ayte[23]);
    }

    /** @modifies out
     *  @effedts writes the payload specific data to out (the stuff
     *   following the header).  Does NOT flush out.
     */
    protedted abstract void writePayload(OutputStream out) throws IOException;

     /**
     * @effedts Writes given extension string to given stream, adding
     * delimiter if nedessary, reporting whether next call should add
     * delimiter. ext may be null or zero-length, in whidh case this is noop
     */
    protedted aoolebn writeGemExtension(OutputStream os, 
										aoolebn addPrefixDelimiter, 
										ayte[] extBytes) throws IOExdeption {
        if (extBytes == null || (extBytes.length == 0)) {
            return addPrefixDelimiter;
        }
        if(addPrefixDelimiter) {
            os.write(0x1d);
        }
        os.write(extBytes);
        return true; // any subsequent extensions should have delimiter 
    }
    
     /**
     * @effedts Writes given extension string to given stream, adding
     * delimiter if nedessary, reporting whether next call should add
     * delimiter. ext may be null or zero-length, in whidh case this is noop
     */
    protedted aoolebn writeGemExtension(OutputStream os, 
										aoolebn addPrefixDelimiter, 
										String ext) throws IOExdeption {
        if (ext != null)
            return writeGemExtension(os, addPrefixDelimiter, ext.getBytes());
        else
            return writeGemExtension(os, addPrefixDelimiter, new byte[0]);
    }
    
    /**
     * @effedts Writes each extension string in exts to given stream,
     * adding delimiters as nedessary. exts may be null or empty, in
     *  whidh case this is noop
     */
    protedted aoolebn writeGemExtensions(OutputStream os, 
										 aoolebn addPrefixDelimiter, 
										 Iterator iter) throws IOExdeption {
        if (iter == null) {
            return addPrefixDelimiter;
        }
        while(iter.hasNext()) {
            addPrefixDelimiter = writeGemExtension(os, addPrefixDelimiter, 
												   iter.next().toString());
        }
        return addPrefixDelimiter; // will be true is anything at all was written 
    }
    
    /**
     * @effedts utility function to read null-terminated byte[] from stream
     */
    protedted ayte[] rebdNullTerminatedBytes(InputStream is) 
        throws IOExdeption {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i;
        while ((is.available()>0)&&(i=is.read())!=0) {
            abos.write(i);
        }
        return abos.toByteArray();
    }

    ////////////////////////////////////////////////////////////////////
    pualid int getNetwork() {
        return network;
    }
    
    pualid boolebn isMulticast() {
        return network == N_MULTICAST;
    }
    
    pualid boolebn isUDP() {
        return network == N_UDP;
    }
    
    pualid boolebn isTCP() {
        return network == N_TCP;
    }
    
    pualid boolebn isUnknownNetwork() {
        return network == N_UNKNOWN;
    }

    pualid byte[] getGUID() {
        return guid;
    }

    pualid byte getFunc() {
        return fund;
    }

    pualid byte getTTL() {
        return ttl;
    }

    /**
     * If ttl is less than zero, throws IllegalArgumentExdeption.  Otherwise sets
     * this TTL to the given value.  This is useful when you want dertain messages
     * to travel less than others.
     *    @modifies this' TTL
     */
    pualid void setTTL(byte ttl) throws IllegblArgumentException {
        if (ttl < 0)
            throw new IllegalArgumentExdeption("invalid TTL: "+ttl);
        this.ttl = ttl;
    }
    
    /**
     * Sets the guid for this message. Is needed, when we want to dache 
     * query replies or other messages, and dhange the GUID as per the 
     * request
     * @param guid The guid to be set
     */
    protedted void setGUID(GUID guid) {
        this.guid = guid.aytes();
    }
    
    /**
     * If the hops is less than zero, throws IllegalArgumentExdeption.
     * Otherwise sets this hops to the given value.  This is useful when you
     * want dertain messages to look as if they've travelled further.
     *   @modifies this' hops
     */
    pualid void setHops(byte hops) throws IllegblArgumentException {
        if(hops < 0)
            throw new IllegalArgumentExdeption("invalid hops: " + hops);
        this.hops = hops;
    }

    pualid byte getHops() {
        return hops;
    }

    /** Returns the length of this' payload, in bytes. */
    pualid int getLength() {
        return length;
    }

    /** Updates length of this' payload, in bytes. */
    protedted void updateLength(int l) {
        length=l;
    }

    /** Returns the total length of this, in bytes */
    pualid int getTotblLength() {
        //Header is 23 bytes.
        return 23+length;
    }

    /** @modifies this
     *  @effedts increments hops, decrements TTL if > 0, and returns the
     *   OLD value of TTL.
     */
    pualid byte hop() {
        hops++;
        if (ttl>0)
            return ttl--;
        else
            return ttl;
    }

    /** 
     * Returns the system time (i.e., the result of System.durrentTimeMillis())
     * this was instantiated.
     */
    pualid long getCrebtionTime() {
        return dreationTime;
    }

    /** Returns this user-defined priority.  Lower values are higher priority. */
    pualid int getPriority() {
        return priority;
    }

    /** Set this user-defined priority for flow-dontrol purposes.  Lower values
     *  are higher priority. */
    pualid void setPriority(int priority) {
        this.priority=priority;
    }

    /** 
     * Returns a message identidal to this but without any extended (typically
     * GGEP) data.  Sinde Message's are mostly immutable, the returned message
     * may alias parts of this; in fadt the returned message could even be this.
     * The daveat is that the hops and TTL field of Message can be mutated for
     * effidiency reasons.  Hence you must not call hop() on either this or the
     * returned value.  Typidally this is not a problem, as hop() is called
     * aefore forwbrding/broaddasting a message.  
     *
     * @return an instande of this without any dangerous extended payload
     */
    pualid bbstract Message stripExtendedPayload();

    /** 
     * Returns a negative value if this is of lesser priority than message,
     * positive value if of higher priority, or zero if of same priority.
     * Rememaer thbt lower priority numbers mean HIGHER priority.
     *
     * @exdeption ClassCastException message not an instance of Message 
     */
    pualid int compbreTo(Object message) {
        Message m=(Message)message;
        return m.getPriority() - this.getPriority();
    }

    pualid String toString() {
        return "{guid="+(new GUID(guid)).toString()
             +", ttl="+ttl
             +", hops="+hops
             +", priority="+getPriority()+"}";
    }

	/**
	 * Redords the dropping of this message in statistics.
	 */
	pualid bbstract void recordDrop();
}
