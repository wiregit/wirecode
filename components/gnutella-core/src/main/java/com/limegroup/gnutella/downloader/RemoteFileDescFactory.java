package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import org.apache.http.HttpException;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public interface RemoteFileDescFactory {

    /**
     * Constructs a RemoteFileDesc based on this memento.
     */
    public RemoteFileDesc createFromMemento(RemoteHostMemento remoteHostMemento)
            throws InvalidDataException;

    /**
     * Constructs a new RemoteFileDescImpl exactly like the other one,
     * but with a different remote host.
     *
     * It is okay to use the same internal structures
     * for URNs because the Set is immutable.
     */
    public RemoteFileDesc createRemoteFileDesc(RemoteFileDesc rfd, IpPort ep);

    /**
     * Constructs a new RemoteFileDescImpl exactly like the other one,
     * but with a different push proxy host.  Will be handy when processing
     * head pongs.
     */
    public RemoteFileDesc createRemoteFileDesc(RemoteFileDesc rfd, PushEndpoint pe);

    /** 
     * Constructs a new RemoteFileDescImpl with metadata.
     *
     * @param host the host's ip
     * @param port the host's port
     * @param index the index of the file that the client sent
     * @param filename the name of the file
     * @param size the completed size of this file
     * @param clientGUID the unique identifier of the client
     * @param speed the speed of the connection
     * @param chat true if the location is chattable
     * @param quality the quality of the connection, where 0 is the
     *  worst and 3 is the best.  (This is the same system as in the
     *  GUI but on a 0 to N-1 scale.)
     * @param browseHost specifies whether or not the remote host supports
     *  browse host
     * @param xmlDoc the <tt>LimeXMLDocument</tt> for the response
     * @param urns the <tt>Set</tt> of <tt>URN</tt>s for the file
     * @param replyToMulticast true if its from a reply to a multicast query
     * @param firewalled true if the host is firewalled
     * @param vendor the vendor of the remote host
     * @param proxies the push proxies for this host
     * @param createTime the network-wide creation time of this file
     * @param tlsCapable true if the remote host supports TLS
     * @throws <tt>IllegalArgumentException</tt> if any of the arguments are
     *  not valid
     * @throws <tt>NullPointerException</tt> if the host argument is 
     *  <tt>null</tt> or if the file name is <tt>null</tt>
     */
    public RemoteFileDesc createRemoteFileDesc(String host, int port, long index, String filename,
            long size, byte[] clientGUID, int speed, boolean chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast, boolean firewalled, String vendor, Set<? extends IpPort> proxies,
            long createTime, boolean tlsCapable);

    /** 
     * Constructs a new RemoteFileDescImpl with metadata.
     *
     * @param host the host's ip
     * @param port the host's port
     * @param index the index of the file that the client sent
     * @param filename the name of the file
     * @param clientGUID the unique identifier of the client
     * @param speed the speed of the connection
     * @param chat true if the location is chattable
     * @param quality the quality of the connection, where 0 is the
     *  worst and 3 is the best.  (This is the same system as in the
     *  GUI but on a 0 to N-1 scale.)
     * @param browseHost specifies whether or not the remote host supports
     *  browse host
     * @param xmlDoc the <tt>LimeXMLDocument</tt> for the response
     * @param urns the <tt>Set</tt> of <tt>URN</tt>s for the file
     * @param replyToMulticast true if its from a reply to a multicast query
     * @param tlsCapable true if the host supports a TLS connection
     * @param xmlDocs the array of XML documents pertaining to this file
     * @throws <tt>IllegalArgumentException</tt> if any of the arguments are
     *  not valid
     * @throws <tt>NullPointerException</tt> if the host argument is 
     *  <tt>null</tt> or if the file name is <tt>null</tt>
     */
    public RemoteFileDesc createRemoteFileDesc(String host, int port, long index, String filename,
            long size, byte[] clientGUID, int speed, boolean chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast, boolean firewalled, String vendor, Set<? extends IpPort> proxies,
            long createTime, int FWTVersion, boolean tlsCapable);

    /** Constructs a RemoteFileDesc using the given PushEndpoint. */
    public RemoteFileDesc createRemoteFileDesc(String host, int port, long index, String filename,
            long size, int speed, boolean chat, int quality, boolean browseHost, LimeXMLDocument xmlDoc,
            Set<? extends URN> urns, boolean replyToMulticast, boolean firewalled, String vendor, long createTime, PushEndpoint pe);

    /**
     * Constructs a URLRemoteFileDesc, whose getUrl method will return that URL.
     */
    public RemoteFileDesc createUrlRemoteFileDesc(String host, int port, String filename, long size,
            Set<? extends URN> urns, URL url);
    
    /**
     * Constructs a URLRemoteFileDesc, looking up the size from the URL if no size is known.<p>
     * 
     * <b>This method can block if the size is <= 0.</b>
     */
    public RemoteFileDesc createUrlRemoteFileDesc(URL url, String filename, URN urn, long size)
            throws IOException, URISyntaxException, HttpException, InterruptedException;

}
