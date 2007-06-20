package com.limegroup.gnutella.uploader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.limewire.collection.BitNumbers;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderValueCollection;
import com.limegroup.gnutella.http.HTTPUtils;

/**
 * Provides methods to add commonly used headers to {@link HttpResponse}s.
 */
public class HTTPHeaderUtils {

    /**
     * Adds the <code>X-Available-Ranges</code> header to
     * <code>response</code> if available.
     */
    public static void addRangeHeader(HttpResponse response,
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
    public static void addProxyHeader(HttpResponse response) {
        if (RouterService.acceptedIncomingConnection())
            return;

        Set<? extends Connectable> proxies = RouterService.getConnectionManager()
                .getPushProxies();

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
        if (UDPService.instance().canDoFWT()) {
            response.addHeader(HTTPHeaderName.FWTPORT.create(UDPService.instance().getStableUDPPort() + ""));
        }
    }

    /**
     * Adds alternate locations for <code>fd</code> to <code>response</code>
     * if available.
     */
    public static void addAltLocationsHeader(HttpResponse response,
            HTTPUploader uploader, FileDesc fd) {
        // write the URN in case the caller wants it
        URN sha1 = fd.getSHA1Urn();
        if (sha1 != null) {
            response
                    .addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(sha1));
            Collection<DirectAltLoc> direct = uploader.getAltLocTracker().getNextSetOfAltsToSend();
            if (direct.size() > 0) {
                List<HTTPHeaderValue> ordered = new ArrayList<HTTPHeaderValue>(direct.size());
                final BitNumbers bn = new BitNumbers(direct.size());
                for(DirectAltLoc al : direct) {
                    IpPort ipp = al.getHost();
                    if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                        bn.set(ordered.size());
                    ordered.add(al);
                }
                
                if(!bn.isEmpty()) {
                    ordered.add(0, new HTTPHeaderValue() {
                        public String httpStringValue() {
                            return DirectAltLoc.TLS_IDX + bn.toHexString();
                        }
                    });
                }
                
                response.addHeader(HTTPHeaderName.ALT_LOCATION
                        .create(new HTTPHeaderValueCollection(ordered)));
            }

            if (uploader.getAltLocTracker().wantsFAlts()) {
                Collection<PushAltLoc> pushes = uploader.getAltLocTracker().getNextSetOfPushAltsToSend();
                if (pushes.size() > 0) {
                    response.addHeader(HTTPHeaderName.FALT_LOCATION
                            .create(new HTTPHeaderValueCollection(pushes)));
                }
            }
        }
    }

    /**
     * Adds an <code>X-Features</code> header to <code>response</code>.
     */
    public static void addFeatures(HttpResponse response) {
        Set<HTTPHeaderValue> features = HTTPUtils.getFeaturesValue();
        if (features.size() > 0) {
            response.addHeader(HTTPHeaderName.FEATURES.create(
                    new HTTPHeaderValueCollection(features)));
        }
    }

}
