package com.limegroup.gnutella;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.limewire.collection.IntervalSet;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * A single result from a query reply message.  (In hindsight, "Result" would
 * have been a better name.)  Besides basic file information, responses can
 * include metadata.
 *
 * Response was originally intended to be immutable, but it currently includes
 * mutator methods for metadata; these will be removed in the future.  
 */
public interface Response {
  
    /**
     * Like writeToArray(), but writes to an OutputStream.
     */
    public void writeToStream(OutputStream os) throws IOException;

    /**
     * Sets this' metadata.
     * @param doc the parsed XML metadata 
     */	
    public void setDocument(LimeXMLDocument doc);
	   
    /**
     */
    public int getIncomingLength();

	/**
	 * Returns the index for the file stored in this <tt>Response</tt>
	 * instance.
	 *
	 * @return the index for the file stored in this <tt>Response</tt>
	 * instance
	 */
    public long getIndex();

	/**
	 * Returns the size of the file for this <tt>Response</tt> instance
	 * (in bytes).
	 *
	 * @return the size of the file for this <tt>Response</tt> instance
	 * (in bytes)
	 */
    public long getSize();

	/**
	 * Returns the name of the file for this response.  This is guaranteed
	 * to be non-null, but it could be the empty string.
	 *
	 * @return the name of the file for this response
	 */
    public String getName();

    /**
     * Returns this' metadata.
     */
    public LimeXMLDocument getDocument();

	/**
	 * Returns an immutable <tt>Set</tt> of <tt>URN</tt> instances for 
	 * this <tt>Response</tt>.
	 *
	 * @return an immutable <tt>Set</tt> of <tt>URN</tt> instances for 
	 * this <tt>Response</tt>, guaranteed to be non-null, although the
	 * set could be empty
	 */
    public Set<URN> getUrns();
    
    /**
     * Returns an immutable <tt>Set</tt> of <tt>Endpoint</tt> that
     * contain the same file described in this <tt>Response</tt>.
     *
     * @return an immutable <tt>Set</tt> of <tt>Endpoint</tt> that
     * contain the same file described in this <tt>Response</tt>,
     * guaranteed to be non-null, although the set could be empty
     */
    public Set<? extends IpPort> getLocations();
    
    /**
     * Returns the create time.
     */
    public long getCreateTime();
    
    public boolean isMetaFile();
    
    byte[] getExtBytes();
    
    public IntervalSet getRanges();
    
    public boolean isVerified();
    
    /**
     * Returns this Response as a RemoteFileDesc.
     */
    public RemoteFileDesc toRemoteFileDesc(HostData data, RemoteFileDescFactory remoteFileDescFactory);
}

