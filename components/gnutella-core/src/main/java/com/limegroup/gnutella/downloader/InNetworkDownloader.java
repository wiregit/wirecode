package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.SharingUtils;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.version.DownloadInformation;

/**
 * A downloader that works in the background, using the network to continue itself.
 */
public class InNetworkDownloader extends ManagedDownloader implements Serializable {
    /** Ensures backwards compatibility of the downloads.dat file. */
    private static final long serialVersionUID = 5713913674943019353L;
    
    /** The size of the completed file. */    
    private final long size;
    
    /** The URN to persist throughout sessions, even if no RFDs are remembered. */
    private final URN urn;
    
    /** The TigerTree root for this download. */
    private final String ttRoot;
    
    /** The number of times we have attempted this download */
    private int downloadAttempts;
    
    /** The time we created this download */
    private final long startTime;
    
    /** 
     * Constructs a new downloader that's gonna work off the network.
     */
    InNetworkDownloader(IncompleteFileManager incompleteFileManager,
                               DownloadInformation info,
                               File dir,
                               long startTime, SaveLocationManager saveLocationManager) throws SaveLocationException {
        super( new RemoteFileDesc[0], incompleteFileManager,
               null, dir, info.getUpdateFileName(), true, saveLocationManager);
        // note: even though we support bigger files, this is a good sanity check
        if(info.getSize() > Integer.MAX_VALUE)
            throw new IllegalArgumentException("size too big for now.");

        this.size = info.getSize();
        this.urn = info.getUpdateURN();
        this.ttRoot = info.getTTRoot();
        this.startTime = startTime;
    }    
    
    /**
     * Overriden to use a different incomplete directory.
     */
    @Override
    protected File getIncompleteFile(IncompleteFileManager ifm, String name,
                                     URN urn, long length) throws IOException {
        return ifm.getFile(name, urn, length, new File(SharingUtils.PREFERENCE_SHARE, "Incomplete"));
    }
    
    /**
     * Gets a new SourceRanker, using only LegacyRanker (not PingRanker).
     */
    protected SourceRanker getSourceRanker(SourceRanker oldRanker) {
        if(oldRanker != null)
            return oldRanker;
        else
            return new LegacyRanker();
    }
    
    /**
     * Overriden to ensure that the 'downloadSHA1' variable is set & we're listening
     * for alternate locations.
     */
    public void initialize(DownloadReferences downloadReferences) {
        super.initialize(downloadReferences);
        if (downloadSHA1 == null) {
            downloadSHA1 = urn;
            // the listener is removed by ManagedDownloader.finished()
            altLocManager.addListener(downloadSHA1, this);
        }
    }
    
    public synchronized void startDownload() {
        downloadAttempts++;
        super.startDownload();
    }
    
    protected boolean shouldValidate(boolean deserialized) {
        return false;
    }
    
    /**
     * Ensures that the VerifyingFile knows what TTRoot we're expecting.
     */
    protected void initializeVerifyingFile() throws IOException {
        super.initializeVerifyingFile();
        if(commonOutFile != null) {
            commonOutFile.setExpectedHashTreeRoot(ttRoot);
        }
    }

    /**
     * Overrides ManagedDownloader to display a reasonable file size even
     * when no locations have been found.
     */
    public synchronized long getContentLength() {
        return size;
    }
    
    /**
     * Sends a targetted query for this.
     */
    protected synchronized QueryRequest newRequery(int numRequeries) 
    throws CantResumeException {
        QueryRequest qr = super.newRequery(numRequeries);
        qr.setTTL((byte)2);
        return qr;
    }
    
    /**
     * @return how many times was this download attempted
     */
    public synchronized int getNumAttempts() {
        return downloadAttempts;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.INNETWORK;
    }
}
