package com.limegroup.gnutella.http;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.HeaderInterceptor;

import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.PushAltLoc;

public class AltLocHeaderInterceptor implements HeaderInterceptor {

    public void process(Header header, HttpContext context)
            throws HttpException, IOException {
        FileDetails fd = HttpContextParams.getFileDetails(context);
        if (fd != null) {
            if (header.getName().equals(HTTPHeaderName.ALT_LOCATION.name())) {
                parseAlternateLocations(getTracker(context, fd), header
                        .getValue(), true);
            } else if (header.getName().equals(HTTPHeaderName.NALTS.name())) {
                parseAlternateLocations(getTracker(context, fd), header
                        .getValue(), false);
            } else if (header.getName().equals(
                    HTTPHeaderName.FALT_LOCATION.name())) {
                AltLocTracker tracker = getTracker(context, fd);
                parseAlternateLocations(tracker, header.getValue(), true);
                tracker.setWantsFalts(true);
            } else if (header.getName().equals(
                    HTTPHeaderName.BFALT_LOCATION.name())) {
                AltLocTracker tracker = getTracker(context, fd);
                parseAlternateLocations(tracker, header.getValue(), false);
                tracker.setWantsFalts(false);
            }
        }
    }

    private AltLocTracker getTracker(HttpContext context, FileDetails fd) {
        AltLocTracker tracker = HttpContextParams.getAltLocTracker(context);
        if (tracker == null) {
            tracker = new AltLocTracker(fd.getSHA1Urn());
        } else {
            assert tracker.getUrn() == fd.getSHA1Urn();
        }
        return tracker;
    }

    /**
     * Parses the alternate location header. The header can contain only one
     * alternate location, or it can contain many in the same header. This
     * method will notify DownloadManager of new alternate locations if the
     * FileDesc is an IncompleteFileDesc.
     * 
     * @param altLocTracker
     * 
     * @param altHeader the full alternate locations header
     * @param alc the <tt>AlternateLocationCollector</tt> that reads alternate
     *        locations should be added to
     */
    private void parseAlternateLocations(AltLocTracker tracker,
            final String altHeader, boolean isGood) {
        final String alternateLocations = HTTPUtils
                .extractHeaderValue(altHeader);
        if (alternateLocations == null) {
            return;
        }

        StringTokenizer st = new StringTokenizer(alternateLocations, ",");
        while (st.hasMoreTokens()) {
            try {
                // note that the trim method removes any CRLF character
                // sequences that may be used if the sender is using
                // continuations.
                AlternateLocation al = AlternateLocation.create(st.nextToken()
                        .trim(), tracker.getUrn());

                if (al.isMe())
                    continue;

                if (al instanceof PushAltLoc)
                    ((PushAltLoc) al).updateProxies(isGood);
                // Note: if this thread gets preempted at this point,
                // the AlternateLocationCollectioin may contain a PE
                // without any proxies.
                if (isGood)
                    RouterService.getAltlocManager().add(al, null);
                else
                    RouterService.getAltlocManager().remove(al, null);

                tracker.addLocation(al);
            } catch (IOException e) {
                // just return without adding it.
                continue;
            }
        }
    }

}
