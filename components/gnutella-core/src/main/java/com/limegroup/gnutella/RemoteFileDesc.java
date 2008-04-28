package com.limegroup.gnutella;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.limewire.collection.IntervalSet;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.downloader.DownloadStatsTracker;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;

/**
 * A reference to a single file on a remote machine.  In this respect
 * RemoteFileDesc is similar to a URL, but it contains Gnutella-
 * specific data as well, such as the server's 16-byte GUID.<p>
 */
public interface RemoteFileDesc extends IpPort, Connectable, FileDetails {

    /** bogus IP we assign to RFDs whose real ip is unknown */
    public static final String BOGUS_IP = "1.1.1.1";

    /** Typed reference to an empty list of RemoteFileDescs. */
    public static final List<RemoteFileDesc> EMPTY_LIST = Collections.emptyList();

    public void setSerializeProxies();

    /** Sets whether or not this host is TLS capable. */
    public void setTLSCapable(boolean tlsCapable);

    /** 
     * Accessor for HTTP11.
     *
     * @return Whether or not we think this host supports HTTP11.
     */
    public boolean isHTTP11();

    /**
     * Mutator for HTTP11.  Should be set after connecting.
     */
    public void setHTTP11(boolean http11);

    /**
     * Returns true if this is a partial source
     */
    public boolean isPartialSource();

    /**
     * @return whether this rfd points to myself.
     */
    public boolean isMe(byte[] myClientGUID);

    /**
     * Accessor for the available ranges.
     */
    public IntervalSet getAvailableRanges();

    /**
     * Mutator for the available ranges.
     */
    public void setAvailableRanges(IntervalSet availableRanges);

    /**
     * Returns the current failed count.
     */
    public int getFailedCount();

    /**
     * Increments the failed count by one.
     */
    public void incrementFailedCount();

    /**
     * Resets the failed count back to zero.
     */
    public void resetFailedCount();

    /**
     * Determines whether or not this RemoteFileDesc was created
     * from an alternate location.
     */
    public boolean isFromAlternateLocation();

    /**
     * @return true if this host is still busy and should not be retried
     */
    public boolean isBusy();

    public boolean isBusy(long now);

    /**
     * @return time to wait until this host will be ready to be retried
     * in seconds
     */
    public int getWaitTime(long now);

    /**
     * Mutator for _earliestRetryTime. 
     * @param seconds number of seconds to wait before retrying
     */
    public void setRetryAfter(int seconds);

    /**
     * The creation time of this file.
     */
    public long getCreationTime();

    /**
     * @return Returns the _THEXFailed.
     */
    public boolean hasTHEXFailed();

    /**
     * Having THEX with this host is no good. We can get our THEX from anybody,
     * so we won't bother again. 
     */
    public void setTHEXFailed();

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
     * Accessor for the host ip with this file.
     *
     * @return the host ip with this file
     */
    public String getHost();

    /**
     * Accessor for the index this file, which can be <tt>null</tt>.
     *
     * @return the file name for this file, which can be <tt>null</tt>
     */
    public long getIndex();

    /**
     * Accessor for the size in bytes of this file.
     *
     * @return the size in bytes of this file
     */
    public long getSize();

    /**
     * Accessor for the client guid for this file, which can be <tt>null</tt>.
     *
     * @return the client guid for this file, which can be <tt>null</tt>
     */
    public byte[] getClientGUID();

    /**
     * Accessor for the speed of the host with this file, which can be 
     * <tt>null</tt>.
     *
     * @return the speed of the host with this file, which can be 
     *  <tt>null</tt>
     */
    public int getSpeed();

    public String getVendor();

    public boolean isChatEnabled();

    public boolean isBrowseHostEnabled();

    /**
     * Returns the "quality" of the remote file in terms of firewalled status,
     * whether or not the remote host has open slots, etc.
     * 
     * @return the current "quality" of the remote file in terms of the 
     *  determined likelihood of the request succeeding
     */
    public int getQuality();

    /**
     * Returns an <tt>URL</tt> instance for this <tt>RemoteFileDesc</tt>.
     *
     * @return an <tt>URL</tt> instance for this <tt>RemoteFileDesc</tt>
     */
    public URL getUrl();

    /**
     * Determines whether or not this RFD was a reply to a multicast query.
     *
     * @return <tt>true</tt> if this RFD was in reply to a multicast query,
     *  otherwise <tt>false</tt>
     */
    public boolean isReplyToMulticast();

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
     * Accessor for the <tt>Set</tt> of <tt>PushProxyInterface</tt>s for this
     * file -- can be empty, but is guaranteed to be non-null.
     *
     * @return the <tt>Set</tt> of proxy hosts that will accept push requests
     *  for this host -- can be empty
     */
    public Set<? extends IpPort> getPushProxies();

    /**
     * @return whether this RFD supports firewall-to-firewall transfer.
     * For this to be true we need to have some push proxies, indication that
     * the host supports FWT and we need to know that hosts' external address.
     */
    public boolean supportsFWTransfer();

    /**
     * Creates the _hostData lazily and uses as necessary
     */
    public RemoteHostData getRemoteHostData();

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

    public void setQueueStatus(int status);

    public int getQueueStatus();

    public void setSpamRating(float rating);

    public float getSpamRating();

    public int getSecureStatus();

    public void setSecureStatus(int secureStatus);

    /**
     * Returns a memento that can be used for serializing this object.
     */
    public RemoteHostMemento toMemento();

}