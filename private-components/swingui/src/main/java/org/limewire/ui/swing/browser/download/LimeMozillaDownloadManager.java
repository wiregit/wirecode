package org.limewire.ui.swing.browser.download;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.service.ErrorService;
import org.mozilla.interfaces.mozIStorageConnection;
import org.mozilla.interfaces.nsICancelable;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsIDownloadProgressListener;
import org.mozilla.interfaces.nsILocalFile;
import org.mozilla.interfaces.nsIMIMEInfo;
import org.mozilla.interfaces.nsISimpleEnumerator;
import org.mozilla.interfaces.nsIURI;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.internal.base.Objects;

/**
 * This class minimally overrides the MozillaDownloadManager in order to
 * intercept download calls.
 */
@Singleton
public class LimeMozillaDownloadManager extends LimeMozillaSingletonFactory implements
        nsIDownloadManager {

    private static final Log LOG = LogFactory.getLog(LimeMozillaDownloadManager.class);

    public static final String NS_IDOWNLOADMANAGER_CID = "@mozilla.org/download-manager;1";

    private final DownloadListManager downloadListManager;

    @Inject
    public LimeMozillaDownloadManager(DownloadListManager downloadListManager) {
        super(NS_IDOWNLOADMANAGER_IID, NS_IDOWNLOADMANAGER_CID);
        this.downloadListManager = Objects.nonNull(downloadListManager, "downloadListManager");
    }

    @Override
    public nsIDownload addDownload(short downloadType, final nsIURI source, final nsIURI target,
            String displayName, nsIMIMEInfo mimeInfo, double startTime, nsILocalFile tempFile,
            nsICancelable cancelable) {
        LOG.tracef("addDownload");

        try {
            LOG.debugf("Adding Mozilla Download: {0}", source.getSpec());
            URI uri = new URI(source.getAsciiSpec());

            String fileName = new File(target.getPath()).getName();
            LOG.debugf("Mozilla File Name: {0}", fileName);

            downloadListManager.addDownload(uri, fileName);
        } catch (URISyntaxException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Error parsing uri for: " + source.getAsciiSpec(), e);
                // TODO should inform the user of the error somehow
            }
        }

        // required to at least return something
        nsIDownload download = new LimeNoOpMozillaDownload();
        return download;
    }

    @Override
    public nsILocalFile getUserDownloadsDirectory() {
        // just setting this to the temp directory.
        // We are overriding the download code with our own download manager
        return new LimeNoOpMozillaLocalFile(new File(System.getProperty("java.io.tmpdir")));
    }

    @Override
    public void addListener(nsIDownloadProgressListener arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelDownload(long arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cleanUp() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getActiveDownloadCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public nsISimpleEnumerator getActiveDownloads() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getCanCleanUp() {
        throw new UnsupportedOperationException();
    }

    @Override
    public mozIStorageConnection getDBConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public nsILocalFile getDefaultDownloadsDirectory() {
        return null;
    }

    @Override
    public nsIDownload getDownload(long arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pauseDownload(long arg0) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void removeDownload(long arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeListener(nsIDownloadProgressListener arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resumeDownload(long arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void retryDownload(long arg0) {
        throw new UnsupportedOperationException();
    }
}
