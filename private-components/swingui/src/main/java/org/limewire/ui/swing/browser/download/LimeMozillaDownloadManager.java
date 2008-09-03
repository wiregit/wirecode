package org.limewire.ui.swing.browser.download;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpException;
import org.limewire.core.settings.MozillaSettings;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.mozilla.interfaces.mozIStorageConnection;
import org.mozilla.interfaces.nsICancelable;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsIDownloadProgressListener;
import org.mozilla.interfaces.nsILocalFile;
import org.mozilla.interfaces.nsIMIMEInfo;
import org.mozilla.interfaces.nsISimpleEnumerator;
import org.mozilla.interfaces.nsIURI;

import com.google.inject.internal.base.Objects;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;

/**
 * This class minimally overrides the MozillaDownloadManager in order to
 * intercept download calls.
 */
public class LimeMozillaDownloadManager extends LimeMozillaSingletonFactory implements nsIDownloadManager {

    private static final Log LOG = LogFactory.getLog(LimeMozillaDownloadManager.class);

    public static final String NS_IDOWNLOADMANAGER_CID = "@mozilla.org/download-manager;1";

    private final DownloadServices downloadServices;

    private final RemoteFileDescFactory remoteFileDescFactory;

    public LimeMozillaDownloadManager(DownloadServices downloadServices,
            RemoteFileDescFactory remoteFileDescFactory) {
        super(NS_IDOWNLOADMANAGER_IID, NS_IDOWNLOADMANAGER_CID);
        this.downloadServices = Objects.nonNull(downloadServices, "downloadServices");
        this.remoteFileDescFactory = remoteFileDescFactory;
    }

    @Override
    public nsIDownload addDownload(short downloadType, final nsIURI source, final nsIURI target,
            String displayName, nsIMIMEInfo mimeInfo, double startTime, nsILocalFile tempFile,
            nsICancelable cancelable) {
        LOG.tracef("");
        Thread work = new Thread(new Runnable() {
            @Override
            public void run() {
                URL url = null;
                URLConnection urlConnection = null;
                try {

                    url = new URL(source.getAsciiSpec());
                    LOG.debugf("Adding Mozilla Download: {0}", url.toString());

                    String fileName = new File(target.getPath()).getName();
                    LOG.debugf("Mozilla File Name: {0}", fileName);

                    urlConnection = url.openConnection();
                    long size = urlConnection.getContentLength();
                    LOG.debugf("Mozilla Download Size: {0}", size);

                    URN urn = null;

                    RemoteFileDesc rfd = null;
                    rfd = remoteFileDescFactory.createUrlRemoteFileDesc(url, fileName, urn, size);
                    File saveDir = new File(MozillaSettings.DOWNLOAD_DIR.getValue().getAbsolutePath());

                    LOG.debugf("Mozilla Download Save Directory: {0}", saveDir.toString());

                    saveDir.mkdirs();
                    boolean overwrite = true;
                    LOG.debugf("Mozilla Download Starting");

                    // TODO instead of starting download, we will want to
                    // integrate with the file dialog for the new UI
                    downloadServices.downloadFromStore(rfd, overwrite, saveDir, fileName);
                } catch (IOException e) {
                    LOG.error("error adding download: " + source.getSpec(), e);
                } catch (URISyntaxException e) {
                    LOG.error("error adding download: " + source.getSpec(), e);
                } catch (HttpException e) {
                    LOG.error("error adding download: " + source.getSpec(), e);
                } catch (InterruptedException e) {
                    LOG.error("error adding download: " + source.getSpec(), e);
                } finally {
                    if (urlConnection != null) {
                        try {
                            if (urlConnection.getInputStream() != null) {
                                urlConnection.getInputStream().close();
                            }
                        } catch (IOException ignored) {
                        }
                        try {
                            if (urlConnection.getOutputStream() != null) {
                                urlConnection.getOutputStream().close();
                            }
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        });

        work.start();

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
