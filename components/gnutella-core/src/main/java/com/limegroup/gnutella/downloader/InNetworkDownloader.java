package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.Serializable;
import java.io.IOException;

import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.version.UpdateInformation;

/**
 * A downloader that works in the background, using the network to continue itself.
 */
public class InNetworkDownloader extends ManagedDownloader implements Serializable {
    /** Ensures backwards compatibility of the downloads.dat file. */
    static final long serialVersionUID = 5713913674943019353L;
    
    /** The size of the completed file. */    
    private final long size;
    
    /** The URN to persist throughout sessions, even if no RFDs are remembered. */
    private final URN urn;
    
    /** The TigerTree root for this download. */
    private String ttRoot;

    /** 
     * Constructs a new downloader that's gonna work off the network.
     */
    public InNetworkDownloader(IncompleteFileManager incompleteFileManager,
                               UpdateInformation info,
                               File dir) throws SaveLocationException {
        super( new RemoteFileDesc[0], incompleteFileManager,
               null, dir, info.getUpdateFileName(), true);
        if(info.getSize() > Integer.MAX_VALUE)
            throw new IllegalArgumentException("size too big for now.");

        this.size = info.getSize();
        this.urn = info.getUpdateURN();
        this.ttRoot = info.getTTRoot();
    }
    
    /**
     * Overriden to ensure that the 'downloadSHA1' variable is set & we're listening
     * for alternate locations.
     */
    public void initialize(DownloadManager manager, FileManager fileManager, 
                           DownloadCallback callback) {
        super.initialize(manager, fileManager, callback);
        if(downloadSHA1 == null) {
            downloadSHA1 = urn;
            RouterService.getAltlocManager().addListener(downloadSHA1,this);
        }
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
    public synchronized int getContentLength() {
        return (int)size;
    }
    
    /**
     * Sends a targetted query for this.
     */
    protected synchronized QueryRequest newRequery(int numRequeries) {
        QueryRequest qr = newRequery(numRequeries);
        qr.setTTL((byte)2);
        return qr;
    }
}
