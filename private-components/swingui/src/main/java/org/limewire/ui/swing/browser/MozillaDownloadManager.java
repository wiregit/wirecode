package org.limewire.ui.swing.browser;

import java.io.File;
import java.net.URI;

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
import com.limegroup.gnutella.DownloadManager;

public class MozillaDownloadManager extends NsISelfReferencingFactory implements nsIDownloadManager {

    public static final String NS_IDOWNLOADMANAGER_CID = "@mozilla.org/download-manager;1";

    private final DownloadManager downloadManager;

    public MozillaDownloadManager(DownloadManager downloadManager) {
        super(NS_IDOWNLOADMANAGER_IID, NS_IDOWNLOADMANAGER_CID);
        this.downloadManager = Objects.nonNull(downloadManager, "downloadManager");
    }

    @Override
    public nsIDownload addDownload(short arg0, nsIURI arg1, nsIURI arg2, String arg3,
            nsIMIMEInfo arg4, double arg5, nsILocalFile arg6, nsICancelable arg7) {
        System.out.println("addDownload");
        System.out.println(arg1.getAsciiHost());
        System.out.println(arg1.getAsciiSpec());
        System.out.println(arg1.getHost());
        System.out.println(arg1.getSpec());
        System.out.println(arg1.getHostPort());
        System.out.println(arg1.getOriginCharset());
        System.out.println(arg1.getUsername());
        System.out.println(arg1.getPassword());
        System.out.println(arg1.getPort());
        System.out.println(arg1.getPrePath());
        System.out.println(arg1.getScheme());
        System.out.println(arg1.getUserPass());
        System.out.println(arg1.getPath());

        URI uri = null;
        File saveLocation = null;
        //downloadManager.download(uri, saveLocation);
        System.out.println(arg2.getPath());
        nsIDownload download = new NoOpMozillaDownload();
        return download;
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
        throw new UnsupportedOperationException();
    }

    @Override
    public nsIDownload getDownload(long arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public nsILocalFile getUserDownloadsDirectory() {
        return null;
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
