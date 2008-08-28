package org.limewire.ui.swing.browser;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpException;
import org.limewire.core.api.download.SaveLocationException;
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

public class MozillaDownloadManager extends NsISelfReferencingFactory implements nsIDownloadManager {

    public static final String NS_IDOWNLOADMANAGER_CID = "@mozilla.org/download-manager;1";

    private final DownloadServices downloadServices;

    private final RemoteFileDescFactory remoteFileDescFactory;

    public MozillaDownloadManager(DownloadServices downloadServices,
            RemoteFileDescFactory remoteFileDescFactory) {
        super(NS_IDOWNLOADMANAGER_IID, NS_IDOWNLOADMANAGER_CID);
        this.downloadServices = Objects.nonNull(downloadServices, "downloadServices");
        this.remoteFileDescFactory = remoteFileDescFactory;
    }

    @Override
    public nsIDownload addDownload(short downloadType, nsIURI source, nsIURI target, String displayName,
            nsIMIMEInfo mimeInfo, double startTime, nsILocalFile tempFile, nsICancelable cancelable) {
        System.out.println("addDownload");
        System.out.println(displayName);
        System.out.println(target.getSpec());
        try {
            System.out.println(tempFile.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(source.getAsciiHost());
        System.out.println(source.getAsciiSpec());
        System.out.println(source.getHost());
        System.out.println(source.getSpec());
        System.out.println(source.getHostPort());
        System.out.println(source.getOriginCharset());
        System.out.println(source.getUsername());
        System.out.println(source.getPassword());
        System.out.println(source.getPort());
        System.out.println(source.getPrePath());
        System.out.println(source.getScheme());
        System.out.println(source.getUserPass());
        System.out.println(source.getPath());

        URL url = null;
        try {
            url = new URL(source.getAsciiSpec());
        } catch (MalformedURLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        String fileName = new File(target.getPath()).getName();
        URN urn = null;
        long size = tempFile.getFileSize();
        System.out.println(size);
        RemoteFileDesc rfd = null;
        try {
            rfd = remoteFileDescFactory.createUrlRemoteFileDesc(url, fileName, urn, size);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (URISyntaxException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (HttpException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        File saveDir = new File("/home/pvertenten/Desktop/testmoz");
        saveDir.mkdirs();
        boolean overwrite = true;

        try {
            downloadServices.downloadFromStore(rfd, overwrite, saveDir, fileName);
        } catch (SaveLocationException e) {
            e.printStackTrace();
        }

        URI uri = null;
        File saveLocation = null;
        // downloadManager.download(uri, saveLocation);
        System.out.println(target.getPath());
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
        return null;
    }

    @Override
    public nsIDownload getDownload(long arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public nsILocalFile getUserDownloadsDirectory() {
        return new NoOpNSILocalFile(new File(System.getProperty("java.io.tmpdir")));
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
