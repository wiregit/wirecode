package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Iterator;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.GUID;

/**
 * A Gnutella message (packet).  This class is abstract; subclasses
 * implement specific messages such as search requests.<p>
 *
 * All messages have message IDs, function IDs, TTLs, hops taken, and
 * data length.  Messages come in two flavors: requests (ping, search)
 * and replies (pong, search results).  Message are mostly immutable;
 * only the TTL, hops, and priority field can be changed.
 */
public abstract class GnutellaMessage extends AbstractMessage implements Serializable {
    
    // Functional IDs defined by Gnutella protocol.
    public static final byte F_PING                  = (byte)0x0;
    public static final byte F_PING_REPLY            = (byte)0x1;
    public static final byte F_PUSH                  = (byte)0x40;
    public static final byte F_QUERY                 = (byte)0x80;
    public static final byte F_QUERY_REPLY           = (byte)0x81;
    public static final byte F_ROUTE_TABLE_UPDATE    = (byte)0x30;
    public static final byte F_VENDOR_MESSAGE        = (byte)0x31;
    public static final byte F_VENDOR_MESSAGE_STABLE = (byte)0x32;
    public static final byte F_UDP_CONNECTION        = (byte)0x41;

    /** Same as GUID.makeGUID.  This exists for backwards compatibility. */
    public static byte[] makeGuid() {
        return GUID.makeGuid();
    }

    ////////////////////////// Instance Data //////////////////////

    /** Rep. invariant */
    protected void repOk() {
        byte[] guid = getGUID();
        byte func = getFunc();
        byte ttl = getTTL();
        byte hops = getHops();
        int length = getLength();
        
        Assert.that(guid.length==16);
        Assert.that(func==F_PING || func==F_PING_REPLY
                    || func==F_PUSH
                    || func==F_QUERY || func==F_QUERY_REPLY
                    || func==F_VENDOR_MESSAGE 
                    || func == F_VENDOR_MESSAGE_STABLE
                    || func == F_UDP_CONNECTION,
                    "Bad function code");

        if (func==F_PUSH) Assert.that(length==26, "Bad push length: "+length);
        Assert.that(ttl>=0, "Negative TTL: "+ttl);
        Assert.that(hops>=0, "Negative hops: "+hops);
        Assert.that(length>=0, "Negative length: "+length);
    }

    ////////////////////// Constructors and Producers /////////////////

    private int length;
    
    /**
     * @requires func is a valid functional id (i.e., 0, 1, 64, 128, 129),
     *  0 &<;= ttl, 0 &<;= length (i.e., high bit not used)
     * @effects Creates a new message with the following data.
     *  The GUID is set appropriately, and the number of hops is set to 0.
     */
    protected GnutellaMessage(byte func, byte ttl, int length) {
        this(func, ttl, length, N_UNKNOWN);
    }

    protected GnutellaMessage(byte func, byte ttl, int length, int network) {
        this(makeGuid(), func, ttl, (byte)0, length, network);
    }

    /**
     * Same as above, but caller specifies TTL and number of hops.
     * This is used when reading packets off network.
     */
    protected GnutellaMessage(byte[] guid, byte func, byte ttl,
              byte hops, int length) {
        this(guid, func, ttl, hops, length, N_UNKNOWN);
    }

    /**
     * Same as above, but caller specifies the network.
     * This is used when reading packets off network.
     */
    protected GnutellaMessage(byte[] guid, byte func, byte ttl,
            byte hops, int length, int network) {
        super(guid, func, ttl, hops, network);
        this.length=length;
        //repOk();
    }
	
     /**
     * @effects Writes given extension string to given stream, adding
     * delimiter if necessary, reporting whether next call should add
     * delimiter. ext may be null or zero-length, in which case this is noop
     */
    protected boolean writeGemExtension(OutputStream os, 
										boolean addPrefixDelimiter, 
										byte[] extBytes) throws IOException {
        if (extBytes == null || (extBytes.length == 0)) {
            return addPrefixDelimiter;
        }
        if(addPrefixDelimiter) {
            os.write(0x1c);
        }
        os.write(extBytes);
        return true; // any subsequent extensions should have delimiter 
    }
    
     /**
     * @effects Writes given extension string to given stream, adding
     * delimiter if necessary, reporting whether next call should add
     * delimiter. ext may be null or zero-length, in which case this is noop
     */
    protected boolean writeGemExtension(OutputStream os, 
										boolean addPrefixDelimiter, 
										String ext) throws IOException {
        if (ext != null)
            return writeGemExtension(os, addPrefixDelimiter, ext.getBytes());
        else
            return writeGemExtension(os, addPrefixDelimiter, new byte[0]);
    }
    
    /**
     * @effects Writes each extension string in exts to given stream,
     * adding delimiters as necessary. exts may be null or empty, in
     *  which case this is noop
     */
    protected boolean writeGemExtensions(OutputStream os, 
										 boolean addPrefixDelimiter, 
										 Iterator iter) throws IOException {
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
     * @effects utility function to read null-terminated byte[] from stream
     */
    protected byte[] readNullTerminatedBytes(InputStream is) 
        throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i;
        while ((is.available()>0)&&(i=is.read())!=0) {
            baos.write(i);
        }
        return baos.toByteArray();
    }

    ////////////////////////////////////////////////////////////////////
    
    /** Returns the length of this' payload, in bytes. */
    public int getLength() {
        return length;
    }

    /** Updates length of this' payload, in bytes. */
    protected void updateLength(int length) {
        this.length=length;
    }
}
