package com.limegroup.gnutella;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.limewire.io.Address;

import com.limegroup.gnutella.downloader.DownloadStatsTracker;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;

/**
 * A reference to a single file on a remote machine.  In this respect
 * RemoteFileDesc is similar to a URL, but it contains Gnutella-
 * specific data as well, such as the server's 16-byte GUID.<p>
 */
public interface RemoteFileDesc extends RemoteFileDetails {

    /** bogus IP we assign to RFDs whose real ip is unknown */
    public static final String BOGUS_IP = "1.1.1.1";

    /** Typed reference to an empty list of RemoteFileDescs. */
    public static final List<RemoteFileDesc> EMPTY_LIST = Collections.emptyList();

    public void setSerializeProxies();

    /**
     * @return whether this rfd points to myself.
     */
    public boolean isMe(byte[] myClientGUID);

    /**
     * Determines whether or not this RemoteFileDesc was created
     * from an alternate location.
     */
    public boolean isFromAlternateLocation();

    /**
     * Sets this RFD as downloading.
     */
    public void setDownloading(boolean dl);

    /**
     * Determines if this RFD is downloading.
     *
     * @return whether or not this is downloading
     */
    public boolean isDownloading();

    /**
     * Returns an <tt>URL</tt> instance for this <tt>RemoteFileDesc</tt>.
     *
     * @return an <tt>URL</tt> instance for this <tt>RemoteFileDesc</tt>
     */
    public URL getUrl();

    /**
     * Determines whether or not this host reported a private address.
     *
     * @return <tt>true</tt> if the address for this host is private,
     *  otherwise <tt>false</tt>.  If the address is unknown, returns
     *  <tt>true</tt>
     *
     * TODO:: use InetAddress in this class for the host so that we don't 
     * have to go through the process of creating one each time we check
     * it it's a private address
     */
    public boolean isPrivate();

    /**
     * @return true if I am not a multicast host and have a hash.
     * also, if I am firewalled I must have at least one push proxy,
     * otherwise my port and address need to be valid.
     */
    public boolean isAltLocCapable();

    /**
     * 
     * @return whether a push should be sent to this rfd.
     */
    public boolean needsPush();
    
    /**
     * 
     * @return whether a push should be sent to this rfd.
     * @param statsTracker used to track download statistics
     */
    public boolean needsPush(DownloadStatsTracker statsTracker);

    /**
     * 
     * @return the push address.
     */
    public PushEndpoint getPushAddr();

    public void setSpamRating(float rating);

    public float getSpamRating();

    public int getSecureStatus();

    public void setSecureStatus(int secureStatus);

    /**
     * Returns a memento that can be used for serializing this object.
     */
    public RemoteHostMemento toMemento();
    
    public boolean isSpam();

    public Address toAddress();

}