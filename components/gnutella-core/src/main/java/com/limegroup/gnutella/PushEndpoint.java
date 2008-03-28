package com.limegroup.gnutella;

import java.util.Set;

import org.limewire.io.IpPort;

import com.limegroup.gnutella.http.HTTPHeaderValue;

public interface PushEndpoint extends HTTPHeaderValue, IpPort {

    public static final int HEADER_SIZE = 17; //guid+# of proxies, maybe other things too

    public static final int PROXY_SIZE = 6; //ip:port

    public static final byte PLAIN = 0x0; //no features for this PE

    public static final byte PPTLS_BINARY = (byte) 0x80;
    
    public static final byte SIZE_MASK=0x7; //0000 0111
    
    public static final byte FWT_VERSION_MASK=0x18; //0001 1000
    
    //the features mask does not clear the bits we do not understand
    //because we may pass on the altloc to someone who does.
    public static final byte FEATURES_MASK= (byte)0xE0;   //1110 0000
    
    /** The pptls portion constant. */
    public static final String PPTLS_HTTP = "pptls";

    /** The maximum number of proxies to use. */
    public static final int MAX_PROXIES = 4;

    /**
     * @return a byte-packed representation of this
     */
    byte[] toBytes(boolean includeTLS);

    /**
     * creates a byte packet representation of this
     * @param where the byte [] to serialize to 
     * @param offset the offset within that byte [] to serialize
     */
    void toBytes(byte[] where, int offset, boolean includeTLS);

    /**
     * Returns the GUID of the client that can be reached through the pushproxies.
     */
    byte[] getClientGUID();

    /**
     * @return a view of the current set of proxies, never returns null
     */
    Set<? extends IpPort> getProxies();

    /**
     * @return which version of F2F transfers this PE supports.
     * This always returns the most current version we know the PE supports
     * unless it has never been put in the map.
     */
    int getFWTVersion();

    /**
     * Should return the {@link GUID#hashCode()} of {@link #getClientGUID()}.
     */
    int hashCode();

    /**
     * Equality should be based on the equality of the value of {@link #getClientGUID()}.
     */
    boolean equals(Object other);
    
    /**
     * @return the various features this PE reports.  This always
     * returns the most current features, or the ones it was created with
     * if they have never been updated.
     */
    byte getFeatures();

    /**
     * @return true if this is the push endpoint for the local node
     */
    boolean isLocal();

    /**
     * Updates either the PushEndpoint or the GUID_PROXY_MAP to ensure
     * that GUID_PROXY_MAP has a reference to all live PE GUIDs and
     * all live PE's reference the same GUID object as in GUID_PROXY_MAP.
     * 
     * If this method is not called, the PE will know only about the set
     * of proxies the remote host had when it was created.  Otherwise it
     * will point to the most recent known set.
     */
    // TODO remove this in the long run and use the cache explicitly
    void updateProxies(boolean good);

    /**
     * Can return null if no valid push endpoint can be cloned. 
     */
    PushEndpoint createClone();
    
    /**
     * Returns an {@link IpPort} representing the valid external address of
     * this push endpoint if it is known, otherwise <code>null</code>. 
     */
    IpPort getValidExternalAddress();
    
    /**
     * @return the external address if known otherwise {@link RemoteFileDesc#BOGUS_IP}
     */
    String getAddress();
}