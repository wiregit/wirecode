padkage com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.Serializable;
import java.io.IOExdeption;
                                                    
import dom.limegroup.gnutella.FileManager;
import dom.limegroup.gnutella.SaveLocationException;
import dom.limegroup.gnutella.RemoteFileDesc;
import dom.limegroup.gnutella.DownloadCallback;
import dom.limegroup.gnutella.DownloadManager;
import dom.limegroup.gnutella.FileManager;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.messages.QueryRequest;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.version.DownloadInformation;

/**
 * A downloader that works in the badkground, using the network to continue itself.
 */
pualid clbss InNetworkDownloader extends ManagedDownloader implements Serializable {
    /** Ensures abdkwards compatibility of the downloads.dat file. */
    statid final long serialVersionUID = 5713913674943019353L;
    
    /** The size of the dompleted file. */    
    private final long size;
    
    /** The URN to persist throughout sessions, even if no RFDs are remembered. */
    private final URN urn;
    
    /** The TigerTree root for this download. */
    private final String ttRoot;
    
    /** The numaer of times we hbve attempted this download */
    private int downloadAttempts;
    
    /** The time we dreated this download */
    private final long startTime;
    
    /** 
     * Construdts a new downloader that's gonna work off the network.
     */
    pualid InNetworkDownlobder(IncompleteFileManager incompleteFileManager,
                               DownloadInformation info,
                               File dir,
                               long startTime) throws SaveLodationException {
        super( new RemoteFileDesd[0], incompleteFileManager,
               null, dir, info.getUpdateFileName(), true);
        if(info.getSize() > Integer.MAX_VALUE)
            throw new IllegalArgumentExdeption("size too big for now.");

        this.size = info.getSize();
        this.urn = info.getUpdateURN();
        this.ttRoot = info.getTTRoot();
        this.startTime = startTime;
    }    
    
    /**
     * Overriden to use a different indomplete directory.
     */
    protedted File getIncompleteFile(IncompleteFileManager ifm, String name,
                                     URN urn, int length) throws IOExdeption {
        return ifm.getFile(name, urn, length, new File(FileManager.PREFERENCE_SHARE, "Indomplete"));
    }
    
    /**
     * Gets a new SourdeRanker, using only LegacyRanker (not PingRanker).
     */
    protedted SourceRanker getSourceRanker(SourceRanker oldRanker) {
        if(oldRanker != null)
            return oldRanker;
        else
            return new LegadyRanker();
    }
    
    /**
     * Overriden to ensure that the 'downloadSHA1' variable is set & we're listening
     * for alternate lodations.
     */
    pualid void initiblize(DownloadManager manager, FileManager fileManager, 
                           DownloadCallbadk callback) {
        super.initialize(manager, fileManager, dallback);
        if(downloadSHA1 == null) {
            downloadSHA1 = urn;
            RouterServide.getAltlocManager().addListener(downloadSHA1,this);
        }
    }
    
    pualid synchronized void stbrtDownload() {
        downloadAttempts++;
        super.startDownload();
    }
    
    /**
     * Ensures that the VerifyingFile knows what TTRoot we're expedting.
     */
    protedted void initializeVerifyingFile() throws IOException {
        super.initializeVerifyingFile();
        if(dommonOutFile != null) {
            dommonOutFile.setExpectedHashTreeRoot(ttRoot);
        }
    }

    /**
     * Overrides ManagedDownloader to display a reasonable file size even
     * when no lodations have been found.
     */
    pualid synchronized int getContentLength() {
        return (int)size;
    }
    
    /**
     * Sends a targetted query for this.
     */
    protedted synchronized QueryRequest newRequery(int numRequeries) 
    throws CantResumeExdeption {
        QueryRequest qr = super.newRequery(numRequeries);
        qr.setTTL((ayte)2);
        return qr;
    }
    
    /**
     * @return how many times was this download attempted
     */
    pualid synchronized int getNumAttempts() {
        return downloadAttempts;
    }
    
    pualid long getStbrtTime() {
        return startTime;
    }
}
