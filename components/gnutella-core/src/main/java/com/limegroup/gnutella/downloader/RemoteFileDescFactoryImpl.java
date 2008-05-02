package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Set;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpHead;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.Connectable;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkInstanceUtils;
import org.xml.sax.SAXException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.http.URIUtils;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.SchemaNotFoundException;

@Singleton
class RemoteFileDescFactoryImpl implements RemoteFileDescFactory {

    private static final int COPY_INDEX = Integer.MAX_VALUE;

    private final LimeXMLDocumentFactory limeXMLDocumentFactory;

    private final PushEndpointFactory pushEndpointFactory;

    private final Provider<LimeHttpClient> httpClientProvider;
    
    private final NetworkInstanceUtils networkInstanceUtils;

    @Inject
    public RemoteFileDescFactoryImpl(LimeXMLDocumentFactory limeXMLDocumentFactory,
            PushEndpointFactory pushEndpointFactory, Provider<LimeHttpClient> httpClientProvider,
            NetworkInstanceUtils networkInstanceUtils) {
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.pushEndpointFactory = pushEndpointFactory;
        this.httpClientProvider = httpClientProvider;
        this.networkInstanceUtils = networkInstanceUtils;
    }

    public RemoteFileDesc createRemoteFileDesc(RemoteFileDesc rfd, IpPort ep) {
        return createRemoteFileDesc(ep.getAddress(), // host
                ep.getPort(), // port
                COPY_INDEX, // index (unknown)
                rfd.getFileName(), // filename
                rfd.getSize(), // filesize
                rfd.getClientGUID(), // client GUID
                0, // speed
                false, // chat capable
                2, // quality
                false, // browse hostable
                rfd.getXMLDocument(), // xml doc
                rfd.getUrns(), // urns
                false, // reply to MCast
                false, // is firewalled
                AlternateLocation.ALT_VENDOR, // vendor
                IpPort.EMPTY_SET, // push proxies
                rfd.getCreationTime(), // creation time
                0, // firewalled transfer
                null, // no PE cause not firewalled
                ep instanceof Connectable ? ((Connectable) ep).isTLSCapable() : false // TLS
                                                                                        // capable
                                                                                        // if
                                                                                        // ep
                                                                                        // is.
        );
    }

    public RemoteFileDesc createRemoteFileDesc(RemoteFileDesc rfd, PushEndpoint pe) {
        return createRemoteFileDesc(pe.getAddress(), // host - ignored
                pe.getPort(), // port -ignored
                COPY_INDEX, // index (unknown)
                rfd.getFileName(), // filename
                rfd.getSize(), // filesize
                DataUtils.EMPTY_GUID, // guid
                rfd.getSpeed(), // speed
                false, // chat capable
                rfd.getQuality(), // quality
                false, // browse hostable
                rfd.getXMLDocument(), // xml doc
                rfd.getUrns(), // urns
                false, // reply to MCast
                true, // is firewalled
                AlternateLocation.ALT_VENDOR, // vendor
                null, // push proxies
                rfd.getCreationTime(), // creation time
                0, // firewalled transfer
                pe, // use existing PE
                false); // not TLS capable (they connect to us anyway)
    }

    public RemoteFileDesc createRemoteFileDesc(String host, int port, long index, String filename,
            long size, byte[] clientGUID, int speed, boolean chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast,
            boolean firewalled, String vendor, Set<? extends IpPort> proxies, long createTime,
            boolean tlsCapable) {
        return createRemoteFileDesc(host, port, index, filename, size, clientGUID, speed, chat,
                quality, browseHost, xmlDoc, urns, replyToMulticast, firewalled, vendor, proxies,
                createTime, 0, null, tlsCapable);
    }

    public RemoteFileDesc createRemoteFileDesc(String host, int port, long index, String filename,
            long size, byte[] clientGUID, int speed, boolean chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast,
            boolean firewalled, String vendor, Set<? extends IpPort> proxies, long createTime,
            int FWTVersion, boolean tlsCapable) {
        return createRemoteFileDesc(host, port, index, filename, size, clientGUID, speed, chat,
                quality, browseHost, xmlDoc, urns, replyToMulticast, firewalled, vendor, proxies,
                createTime, FWTVersion, null, // this will create a PE to
                                                // house the data if the host is
                                                // firewalled
                tlsCapable);
    }

    public RemoteFileDesc createRemoteFileDesc(String host, int port, long index, String filename,
            long size, int speed, boolean chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast,
            boolean firewalled, String vendor, long createTime, PushEndpoint pe) {
        return createRemoteFileDesc(host, port, index, filename, size, null, speed, chat, quality,
                browseHost, xmlDoc, urns, replyToMulticast, firewalled, vendor, null, createTime,
                0, pe, false); // use exising pe
    }

    private RemoteFileDesc createRemoteFileDesc(String host, int port, long index, String filename,
            long size, byte[] clientGUID, int speed, boolean chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast,
            boolean firewalled, String vendor, Set<? extends IpPort> proxies, long createTime,
            int FWTVersion, PushEndpoint pe, boolean tlsCapable) {
        if (firewalled) {
            if (pe == null) {
                // Don't allow the bogus_ip in here.
                IpPort ipp;
                if(!host.equals(RemoteFileDesc.BOGUS_IP)) {
                    try {
                        ipp = new IpPortImpl(host, port);
                    } catch(UnknownHostException uhe) {
                        throw new IllegalArgumentException(uhe);
                    }
                } else {
                    ipp = null;
                    FWTVersion = 0;
                }
                pe = pushEndpointFactory.createPushEndpoint(clientGUID, proxies,
                        PushEndpoint.PLAIN, FWTVersion, ipp);
            }
            clientGUID = pe.getClientGUID();
        } else {
            assert pe == null;
        }

        if (urns == null)
            urns = Collections.emptySet();
        boolean http11 = !urns.isEmpty();

        return new RemoteFileDescImpl(host, port, index, filename, size, clientGUID, speed, chat,
                quality, browseHost, xmlDoc, urns, replyToMulticast, firewalled, vendor, proxies,
                createTime, FWTVersion, pe, tlsCapable, http11, networkInstanceUtils);
    }

    public RemoteFileDesc createUrlRemoteFileDesc(String host, int port, String filename,
            long size, Set<? extends URN> urns, URL url) {
        RemoteFileDesc rfd = new UrlRemoteFileDescImpl(host, port, filename, size, urns, url, networkInstanceUtils);
        rfd.setHTTP11(false);
        return rfd;
    }

    public RemoteFileDesc createUrlRemoteFileDesc(URL url, String filename, URN urn, long size)
            throws IOException, URISyntaxException, HttpException, InterruptedException {
        // Use the URL class to do a little parsing for us.
        int port = url.getPort();
        if (port < 0)
            port = 80; // assume default for HTTP (not 6346)

        Set<URN> urns = new UrnSet();
        if (urn != null)
            urns.add(urn);

        URI uri = URIUtils.toURI(url.toExternalForm());

        return createUrlRemoteFileDesc(url.getHost(), port, filename != null ? filename
                : MagnetOptions.extractFileName(uri), size <= 0 ? contentLength(uri) : size, urns,
                url);
    }

    private long contentLength(URI uri) throws HttpException, IOException, InterruptedException {
        HttpHead head = new HttpHead(uri);
        head.addHeader("User-Agent", LimeWireUtils.getHttpServer());
        HttpResponse response = null;
        LimeHttpClient client = httpClientProvider.get();
        try {
            response = client.execute(head);
            // Extract Content-length, but only if the response was 200 OK.
            // Generally speaking any 2xx response is ok, but in this situation
            // we expect only 200.
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Got " + response.getStatusLine().getStatusCode()
                        + " instead of 200 for URL: " + uri);

            long length = -1;
            if (response.getEntity() != null) {
                length = response.getEntity().getContentLength();
            }
            if (length < 0)
                throw new IOException("No content length");
            return length;
        } finally {
            client.releaseConnection(response);
        }
    }

    public RemoteFileDesc createFromMemento(RemoteHostMemento remoteHostMemento)
            throws InvalidDataException {
        if (remoteHostMemento.getCustomUrl() != null) {
            return createUrlRemoteFileDesc(remoteHostMemento.getHost(),
                    remoteHostMemento.getPort(), remoteHostMemento.getFileName(), remoteHostMemento
                            .getSize(), remoteHostMemento.getUrns(), remoteHostMemento
                            .getCustomUrl());
        } else {
            try {
                return createRemoteFileDesc(remoteHostMemento.getHost(), remoteHostMemento
                        .getPort(), remoteHostMemento.getIndex(), remoteHostMemento.getFileName(),
                        remoteHostMemento.getSize(), remoteHostMemento.getClientGuid(),
                        remoteHostMemento.getSpeed(), remoteHostMemento.isChat(), remoteHostMemento
                                .getQuality(), remoteHostMemento.isBrowseHost(),
                        xml(remoteHostMemento.getXml()), remoteHostMemento.getUrns(),
                        remoteHostMemento.isReplyToMulticast(), remoteHostMemento.isFirewalled(),
                        remoteHostMemento.getVendor(), IpPort.EMPTY_SET, -1L, -1,
                        pe(remoteHostMemento.getPushAddr()), remoteHostMemento.isTls());
            } catch (SAXException e) {
                throw new InvalidDataException(e);
            } catch (SchemaNotFoundException e) {
                throw new InvalidDataException(e);
            } catch (IOException e) {
                throw new InvalidDataException(e);
            }
        }
    }

    private PushEndpoint pe(String pushAddr) throws IOException {
        if (pushAddr != null)
            return pushEndpointFactory.createPushEndpoint(pushAddr);
        else
            return null;
    }

    private LimeXMLDocument xml(String xml) throws SAXException, SchemaNotFoundException,
            IOException {
        if (xml != null)
            return limeXMLDocumentFactory.createLimeXMLDocument(xml);
        else
            return null;
    }

}
