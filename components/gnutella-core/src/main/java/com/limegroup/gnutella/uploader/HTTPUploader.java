package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.net.InetAddress;

import org.limewire.collection.Interval;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.http.AltLocTracker;

public class HTTPUploader extends AbstractUploader implements Uploader {
    /**
     * The URN specified in the X-Gnutella-Content-URN header, if any.
     */
    private URN requestedURN;

    private boolean supportsQueueing = false;

    private AltLocTracker altLocTracker;

    private long uploadBegin;
    
    private long uploadEnd;

    private boolean containedRangeRequest;

    public HTTPUploader(String fileName, UploadSession session, int index) {
        super(fileName, session, index);
    }

    @Override
    public void reinitialize() {
        super.reinitialize();
        
        requestedURN = null;
    }
    
    @Override
    public void setFileDesc(FileDesc fd) throws IOException {
        super.setFileDesc(fd);
        
        setUploadBegin(0);
        setUploadEnd(getFileSize());
    }
    
    @Override
    public int getAmountWritten() {
        // TODO Auto-generated method stub
        return 0;
    }

    public InetAddress getConnectedHost() {
        // TODO Auto-generated method stub
        return null;
    }

    public void stop() {
        // TODO Auto-generated method stub
        
    }

    public boolean isTHEXRequest() {
        // TODO Auto-generated method stub
        return false;
    }

    public long getUploadBegin() {
        return this.uploadBegin;
    }

    /**
     * Exclusive index of the last byte.
     */
    public long getUploadEnd() {
        return this.uploadEnd;
    }

    public boolean containedRangeRequest() {
        return containedRangeRequest;
    }

    public boolean validateRange() {
        long first = getUploadBegin();
        long last = getUploadEnd();

        if (getFileDesc() instanceof IncompleteFileDesc) {
            // If we are allowing, see if we have the range.
            IncompleteFileDesc ifd = (IncompleteFileDesc) getFileDesc();
            // If the request contained a 'Range:' header, then we can
            // shrink the request to what we have available.
            if (containedRangeRequest()) {
                Interval request = ifd.getAvailableSubRange((int)first, (int)last - 1);
                if (request == null) {
                    return false;
                }
                setUploadBegin(request.low);
                setUploadEnd(request.high + 1);
            } else {
                if (!ifd.isRangeSatisfiable((int) first, (int) last - 1)) {
                    return false;
                }
            }
        } else {
            if (containedRangeRequest()) {
                setUploadEnd(Math.min(last, getFileSize())); 
            } else  if (first < 0 || last > getFileSize()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the content URN that the client asked for.
     */
    public URN getRequestedURN() {
        return requestedURN;
    }

    public void setRequestedURN(URN requestedURN) {
        this.requestedURN = requestedURN;
    }
    
    public boolean supportsQueueing() {
        return supportsQueueing && isValidQueueingAgent();
    }

    /**
     * Blocks certain vendors from being queued, because of buggy downloading
     * implementations on their side.
     */
    private boolean isValidQueueingAgent() {
        if (getUserAgent() == null)
            return true;

        return !getUserAgent().startsWith("Morpheus 3.0.2");
    }

    public void setSupportsQueueing(boolean supportsQueueing) {
        this.supportsQueueing = supportsQueueing;
    }

    public AltLocTracker getAltLocTracker() {
        if (altLocTracker == null) {
            altLocTracker = new AltLocTracker(getFileDesc().getSHA1Urn());
        }
        return altLocTracker;
    }

    public void setUploadBegin(long uploadBegin) {
        this.uploadBegin = uploadBegin;
    }
    
    public void setUploadEnd(long uploadEnd) {
        this.uploadEnd = uploadEnd;
    }
    
    public void setContainedRangeRequest(boolean containedRangeRequest) {
        this.containedRangeRequest = containedRangeRequest;
    }

    
}
