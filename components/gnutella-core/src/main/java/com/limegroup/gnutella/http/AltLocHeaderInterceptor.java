package com.limegroup.gnutella.http;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.HeaderInterceptor;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.uploader.HTTPUploader;

public class AltLocHeaderInterceptor implements HeaderInterceptor {

    private HTTPUploader uploader;

    public AltLocHeaderInterceptor(HTTPUploader uploader) {
        this.uploader = uploader;
    }

    public void process(Header header, HttpContext context)
            throws HttpException, IOException {
        if (HTTPHeaderName.ALT_LOCATION.matches(header)) {
            parseAlternateLocations(uploader.getAltLocTracker(), header.getValue(),
                    true);
        } else if (HTTPHeaderName.NALTS.matches(header)) {
            parseAlternateLocations(uploader.getAltLocTracker(), header.getValue(),
                    false);
        } else if (HTTPHeaderName.FALT_LOCATION.matches(header)) {
            AltLocTracker tracker = uploader.getAltLocTracker();
            parseAlternateLocations(tracker, header.getValue(), true);
            tracker.setWantsFAlts(true);
        } else if (HTTPHeaderName.BFALT_LOCATION.matches(header)) {
            AltLocTracker tracker = uploader.getAltLocTracker();
            parseAlternateLocations(tracker, header.getValue(), false);
            tracker.setWantsFAlts(false);
        }
    }

//    private AltLocTracker getTracker(HttpContext context, FileDetails fd) {
//        AltLocTracker tracker = HttpContextParams.getAltLocTracker(context);
//        if (tracker == null) {
//            tracker = new AltLocTracker(fd.getSHA1Urn());
//        } else {
//            assert tracker.getUrn() == fd.getSHA1Urn();
//        }
//        return tracker;
//    }

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
            final String alternateLocations, boolean isGood) {
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
