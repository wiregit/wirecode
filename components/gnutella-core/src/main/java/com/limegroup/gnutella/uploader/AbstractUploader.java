package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

/**
 * Maintains state for an HTTP upload request.
 * 
 * Care must be taken to call closeFileStreams whenever a chunk of the transfer
 * is finished, and to call stop when the entire HTTP/1.1 session is finished.
 * 
 * A single HTTPUploader should be reused for multiple chunks of a single file
 * in an HTTP/1.1 session. However, multiple HTTPUploaders should be used for
 * multiple files in a single HTTP/1.1 session.
 */
public abstract class AbstractUploader implements Uploader {

    private static final Log LOG = LogFactory.getLog(AbstractUploader.class);

    private final UploadSession session;

    private long totalAmountReadBefore;

    private long totalAmountRead;

    private long amountRead;

    private long fileSize;

    private int index;

    private String userAgent;

    private final String _filename;

    private int _stateNum = CONNECTING;

    private int _lastTransferStateNum;

    private boolean _firstReply = true;

    private boolean chatEnabled;

    private boolean browseEnabled;

    /**
     * True if this is a forcibly shared network file.
     */
    private boolean isForcedShare = false;

    /**
     * True if this is an uploader with high priority.
     */
    private boolean priorityShare = false;

    /**
     * The descriptor for the file we're uploading.
     */
    private FileDesc fileDesc;

    /**
     * The address as described by the "X-Node" header.
     */
    private InetAddress nodeAddress = null;

    /**
     * The port as described by the "X-Node" header.
     */
    private int nodePort = -1;

    /** The upload type of this uploader. */
    private UploadType _uploadType;

    public AbstractUploader(String fileName, UploadSession session, int index) {
        this.session = session;
        _filename = fileName;
        this.index = index;
        totalAmountRead = 0;
        amountRead = 0;
        reinitialize();
    }

    /**
     * Reinitializes this uploader for a new request method.
     * 
     * @param method the HTTPRequestMethod to change to.
     * @param params the parameter list to change to.
     */
    public void reinitialize() {
        _stateNum = CONNECTING;
        nodePort = 0;
        totalAmountReadBefore = 0;
        totalAmountRead += amountRead;
        amountRead = 0;
    }

    /**
     * Sets the FileDesc for this HTTPUploader to use.
     * 
     * @param fd the <tt>FileDesc</tt> to use
     * @throws IOException if the file cannot be read from the disk.
     */
    public void setFileDesc(FileDesc fd) throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("trying to set the fd for uploader " + this + " with "
                    + fd);
        fileDesc = fd;
        fileSize = (int) fd.getFileSize();

        isForcedShare = FileManager.isForcedShare(fileDesc);
        priorityShare = FileManager.isApplicationSpecialShare(fileDesc
                .getFile());
    }

    public void setState(int state) {
        _lastTransferStateNum = _stateNum;
        _stateNum = state;
    }

    /**
     * Returns the queued position if queued.
     */
    public int getQueuePosition() {
        if (_lastTransferStateNum != QUEUED || _stateNum == INTERRUPTED)
            return -1;
        else
            return session.positionInQueue();
    }

    /**
     * Sets the number of bytes that have been uploaded for this upload. This is
     * expected to restart from 0 for each chunk of an HTTP/1.1 transfer.
     * 
     * @param amount the number of bytes that have been uploaded
     */
    void setAmountUploaded(long amount) {
        int newData = (int) (amount - amountRead);
        if (newData > 0) {
            if (isForcedShare())
                BandwidthStat.HTTP_BODY_UPSTREAM_INNETWORK_BANDWIDTH
                        .addData(newData);
            else
                BandwidthStat.HTTP_BODY_UPSTREAM_BANDWIDTH.addData(newData);
        }
        amountRead = amount;
    }

    /**
     * Returns whether or not this upload is in what is considered an "inactive"
     * state, such as completed or aborted.
     * 
     * @return <tt>true</tt> if this upload is in an inactive state,
     *         <tt>false</tt> otherwise
     */
    public boolean isInactive() {
        switch (_stateNum) {
        case COMPLETE:
        case INTERRUPTED:
            return true;
        default:
            return false;
        }
    }

    // implements the Uploader interface
    public long getFileSize() {
        if (_stateNum == THEX_REQUEST)
            return fileDesc.getHashTree().getOutputLength();
        else
            return fileSize;
    }

    // implements the Uploader interface
    public int getIndex() {
        return index;
    }

    // implements the Uploader interface
    public String getFileName() {
        return _filename;
    }

    // implements the Uploader interface
    public int getState() {
        return _stateNum;
    }

    // implements the Uploader interface
    public int getLastTransferState() {
        return _lastTransferStateNum;
    }

    // implements the Uploader interface
    public String getHost() {
        return session.getHost();
    }

    // implements the Uploader interface
    public boolean isChatEnabled() {
        return chatEnabled;
    }

    // implements the Uploader interface
    public boolean isBrowseHostEnabled() {
        return browseEnabled;
    }

    // implements the Uploader interface
    public int getGnutellaPort() {
        return nodePort;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public boolean isForcedShare() {
        return isForcedShare;
    }

    // uploader with high priority?
    public boolean isPriorityShare() {
        return priorityShare;
    }

    protected boolean isFirstReply() {
        return _firstReply;
    }

    public InetAddress getNodeAddress() {
        return nodeAddress;
    }

    public int getNodePort() {
        return nodePort;
    }

    /**
     * The amount of bytes that this upload has transferred. For HTTP/1.1
     * transfers, this number is the amount uploaded for this specific chunk
     * only. Uses getTotalAmountUploaded for the entire amount uploaded.
     * 
     * Implements the Uploader interface.
     */
    public long amountUploaded() {
        if (_stateNum == THEX_REQUEST) {
            return getAmountWritten();
        } else
            return amountRead;
    }

    /**
     * The total amount of bytes that this upload and all previous uploaders
     * have transferred on this socket in this file-exchange.
     * 
     * Implements the Uploader interface.
     */
    public long getTotalAmountUploaded() {
        if (_stateNum == THEX_REQUEST) {
            return getAmountWritten();
        } else {
            if (totalAmountReadBefore > 0)
                return totalAmountReadBefore + amountRead;
            else
                return totalAmountRead + amountRead;
        }
    }

    /**
     * Returns the <tt>FileDesc</tt> instance for this uploader.
     * 
     * @return the <tt>FileDesc</tt> instance for this uploader, or
     *         <tt>null</tt> if the <tt>FileDesc</tt> could not be assigned
     *         from the shared files
     */
    public FileDesc getFileDesc() {
        return fileDesc;
    }

    public void measureBandwidth() {
        long written = totalAmountRead + getAmountWritten();
        // FIXME type conversion
        session.measureBandwidth((int) written);
    }

    public abstract int getAmountWritten();

    public float getMeasuredBandwidth() throws InsufficientDataException {
        return session.getMeasuredBandwidth();
    }

    public float getAverageBandwidth() {
        return session.getAverageBandwidth();
    }

    public String getCustomIconDescriptor() {
        return null;
    }

    public UploadType getUploadType() {
        return _uploadType;
    }

    public void setUploadType(UploadType type) {
        _uploadType = type;
    }

    public void setBrowseEnabled(boolean browseEnabled) {
        this.browseEnabled = browseEnabled;
    }
    
    public void setChatEnabled(boolean chatEnabled) {
        this.chatEnabled = chatEnabled;
    }
    
    public void setNodeAddress(InetAddress nodeAddress) {
        this.nodeAddress = nodeAddress;
    }
    
    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }
    
    public void setTotalAmountReadBefore(int totalAmountReadBefore) {
        this.totalAmountReadBefore = totalAmountReadBefore;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setIndex(int index) {
        this.index = index;
    }
    
    // overrides Object.toString
    public String toString() {
        return "<" + getHost() + ":" + index + ">";
        // return "HTTPUploader:\r\n"+
        // "File Name: "+_fileName+"\r\n"+
        // "Host Name: "+_hostName+"\r\n"+
        // "Port: "+_port+"\r\n"+
        // "File Size: "+_fileSize+"\r\n"+
        // "State: "+_state;

    }
    
    public UploadSession getSession() {
        return session;
    }

}
