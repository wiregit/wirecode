package org.limewire.ui.swing.browser;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpException;
import org.limewire.core.api.download.SaveLocationException;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.mozIStorageConnection;
import org.mozilla.interfaces.nsICancelable;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsIDownloadProgressListener;
import org.mozilla.interfaces.nsIFile;
import org.mozilla.interfaces.nsILocalFile;
import org.mozilla.interfaces.nsIMIMEInfo;
import org.mozilla.interfaces.nsISimpleEnumerator;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIURI;
import org.mozilla.xpcom.Mozilla;

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

        URL url = null;
        try {
            url = new URL(arg1.getAsciiSpec());
        } catch (MalformedURLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        String fileName = arg6.getLeafName();
        URN urn = null;
        long size = arg6.getFileSize();

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
        boolean overwrite = false;

        try {
            downloadServices.downloadFromStore(rfd, overwrite, saveDir, fileName);
        } catch (SaveLocationException e) {
            // yum
        }

        URI uri = null;
        File saveLocation = null;
        // downloadManager.download(uri, saveLocation);
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
        return null;
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

    private final class NoOpNSILocalFile implements nsILocalFile {
        @Override
        public void appendRelativePath(String relativeFilePath) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        @Override
        public long getDiskSpaceAvailable() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return 0;
        }

        @Override
        public boolean getFollowLinks() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return false;
        }

        @Override
        public String getPersistentDescriptor() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return null;
        }

        @Override
        public String getRelativeDescriptor(nsILocalFile fromFile) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return null;
        }

        @Override
        public void initWithFile(nsILocalFile file) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void initWithPath(String filePath) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void launch() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void reveal() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void setFollowLinks(boolean followLinks) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void setPersistentDescriptor(String persistentDescriptor) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void setRelativeDescriptor(nsILocalFile fromFile, String relativeDesc) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public nsIFile _clone() {
            return this;
        }

        @Override
        public boolean _equals(nsIFile inFile) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return false;
        }

        @Override
        public void append(String node) {
            // TODO Auto-generated method stub
            // throw new UnsupportedOperationException();
            String fileName = node;
        }

        @Override
        public boolean contains(nsIFile inFile, boolean recur) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return false;
        }

        @Override
        public void copyTo(nsIFile newParentDir, String newName) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void copyToFollowingLinks(nsIFile newParentDir, String newName) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void create(long type, long permissions) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void createUnique(long type, long permissions) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public nsISimpleEnumerator getDirectoryEntries() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return null;
        }

        @Override
        public long getFileSize() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return 0;
        }

        @Override
        public long getFileSizeOfLink() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return 0;
        }

        @Override
        public long getLastModifiedTime() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return 0;
        }

        @Override
        public long getLastModifiedTimeOfLink() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return 0;
        }

        @Override
        public String getLeafName() {
            return "blah";
        }

        @Override
        public nsIFile getParent() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return null;
        }

        @Override
        public String getPath() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return null;
        }

        @Override
        public long getPermissions() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return 0;
        }

        @Override
        public long getPermissionsOfLink() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return 0;
        }

        @Override
        public String getTarget() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return null;
        }

        @Override
        public boolean isDirectory() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return false;
        }

        @Override
        public boolean isExecutable() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return false;
        }

        @Override
        public boolean isFile() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return false;
        }

        @Override
        public boolean isHidden() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return false;
        }

        @Override
        public boolean isReadable() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return false;
        }

        @Override
        public boolean isSpecial() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return false;
        }

        @Override
        public boolean isSymlink() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return false;
        }

        @Override
        public boolean isWritable() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
            // return false;
        }

        @Override
        public void moveTo(nsIFile newParentDir, String newName) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void normalize() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void remove(boolean recursive) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void setFileSize(long fileSize) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void setLastModifiedTime(long lastModifiedTime) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void setLastModifiedTimeOfLink(long lastModifiedTimeOfLink) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void setLeafName(String leafName) {
            String leaf = leafName;

        }

        @Override
        public void setPermissions(long permissions) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public void setPermissionsOfLink(long permissionsOfLink) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();

        }

        @Override
        public nsISupports queryInterface(String aIID) {
            return null;
        }
    }
}
