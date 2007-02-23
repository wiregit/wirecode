package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.URN;
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

    private int _totalAmountReadBefore;

    private int _totalAmountRead;

    private int _amountRead;

    // useful so we don't have to do _uploadEnd - _uploadBegin everywhere
    private int _amountRequested;

    private int _fileSize;

    private final int _index;

    private String _userAgent;

    private boolean _headersParsed;

    private final String _fileName;

    private int _stateNum = CONNECTING;

    private int _lastTransferStateNum;

    private boolean _firstReply = true;

    private boolean _chatEnabled;

    private boolean _browseEnabled;

    private boolean _supportsQueueing = false;

    /**
     * True if this is a forcibly shared network file.
     */
    private boolean _isForcedShare = false;

    /**
     * True if this is an uploader with high priority.
     */
    private boolean priorityShare = false;

    /**
     * The URN specified in the X-Gnutella-Content-URN header, if any.
     */
    private URN _requestedURN;

    /**
     * The descriptor for the file we're uploading.
     */
    private FileDesc _fileDesc;

    /**
     * Indicates that the client to which we are uploading is capable of
     * accepting Queryreplies in the response.
     */
    private boolean _clientAcceptsXGnutellaQueryreplies = false;

    /**
     * The address as described by the "X-Node" header.
     */
    private InetAddress _nodeAddress = null;

    /**
     * The port as described by the "X-Node" header.
     */
    private int _nodePort = -1;

    /** The upload type of this uploader. */
    private UploadType _uploadType;

    public AbstractUploader(String fileName, UploadSession session, int index) {
        this.session = session;
        _fileName = fileName;
        _index = index;
        _totalAmountRead = 0;
        _amountRead = 0;
        reinitialize();
    }

    /**
     * Reinitializes this uploader for a new request method.
     * 
     * @param method the HTTPRequestMethod to change to.
     * @param params the parameter list to change to.
     */
    public void reinitialize() {
        _amountRequested = 0;
        _headersParsed = false;
        _stateNum = CONNECTING;
        _nodePort = 0;
        _supportsQueueing = false;
        _requestedURN = null;
        _clientAcceptsXGnutellaQueryreplies = false;
        _totalAmountReadBefore = 0;
        _totalAmountRead += _amountRead;
        _amountRead = 0;

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
        _fileDesc = fd;
        _fileSize = (int) fd.getFileSize();

        _isForcedShare = FileManager.isForcedShare(_fileDesc);
        priorityShare = FileManager.isApplicationSpecialShare(_fileDesc
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
    void setAmountUploaded(int amount) {
        int newData = amount - _amountRead;
        if (newData > 0) {
            if (isForcedShare())
                BandwidthStat.HTTP_BODY_UPSTREAM_INNETWORK_BANDWIDTH
                        .addData(newData);
            else
                BandwidthStat.HTTP_BODY_UPSTREAM_BANDWIDTH.addData(newData);
        }
        _amountRead = amount;
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
            return _fileDesc.getHashTree().getOutputLength();
        else
            return _fileSize;
    }

    // implements the Uploader interface
    public long getAmountRequested() {
        if (_stateNum == THEX_REQUEST)
            return _fileDesc.getHashTree().getOutputLength();
        else
            return _amountRequested;
    }

    // implements the Uploader interface
    public int getIndex() {
        return _index;
    }

    // implements the Uploader interface
    public String getFileName() {
        return _fileName;
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
        return _chatEnabled;
    }

    // implements the Uploader interface
    public boolean isBrowseHostEnabled() {
        return _browseEnabled;
    }

    // implements the Uploader interface
    public int getGnutellaPort() {
        return _nodePort;
    }

    // implements the Uploader interface
    public String getUserAgent() {
        return _userAgent;
    }

    // implements the Uploader interface
    public boolean isHeaderParsed() {
        return _headersParsed;
    }

    // is a forced network share?
    public boolean isForcedShare() {
        return _isForcedShare;
    }

    // uploader with high priority?
    public boolean isPriorityShare() {
        return priorityShare;
    }

    public boolean supportsQueueing() {
        return _supportsQueueing && isValidQueueingAgent();
    }

    /**
     * Blocks certain vendors from being queued, because of buggy downloading
     * implementations on their side.
     */
    private boolean isValidQueueingAgent() {
        if (_userAgent == null)
            return true;

        return !_userAgent.startsWith("Morpheus 3.0.2");
    }

    protected boolean isFirstReply() {
        return _firstReply;
    }

    public InetAddress getNodeAddress() {
        return _nodeAddress;
    }

    public int getNodePort() {
        return _nodePort;
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
            return _amountRead;
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
            if (_totalAmountReadBefore > 0)
                return _totalAmountReadBefore + _amountRead;
            else
                return _totalAmountRead + _amountRead;
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
        return _fileDesc;
    }

    boolean getClientAcceptsXGnutellaQueryreplies() {
        return _clientAcceptsXGnutellaQueryreplies;
    }

    /**
     * Returns the content URN that the client asked for.
     */
    public URN getRequestedURN() {
        return _requestedURN;
    }

    public void measureBandwidth() {
        int written = _totalAmountRead + getAmountWritten();
        session.measureBandwidth(written);
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

    // overrides Object.toString
    public String toString() {
        return "<" + getHost() + ":" + _index + ">";
        // return "HTTPUploader:\r\n"+
        // "File Name: "+_fileName+"\r\n"+
        // "Host Name: "+_hostName+"\r\n"+
        // "Port: "+_port+"\r\n"+
        // "File Size: "+_fileSize+"\r\n"+
        // "State: "+_state;

    }
}
