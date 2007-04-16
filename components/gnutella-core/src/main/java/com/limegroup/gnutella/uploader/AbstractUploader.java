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

    /** The number of bytes that were transferred in previous sessions. */
    private long totalAmountUploadedBefore;

    /**
     * The number of bytes transfered by all requests represented by this
     * Uploader.
     */
    private long totalAmountUploaded;

    /** The number of bytes transfered for the current request. */
    private long amountUploaded;

    private boolean ignoreTotalAmountUploaded;

    private long fileSize;

    private int index;

    private String userAgent;

    private final String filename;

    private int state = CONNECTING;

    private int lastTransferState;

    private boolean firstReply;

    private boolean chatEnabled;

    private boolean browseEnabled;

    /**
     * True if this is a forcibly shared network file.
     */
    private boolean forcedShare = false;

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
    private UploadType uploadType;

    public AbstractUploader(String fileName, UploadSession session) {
        this.session = session;
        this.filename = fileName;

        // XXX it is really bad to call this public method from the constructor
        reinitialize();

        firstReply = true;
    }

    /**
     * Reinitializes this uploader for a new request method.
     * 
     * @param method the HTTPRequestMethod to change to.
     * @param params the parameter list to change to.
     */
    public void reinitialize() {
        state = CONNECTING;
        nodePort = 0;
        totalAmountUploadedBefore = 0;
        if (!ignoreTotalAmountUploaded) {
            totalAmountUploaded += amountUploaded;
        }
        ignoreTotalAmountUploaded = false;
        amountUploaded = 0;
        firstReply = false;
    }

    /**
     * Sets the FileDesc for this HTTPUploader to use.
     * 
     * @param fd the <tt>FileDesc</tt> to use
     * @throws IOException if the file cannot be read from the disk.
     */
    public void setFileDesc(FileDesc fd) throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("Setting file description for " + this + ": " + fd);
        fileDesc = fd;
        forcedShare = FileManager.isForcedShare(fd);
        priorityShare = FileManager.isApplicationSpecialShare(fd.getFile());
        setFileSize(fd.getFileSize());
    }

    public void setState(int state) {
        this.lastTransferState = state;
        this.state = state;
    }

    /**
     * Returns the queued position if queued.
     */
    public int getQueuePosition() {
        if (lastTransferState != QUEUED || state == INTERRUPTED)
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
        addAmountUploaded((int) (amount - amountUploaded));
    }

    public void addAmountUploaded(int written) {
        if (written > 0) {
            if (isForcedShare())
                BandwidthStat.HTTP_BODY_UPSTREAM_INNETWORK_BANDWIDTH
                        .addData(written);
            else
                BandwidthStat.HTTP_BODY_UPSTREAM_BANDWIDTH.addData(written);
        }
        amountUploaded += written;
    }

    /**
     * Returns whether or not this upload is in what is considered an "inactive"
     * state, such as completed or aborted.
     * 
     * @return <tt>true</tt> if this upload is in an inactive state,
     *         <tt>false</tt> otherwise
     */
    public boolean isInactive() {
        switch (state) {
        case COMPLETE:
        case INTERRUPTED:
            return true;
        default:
            return false;
        }
    }

    // implements the Uploader interface
    public long getFileSize() {
        return fileSize;
    }

    // implements the Uploader interface
    public int getIndex() {
        return index;
    }

    // implements the Uploader interface
    public String getFileName() {
        return this.filename;
    }

    // implements the Uploader interface
    public int getState() {
        return state;
    }

    // implements the Uploader interface
    public int getLastTransferState() {
        return lastTransferState;
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
        return forcedShare;
    }

    // uploader with high priority?
    public boolean isPriorityShare() {
        return priorityShare;
    }

    protected boolean isFirstReply() {
        return firstReply;
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
        return amountUploaded;
    }

    /**
     * The total amount of bytes that this upload and all previous uploaders
     * have transferred on this socket in this file-exchange.
     * 
     * Implements the Uploader interface.
     */
    public long getTotalAmountUploaded() {
        if (ignoreTotalAmountUploaded)
            return amountUploaded;
        else if (totalAmountUploadedBefore > 0)
            return totalAmountUploadedBefore + amountUploaded;
        else
            return totalAmountUploaded + amountUploaded;
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
        // FIXME type conversion
        int written = (int) (totalAmountUploaded + amountUploaded);
        session.measureBandwidth(written);
    }

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
        return uploadType;
    }

    public void setUploadType(UploadType type) {
        uploadType = type;
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

    public void setTotalAmountUploadedBefore(int totalAmountReadBefore) {
        this.totalAmountUploadedBefore = totalAmountReadBefore;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String toString() {
        return getClass().getName() + "[host=" + getHost() + ",index=" + index
                + ",filename=" + filename + ",state=" + state
                + ",lastTransferState=" + lastTransferState + "]";
    }

    public UploadSession getSession() {
        return session;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public boolean getIgnoreTotalAmountUploaded() {
        return ignoreTotalAmountUploaded;
    }

    public void setIgnoreTotalAmountUploaded(boolean ignoreTotalAmountUploaded) {
        this.ignoreTotalAmountUploaded = ignoreTotalAmountUploaded;
    }

}
