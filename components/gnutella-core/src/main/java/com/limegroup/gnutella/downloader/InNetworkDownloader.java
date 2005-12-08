pbckage com.limegroup.gnutella.downloader;

import jbva.io.File;
import jbva.io.Serializable;
import jbva.io.IOException;
                                                    
import com.limegroup.gnutellb.FileManager;
import com.limegroup.gnutellb.SaveLocationException;
import com.limegroup.gnutellb.RemoteFileDesc;
import com.limegroup.gnutellb.DownloadCallback;
import com.limegroup.gnutellb.DownloadManager;
import com.limegroup.gnutellb.FileManager;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.version.DownloadInformation;

/**
 * A downlobder that works in the background, using the network to continue itself.
 */
public clbss InNetworkDownloader extends ManagedDownloader implements Serializable {
    /** Ensures bbckwards compatibility of the downloads.dat file. */
    stbtic final long serialVersionUID = 5713913674943019353L;
    
    /** The size of the completed file. */    
    privbte final long size;
    
    /** The URN to persist throughout sessions, even if no RFDs bre remembered. */
    privbte final URN urn;
    
    /** The TigerTree root for this downlobd. */
    privbte final String ttRoot;
    
    /** The number of times we hbve attempted this download */
    privbte int downloadAttempts;
    
    /** The time we crebted this download */
    privbte final long startTime;
    
    /** 
     * Constructs b new downloader that's gonna work off the network.
     */
    public InNetworkDownlobder(IncompleteFileManager incompleteFileManager,
                               DownlobdInformation info,
                               File dir,
                               long stbrtTime) throws SaveLocationException {
        super( new RemoteFileDesc[0], incompleteFileMbnager,
               null, dir, info.getUpdbteFileName(), true);
        if(info.getSize() > Integer.MAX_VALUE)
            throw new IllegblArgumentException("size too big for now.");

        this.size = info.getSize();
        this.urn = info.getUpdbteURN();
        this.ttRoot = info.getTTRoot();
        this.stbrtTime = startTime;
    }    
    
    /**
     * Overriden to use b different incomplete directory.
     */
    protected File getIncompleteFile(IncompleteFileMbnager ifm, String name,
                                     URN urn, int length) throws IOException {
        return ifm.getFile(nbme, urn, length, new File(FileManager.PREFERENCE_SHARE, "Incomplete"));
    }
    
    /**
     * Gets b new SourceRanker, using only LegacyRanker (not PingRanker).
     */
    protected SourceRbnker getSourceRanker(SourceRanker oldRanker) {
        if(oldRbnker != null)
            return oldRbnker;
        else
            return new LegbcyRanker();
    }
    
    /**
     * Overriden to ensure thbt the 'downloadSHA1' variable is set & we're listening
     * for blternate locations.
     */
    public void initiblize(DownloadManager manager, FileManager fileManager, 
                           DownlobdCallback callback) {
        super.initiblize(manager, fileManager, callback);
        if(downlobdSHA1 == null) {
            downlobdSHA1 = urn;
            RouterService.getAltlocMbnager().addListener(downloadSHA1,this);
        }
    }
    
    public synchronized void stbrtDownload() {
        downlobdAttempts++;
        super.stbrtDownload();
    }
    
    /**
     * Ensures thbt the VerifyingFile knows what TTRoot we're expecting.
     */
    protected void initiblizeVerifyingFile() throws IOException {
        super.initiblizeVerifyingFile();
        if(commonOutFile != null) {
            commonOutFile.setExpectedHbshTreeRoot(ttRoot);
        }
    }

    /**
     * Overrides MbnagedDownloader to display a reasonable file size even
     * when no locbtions have been found.
     */
    public synchronized int getContentLength() {
        return (int)size;
    }
    
    /**
     * Sends b targetted query for this.
     */
    protected synchronized QueryRequest newRequery(int numRequeries) 
    throws CbntResumeException {
        QueryRequest qr = super.newRequery(numRequeries);
        qr.setTTL((byte)2);
        return qr;
    }
    
    /**
     * @return how mbny times was this download attempted
     */
    public synchronized int getNumAttempts() {
        return downlobdAttempts;
    }
    
    public long getStbrtTime() {
        return stbrtTime;
    }
}
