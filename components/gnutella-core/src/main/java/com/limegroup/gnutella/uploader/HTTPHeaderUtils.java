package com.limegroup.gnutella.uploader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.limewire.collection.BitNumbers;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.http.AltLocTracker;
import com.limegroup.gnutella.http.FeaturesWriter;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderValueCollection;

/**
 * Provides methods to add commonly used headers to {@link HttpResponse}s.
 */
@Singleton
public class HTTPHeaderUtils {

    private final NetworkManager networkManager;
    private final FeaturesWriter featuresWriter;
    private final Provider<ConnectionManager> connectionManager;
    
    @Inject
    public HTTPHeaderUtils(FeaturesWriter featuresWriter, NetworkManager networkManager, Provider<ConnectionManager> connectionManager) {
        this.networkManager = networkManager;
        this.featuresWriter = featuresWriter;
        this.connectionManager = connectionManager;
    }
    
    /**
     * Adds the <code>X-Available-Ranges</code> header to
     * <code>response</code> if available.
     */
    public void addRangeHeader(HttpResponse response,
            HTTPUploader uploader, FileDesc fd) {
        if (fd instanceof IncompleteFileDesc) {
            URN sha1 = uploader.getFileDesc().getSHA1Urn();
            if (sha1 != null) {
                IncompleteFileDesc ifd = (IncompleteFileDesc) fd;
                response.addHeader(HTTPHeaderName.AVAILABLE_RANGES.create(ifd));
            }
        }
    }

    /**
     * Writes out the X-Push-Proxies header as specified by section 4.2 of the
     * Push Proxy proposal, v. 0.7
     */
    public void addProxyHeader(HttpResponse response) {
        if (networkManager.acceptedIncomingConnection())
            return;

        Set<? extends Connectable> proxies = connectionManager.get().getPushProxies();

        StringBuilder buf = new StringBuilder();
        int proxiesWritten = 0;
        BitNumbers bn = new BitNumbers(proxies.size());
        for(Connectable current : proxies) {
            if(proxiesWritten >= 4)
                break;
            
            if(current.isTLSCapable())
                bn.set(proxiesWritten);

            buf.append(current.getAddress())
               .append(":")
               .append(current.getPort())
               .append(",");

            proxiesWritten++;
        }
        
        if(!bn.isEmpty())
            buf.insert(0, PushEndpoint.PPTLS_HTTP + "=" + bn.toHexString() + ",");

        if (proxiesWritten > 0)
            buf.deleteCharAt(buf.length() - 1);
        else
            return;

        response.addHeader(HTTPHeaderName.PROXIES.create(buf.toString()));
        
        // write out X-FWPORT if we support firewalled transfers, so the other side gets our port
        // for future fw-fw transfers
        if (networkManager.canDoFWT()) {
            response.addHeader(HTTPHeaderName.FWTPORT.create(networkManager.getStableUDPPort() + ""));
        }
    }

    /**
     * Adds alternate locations for <code>fd</code> to <code>response</code>
     * if available.
     */
    public void addAltLocationsHeader(HttpResponse response, AltLocTracker altLocTracker, AltLocManager altLocManager) {
        response.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(altLocTracker.getUrn()));
        Collection<DirectAltLoc> direct = altLocTracker.getNextSetOfAltsToSend(altLocManager);
        if (direct.size() > 0) {
            List<HTTPHeaderValue> ordered = new ArrayList<HTTPHeaderValue>(
                    direct.size());
            final BitNumbers bn = new BitNumbers(direct.size());
            for (DirectAltLoc al : direct) {
                IpPort ipp = al.getHost();
                if (ipp instanceof Connectable
                        && ((Connectable) ipp).isTLSCapable())
                    bn.set(ordered.size());
                ordered.add(al);
            }

            if (!bn.isEmpty()) {
                ordered.add(0, new HTTPHeaderValue() {
                    public String httpStringValue() {
                        return DirectAltLoc.TLS_IDX + bn.toHexString();
                    }
                });
            }

            response.addHeader(HTTPHeaderName.ALT_LOCATION
                    .create(new HTTPHeaderValueCollection(ordered)));
        }

        if (altLocTracker.wantsFAlts()) {
            Collection<PushAltLoc> pushes = altLocTracker.getNextSetOfPushAltsToSend(altLocManager);
            if (pushes.size() > 0) {
                response.addHeader(HTTPHeaderName.FALT_LOCATION
                        .create(new HTTPHeaderValueCollection(pushes)));
            }
        }
    }

//    public void addAltLocationsHeaders(HttpResponse response, HTTPUploader uploader, URN urn) {
//        response.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(urn));
//        Collection<? extends AlternateLocation> alts = uploader.getAltLocTracker().getNextSetOfAltsToSend();
//        if(alts.size() > 0) {
//            response.addHeader(HTTPHeaderName.ALT_LOCATION.create(new HTTPHeaderValueCollection(alts)));
//        }
//
//        if (uploader.getAltLocTracker().wantsFAlts) {
//            alts = getNextSetOfPushAltsToSend();
//            if (alts.size() > 0) {
//                response.addHeader(HTTPHeaderName.FALT_LOCATION.create(new HTTPHeaderValueCollection(alts)));
//            }
//        }
//    }

    /**
     * Adds an <code>X-Features</code> header to <code>response</code>.
     */
    public void addFeatures(HttpResponse response) {
        Set<HTTPHeaderValue> features = featuresWriter.getFeaturesValue();
        if (features.size() > 0) {
            response.addHeader(HTTPHeaderName.FEATURES.create(
                    new HTTPHeaderValueCollection(features)));
        }
    }

}
