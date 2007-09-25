package com.limegroup.gnutella;


import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Set;

import org.limewire.io.IpPort;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;


/**
 * A single result from a query reply message.  (In hindsight, "Result" would
 * have been a better name.)  Besides basic file information, responses can
 * include metadata.
 *
 * Response was originally intended to be immutable, but it currently includes
 * mutator methods for metadata; these will be removed in the future.  
 */
public class Response {
    
    //private static final Log LOG = LogFactory.getLog(Response.class);
    
    /** Both index and size must fit into 4 unsigned bytes; see
     *  constructor for details. */
    private final long index;
    private final long size;

	/**
	 * The bytes for the name string, guaranteed to be non-null.
	 */
    private final byte[] nameBytes;

    /** The name of the file matching the search.  This does NOT
     *  include the double null terminator.
     */
    private final String name;

    /** The document representing the XML in this response. */
    private LimeXMLDocument document;

    /** 
	 * The <tt>Set</tt> of <tt>URN</tt> instances for this <tt>Response</tt>,
	 * as specified in HUGE v0.94.  This is guaranteed to be non-null, 
	 * although it is often empty.
     */
    private final Set<URN> urns;

	/**
	 * The bytes between the nulls for the <tt>Response</tt>, as specified
	 * in HUGE v0.94.  This is guaranteed to be non-null, although it can be
	 * an empty array.
	 */
    private final byte[] extBytes;
    
    /**
     * The cached RemoteFileDesc created from this Response.
     */
    private volatile RemoteFileDesc cachedRFD;
        
    /**
     * If this is a response for a metafile, i.e. a file
     * that itself triggers another download.
     */
    private final boolean isMetaFile;
    
    /** The alternate locations for this Response. */
    private final Set<? extends IpPort> alternateLocations;
    
    /** The creation time for this Response. */
    private final long creationTime;


    /**
	 * Overloaded constructor that allows the creation of Responses with
     * meta-data and a <tt>Set</tt> of <tt>URN</tt> instances.  This 
	 * is the primary constructor that establishes all of the class's 
	 * invariants, does any necessary parameter validation, etc.
	 *
	 * If extensions is non-null, it is used as the extBytes instead
	 * of creating them from the urns and locations.
	 *
	 * @param index the index of the file referenced in the response
	 * @param size the size of the file (in bytes)
	 * @param name the name of the file
	 * @param urns the <tt>Set</tt> of <tt>URN</tt> instances associated
	 *  with the file
	 * @param doc the <tt>LimeXMLDocument</tt> instance associated with
	 *  the file
	 * @param endpoints a collection of other locations on this network
	 *        that will have this file
	 * @param extensions The raw unparsed extension bytes.
     */
    Response(long index, long size, String name,
					 Set<? extends URN> urns, LimeXMLDocument doc, 
					 Set<? extends IpPort> alternateLocations,
					 long creationTime,
					 byte[] extensions) {
        if( (index & 0xFFFFFFFF00000000L)!=0 )
            throw new IllegalArgumentException("invalid index: " + index);
        // see note in createFromStream about Integer.MAX_VALUE
        if (size < 0 || size > MAX_FILE_SIZE)
            throw new IllegalArgumentException("invalid size: " + size);
            
        this.index=index;
        this.size=size;
        
		if (name == null)
			this.name = "";
		else 
			this.name = name;
		
		isMetaFile = this.name.toLowerCase().endsWith(".torrent");

        byte[] temp = null;
        try {
            temp = this.name.getBytes("UTF-8");
        } catch(UnsupportedEncodingException namex) {
            //b/c this should never happen, we will show and error
            //if it ever does for some reason.
            ErrorService.error(namex);
        }
        this.nameBytes = temp;

		if (urns == null)
			this.urns = Collections.emptySet();
		else
			this.urns = Collections.unmodifiableSet(urns);
		
		this.alternateLocations = alternateLocations;
		this.creationTime = creationTime;
		this.extBytes = extensions;

		this.document = doc;
    }
  
    /**
     * Like writeToArray(), but writes to an OutputStream.
     */
    public void writeToStream(OutputStream os) throws IOException {
        ByteOrder.int2leb((int)index, os);
        if (size > Integer.MAX_VALUE) 
            ByteOrder.int2leb(0xFFFFFFFF, os);
        else
            ByteOrder.int2leb((int)size, os);
        for (int i = 0; i < nameBytes.length; i++)
            os.write(nameBytes[i]);
        //Write first null terminator.
        os.write(0);
        // write HUGE v0.93 General Extension Mechanism extensions
        // (currently just URNs)
        for (int i = 0; i < extBytes.length; i++)
            os.write(extBytes[i]);
        //add the second null terminator
        os.write(0);
    }

    /**
     * Sets this' metadata.
     * @param meta the parsed XML metadata 
     */	
    public void setDocument(LimeXMLDocument doc) {
        document = doc;
	}
	
    
    /**
     */
    public int getLength() {
        // must match same number of bytes writeToArray() will write
		return 8 +                   // index and size
		nameBytes.length +
		1 +                   // null
		extBytes.length +
		1;                    // final null
    }   
   

	/**
	 * Returns the index for the file stored in this <tt>Response</tt>
	 * instance.
	 *
	 * @return the index for the file stored in this <tt>Response</tt>
	 * instance
	 */
    public long getIndex() {
        return index;
    }

	/**
	 * Returns the size of the file for this <tt>Response</tt> instance
	 * (in bytes).
	 *
	 * @return the size of the file for this <tt>Response</tt> instance
	 * (in bytes)
	 */
    public long getSize() {
        return size;
    }

	/**
	 * Returns the name of the file for this response.  This is guaranteed
	 * to be non-null, but it could be the empty string.
	 *
	 * @return the name of the file for this response
	 */
    public String getName() {
        return name;
    }

    /**
     * Returns this' metadata.
     */
    public LimeXMLDocument getDocument() {
        return document;
    }

	/**
	 * Returns an immutable <tt>Set</tt> of <tt>URN</tt> instances for 
	 * this <tt>Response</tt>.
	 *
	 * @return an immutable <tt>Set</tt> of <tt>URN</tt> instances for 
	 * this <tt>Response</tt>, guaranteed to be non-null, although the
	 * set could be empty
	 */
    public Set<URN> getUrns() {
		return urns;
    }
    
    /**
     * Returns an immutabe <tt>Set</tt> of <tt>Endpoint</tt> that
     * contain the same file described in this <tt>Response</tt>.
     *
     * @return an immutabe <tt>Set</tt> of <tt>Endpoint</tt> that
     * contain the same file described in this <tt>Response</tt>,
     * guaranteed to be non-null, although the set could be empty
     */
    public Set<? extends IpPort> getLocations() {
        return alternateLocations;
    }
    
    /**
     * Returns the create time.
     */
    public long getCreateTime() {
        return creationTime;
    }    
    
    public boolean isMetaFile() {
    	return isMetaFile;
    }
    
    byte[] getExtBytes() {
        return extBytes;
    }
    
    /**
     * Returns this Response as a RemoteFileDesc.
     */
    public RemoteFileDesc toRemoteFileDesc(HostData data){
        if(cachedRFD != null &&
           cachedRFD.getPort() == data.getPort() &&
           cachedRFD.getHost().equals(data.getIP()))
            return cachedRFD;
        else {
            RemoteFileDesc rfd = new RemoteFileDesc(
                 data.getIP(),
                 data.getPort(),
                 getIndex(),
                 getName(),
                 getSize(),
                 data.getClientGUID(),
                 data.getSpeed(),
                 data.isChatEnabled(),
                 data.getQuality(),
                 data.isBrowseHostEnabled(),
                 getDocument(),
                 getUrns(),
                 data.isReplyToMulticastQuery(),
                 data.isFirewalled(), 
                 data.getVendorCode(),
                 data.getPushProxies(),
                 getCreateTime(),
                 data.getFWTVersionSupported(),
                 data.isTLSCapable()
                );
            cachedRFD = rfd;
            return rfd;
        }
    }

	/**
	 * Overrides equals to check that these two responses are equal.
	 * Raw extension bytes are not checked, because they may be
	 * extensions that do not change equality, such as
	 * otherLocations.
	 */
    public boolean equals(Object o) {
		if(o == this) return true;
        if (! (o instanceof Response))
            return false;
        Response r=(Response)o;
		return getIndex() == r.getIndex() &&
               getSize() == r.getSize() &&
			   getName().equals(r.getName()) &&
               ((getDocument() == null) ? (r.getDocument() == null) :
               getDocument().equals(r.getDocument())) &&
               getUrns().equals(r.getUrns());
    }


    public int hashCode() {
        return  (int)((31 * 31 * getName().hashCode() + 31 * getSize()+getIndex()));
    }

	/**
	 * Overrides Object.toString to print out a more informative message.
	 */
	public String toString() {
		return ("index:        "+index+"\r\n"+
				"size:         "+size+"\r\n"+
				"name:         "+name+"\r\n"+
				"xml document: "+document+"\r\n"+
				"urns:         "+urns);
	}
}

