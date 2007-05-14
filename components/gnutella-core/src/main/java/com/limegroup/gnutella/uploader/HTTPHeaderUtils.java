package com.limegroup.gnutella.uploader;

import java.util.Iterator;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
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

        Set<IpPort> proxies = RouterService.getConnectionManager()
                .getPushProxies();

        StringBuilder buf = new StringBuilder();
        int proxiesWritten = 0;
        for (Iterator<IpPort> iter = proxies.iterator(); iter.hasNext()
                && proxiesWritten < 4;) {
            IpPort current = iter.next();
            buf.append(current.getAddress()).append(":").append(
                    current.getPort()).append(",");

            proxiesWritten++;
        }

        if (proxiesWritten > 0)
            buf.deleteCharAt(buf.length() - 1);
        else
            return;

        response.addHeader(HTTPHeaderName.PROXIES.create(buf.toString()));
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
            Set<? extends AlternateLocation> alts = uploader.getAltLocTracker()
                    .getNextSetOfAltsToSend();
            if (alts.size() > 0) {
                response.addHeader(HTTPHeaderName.ALT_LOCATION
                        .create(new HTTPHeaderValueCollection(alts)));
            }

            if (uploader.getAltLocTracker().wantsFAlts()) {
                alts = uploader.getAltLocTracker().getNextSetOfPushAltsToSend();
                if (alts.size() > 0) {
                    response.addHeader(HTTPHeaderName.FALT_LOCATION
                            .create(new HTTPHeaderValueCollection(alts)));
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
