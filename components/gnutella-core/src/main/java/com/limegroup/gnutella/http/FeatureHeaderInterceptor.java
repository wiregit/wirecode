package com.limegroup.gnutella.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.HeaderInterceptor;
import org.limewire.io.NetworkUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.uploader.HTTPUploader;

/**
 * Processes Gnutella headers from an {@link HttpRequest} and updates
 * a corresponding {@link HTTPUploader}.
 */
public class FeatureHeaderInterceptor implements HeaderInterceptor {

    private static final Log LOG = LogFactory.getLog(FeatureHeaderInterceptor.class);

    private HTTPUploader uploader;

    public FeatureHeaderInterceptor(HTTPUploader uploader) {
        this.uploader = uploader;
    }

    public void process(Header header, HttpContext context)
            throws HttpException, IOException {
        if (readChatHeader(header))
            ;
        else if (readRangeHeader(header))
            ;
        else if (readContentURNHeader(header))
            ;
        else if (readQueueVersion(header))
            ;
        else if (readFeatureHeader(header))
            ;
        else if (readXDownloadedHeader(header))
            ;
        else if (readNodeHeader(header))
            ;
    }

    /**
     * Read the chat portion of a header.
     * 
     * @return true if it had a chat header.
     */
    private boolean readChatHeader(Header header) throws IOException {
        if (!HTTPHeaderName.CHAT.matches(header)) {
            return false;
        }

        if (setHostAndPort(header.getValue())) {
            uploader.setChatEnabled(true);
            uploader.setBrowseHostEnabled(true);
        } else {
            throw new ProblemReadingHeaderException();
        }
        
        return true;
    }

    /**
     * Look for X-Downloaded header which represents number of bytes for this
     * file already downloaded by peer
     * 
     * @return true if it had a X-Downloaded header
     */
    private boolean readXDownloadedHeader(Header header) {

        if (!HTTPHeaderName.DOWNLOADED.matches(header))
            return false;

        try {
            uploader.setTotalAmountUploadedBefore(Integer.parseInt(header.getValue()));
        } catch (NumberFormatException e) {
        }

        return true;
    }

    /**
     * Look for range header of form, "Range: bytes=", "Range:bytes=", "Range:
     * bytes ", etc. Note that the "=" is required by HTTP, but old versions of
     * BearShare do not send it. The value following the bytes unit will be in
     * the form '-n', 'm-n', or 'm-'.
     * 
     * @return true if it had a Range header
     */
    private boolean readRangeHeader(Header header) throws IOException {
        if (!HTTPHeaderName.RANGE.matches(header)) { 
            return false;
        }

        uploader.setContainedRangeRequest(true);

        // Set 'sub' to the value after the "bytes=" or "bytes ". Note
        // that we don't validate the data between "Range:" and the
        // bytes.
        String sub;
        String second;
        try {
            int i = header.getValue().indexOf("bytes");
            if (i < 0)
                throw new ProblemReadingHeaderException(
                        "bytes not present in range");
            sub = header.getValue().substring(i + 6);
        } catch (IndexOutOfBoundsException e) {
            throw new ProblemReadingHeaderException();
        }
        // remove the white space
        sub = sub.trim();
        char c;
        // get the first character
        try {
            c = sub.charAt(0);
        } catch (IndexOutOfBoundsException e) {
            throw new ProblemReadingHeaderException();
        }
        // - n
        if (c == '-') {
            // String second;
            try {
                second = sub.substring(1);
            } catch (IndexOutOfBoundsException e) {
                throw new ProblemReadingHeaderException();
            }
            second = second.trim();
            try {
                // A range request for "-3" means return the last 3 bytes
                // of the file. (LW used to incorrectly return bytes
                // 0-3.)
                uploader.setUploadBegin(Math
                        .max(0, uploader.getFileSize() - Integer.parseInt(second)));
                uploader.setUploadEnd(uploader.getFileSize());
            } catch (NumberFormatException e) {
                throw new ProblemReadingHeaderException();
            }
        } else {
            // m - n or 0 -
            int dash = sub.indexOf("-");

            // If the "-" does not exist, the head is incorrectly formatted.
            if (dash == -1) {
                throw new ProblemReadingHeaderException();
            }
            String first = sub.substring(0, dash).trim();
            try {
                uploader.setUploadBegin(Long.parseLong(first));
            } catch (NumberFormatException e) {
                throw new ProblemReadingHeaderException();
            }
            try {
                second = sub.substring(dash + 1);
            } catch (IndexOutOfBoundsException e) {
                throw new ProblemReadingHeaderException();
            }
            second = second.trim();
            if (!second.equals(""))
                try {
                    // HTTP range requests are inclusive. So "1-3" means
                    // bytes 1, 2, and 3. But _uploadEnd is an EXCLUSIVE
                    // index, so increment by 1.
                    uploader.setUploadEnd(Long.parseLong(second) + 1);
                } catch (NumberFormatException e) {
                    throw new ProblemReadingHeaderException();
                }
        }

        return true;
    }

    /**
     * This method parses the "X-Gnutella-Content-URN" header, as specified
     * in HUGE v0.93.  This assigns the requested urn value for this 
     * upload, which otherwise remains null.
     *
     * @param contentUrnStr the string containing the header
     * @return a new <tt>URN</tt> instance for the request line, or 
     *  <tt>null</tt> if there was any problem creating it
     * 
     * @return true if the header had a contentURN field
     */
    private boolean readContentURNHeader(Header header) {
        if (!HTTPHeaderName.GNUTELLA_CONTENT_URN.matches(header))
            return false;

        try {
            uploader.setRequestedURN(URN.createSHA1Urn(header.getValue()));
        } catch(IOException e) {
            uploader.setRequestedURN(URN.INVALID);
        }       
        
        return true;
    }

    private boolean readQueueVersion(Header header) {
        if (!HTTPHeaderName.QUEUE.matches(header))
            return false;

        // we are not interested in the value at this point, the fact that the
        // header was sent implies that the uploader supports queueing.
        uploader.setSupportsQueueing(true);
        
        return true;
    }

    /**
     * Reads the X-Features header
     * 
     * @return true if the header had an node description value
     */
    private boolean readFeatureHeader(Header header) {
        if (!HTTPHeaderName.FEATURES.matches(header))
            return false;
        String value = header.getValue();
        if (LOG.isDebugEnabled())
            LOG.debug("reading feature header: " + value);
        
        StringTokenizer tok = new StringTokenizer(value, ",");
        while (tok.hasMoreTokens()) {
            String feature = tok.nextToken();
            String protocol = "";
            int slash = feature.indexOf("/");
            if (slash == -1) {
                protocol = feature.toLowerCase().trim();
            } else {
                protocol = feature.substring(0, slash).toLowerCase().trim();
            }
            // not interested in the version ...

            if (protocol.equals(HTTPConstants.CHAT_PROTOCOL))
                uploader.setChatEnabled(true);
            else if (protocol.equals(HTTPConstants.BROWSE_PROTOCOL))
                uploader.setBrowseHostEnabled(true);
            else if (protocol.equals(HTTPConstants.QUEUE_PROTOCOL))
                uploader.setSupportsQueueing(true);
            else if (protocol.equals(HTTPConstants.PUSH_LOCS))
                uploader.getAltLocTracker().setWantsFAlts(true);
            else if (protocol.equals(HTTPConstants.FW_TRANSFER)) {
                try {
                    // for this header we care about the version
                    uploader.getAltLocTracker().setFwtVersion((int) HTTPUtils.parseFeatureToken(feature));
                    uploader.getAltLocTracker().setWantsFAlts(true);
                } catch (ProblemReadingHeaderException prhe) {
                    continue;
                }
            }

        }
        return true;
    }

    /**
     * Reads the X-Node header.
     * 
     * @return true if the header had an node description value
     */
    private boolean readNodeHeader(final Header header) {
        if (!HTTPHeaderName.NODE.matches(header))
            return false;

        setHostAndPort(header.getValue());

        return true;
    }
    
    /**
     * Sets the host and port of <code>uploader</code> from <code>value</code>
     * if it describes a valid address.
     * 
     * @param value host:port
     * @return true, if host and port were set
     */
    private boolean setHostAndPort(String value) {
        InetAddress host;
        int port = -1;

        StringTokenizer st = new StringTokenizer(value, ":");
        if (st.countTokens() == 2) {
            try {
                host = InetAddress.getByName(st.nextToken().trim());
                port = Integer.parseInt(st.nextToken().trim());

                if (NetworkUtils.isValidPort(port)) {
                    uploader.setHost(host.getHostAddress());
                    uploader.setGnutellaPort(port);
                    return true;
                }
            } catch (UnknownHostException ignore) {
            } catch (NumberFormatException ignore) {
            }
        }        
        return false;
    }
    

}
