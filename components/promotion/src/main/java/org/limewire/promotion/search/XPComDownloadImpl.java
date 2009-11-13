package org.limewire.promotion.search;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpException;
import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.settings.SharingSettings;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.IXPCOMError;
import org.mozilla.xpcom.XPCOMException;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;

public class XPComDownloadImpl implements XPComDownload {
    
    /** UUID for the component implementation.  This MUST be different than
     *  the interface IID.  
     */
    public static String CID = "{f083e1d0-62cf-45a2-93f5-926ba02aad73}";
    
    /** Unique identifier for the implementation. */
    public static String CONTRACT_ID = "@org.limewire/XPComDownload;1";
    private final CategoryManager categoryManager;
    private final RemoteFileDescFactory remoteFileDescFactory;
    private final DownloadServices downloadServices;

    public XPComDownloadImpl(CategoryManager categoryManager, RemoteFileDescFactory remoteFileDescFactory, DownloadServices downloadServices) {
        this.categoryManager = categoryManager;
        this.remoteFileDescFactory = remoteFileDescFactory;
        this.downloadServices = downloadServices;
    }

    @Override
    public void download(String fileName, String url, long size) {
        try {
            Category category = categoryManager.getCategoryForFilename(fileName);
            File saveDir = SharingSettings.getSaveDirectory(category);
            RemoteFileDesc rfd = remoteFileDescFactory.createUrlRemoteFileDesc(new URL(url), fileName, null, size);
            rfd.setHTTP11(false);
            // TODO check for already downloading file
            Downloader theDownloader = downloadServices.downloadFromStore(rfd, true, saveDir, fileName);
            //long idOfTheDownloader = System.identityHashCode(theDownloader);
            //downloaderIDs2progressBarIDs.put(String.valueOf(idOfTheDownloader), idOfTheProgressBarString.getValue());
            //return idOfTheDownloader + " " + idOfTheProgressBarString.getValue();
        } catch (IOException e) {
            // invalid url or other causes, fail silently
        } catch (HttpException e) {
            // invalid url or other causes, fail silently
        } catch (InterruptedException e) {
            // invalid url or other causes, fail silently
        } catch (URISyntaxException e) {
            // invalid url or other causes, fail silently
        }
    }

    @Override
    public nsISupports queryInterface(String uuid) {
        if (!uuid.equals(NS_ISUPPORTS_IID) && (!uuid.equals(XPCOMDOWNLOAD_IID))) {
            throw new XPCOMException(IXPCOMError.NS_ERROR_NOT_IMPLEMENTED);
        }
        return this;
    }
}
