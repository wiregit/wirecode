package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.service.ErrorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.lws.server.LWSManager;
import com.limegroup.gnutella.lws.server.LWSManagerCommandResponseHandlerWithCallback;
import com.limegroup.gnutella.lws.server.LWSUtil;
import com.limegroup.gnutella.settings.LWSSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.EncodingUtils;
import com.limegroup.gnutella.util.Tagged;
import com.limegroup.gnutella.util.URLDecoder;

@Singleton
public final class LWSIntegrationServicesImpl implements LWSIntegrationServices {
    
    private static final Log LOG = LogFactory.getLog(HTTPDownloader.class);
    
    private final LWSManager lwsManager;
    private final DownloadServices downloadServices;
    private final LWSIntegrationServicesDelegate lwsIntegrationServicesDelegate;
    
    @Inject
    public LWSIntegrationServicesImpl(LWSManager lwsManager, 
                                      DownloadServices downloadServices,
                                      LWSIntegrationServicesDelegate lwsIntegrationServicesDelegate) {
        this.lwsManager = lwsManager;
        this.downloadServices = downloadServices;
        this.lwsIntegrationServicesDelegate = lwsIntegrationServicesDelegate;
    }
    

    public void init() {
        //
        // Add a handler for the LimeWire Store Server so that
        // we can keep track of downloads that were made on the
        // DownloadMediator
        // 
        // INPUT
        //  urns - space-separated string of urns
        // OUTPUT
        //  ID of download - a string of form
        //          <ID> ' ' <percentage-downloaded> [ '|' <ID> ' ' <percentage-downloaded> ]
        //  This ID is the identity hash code
        //
        lwsManager.registerHandler("GetDownloadProgress", new LWSManagerCommandResponseHandlerWithCallback("GetDownloadProgress") {

            protected String handleRest(Map<String, String> args) {
                        
                // TODO: These aren't URNs, they are identity hash codes
                //       But I don't really care right
                Tagged<String> idsString = LWSUtil.getArg(args, "urns", "GetDownloadProgress");
                if (!idsString.isValid()) return idsString.getValue();
                //
                // Return a string mapping urns to download percentages
                //
                StringBuffer res = new StringBuffer();
                String decodedIDs = null;
                try {
                    decodedIDs = URLDecoder.decode(idsString.getValue());
                } catch (IOException e) {
                    return "invalid.ids";
                }
                String[] downloaderIDs = decodedIDs.split(" ");
                synchronized (lwsIntegrationServicesDelegate) {
                    for (AbstractDownloader d : lwsIntegrationServicesDelegate.getAllDownloaders()) {
                        if (d == null) continue;
                        urnLoop: for (String downloaderID : downloaderIDs) {
                            String id = String.valueOf(System.identityHashCode(d));
                            if (downloaderID.equals(id)) {
                                long read = d.getAmountRead();
                                long total = d.getContentLength();
                                String ratio = String.valueOf((float)read / (float)total);
                                res.append(downloaderID);
                                res.append(" ");
                                res.append(ratio);
                                res.append("|");
                                break urnLoop;
                            }
                        }
                    }
                }
                return res.toString(); 
            }            
        }); 
        //
        // Add a handler for the LimeWire Store Server so that
        // we can download songs from The Store
        // INPUT
        //  url - to download
        //  file - name of file (optional)
        //  id - id of progress bar to update on the way back
        //  length - length of the track (optional)
        // OUTPUT
        //  URN - of downloader for keeping track of progress
        //   -or-
        //  timeout - if we timeout
        //
        lwsManager.registerHandler("Download", new LWSManagerCommandResponseHandlerWithCallback("Download") {

            protected String handleRest(Map<String, String> args) {
                //
                // The relative URL
                //
                Tagged<String> urlString = LWSUtil.getArg(args, "url", "downloading");
                if (!urlString.isValid()) return urlString.getValue();
                //
                // The file name.  If this isn't given (mainly for testing), we'll let it
                // go, but note that it was missing
                //
                Tagged<String> fileString = LWSUtil.getArg(args, "file", "downloading");
                if (!fileString.isValid()) LOG.info("no file name given to downloader...");
                //
                // The id of the tag we want to associate with the URN we return
                // 
                Tagged<String> idOfTheProgressBarString = LWSUtil.getArg(args, "id", "downloading");
                if (!idOfTheProgressBarString.isValid()) return idOfTheProgressBarString.getValue(); 
                //
                // The length of the URL (optional)
                // 
                Tagged<String> lengthString = LWSUtil.getArg(args, "length", "downloading");
                long length = -1;
                if (lengthString.isValid()) {
                    try {
                        length = Long.parseLong(lengthString.getValue());
                    } catch (NumberFormatException e) { /* ignore */ }
                } 
                //
                // We don't want to pass in a full URL and download it, so have
                // the remote setting LWSSettings.LWS_DOWNLOAD_PREFIX specifying
                // the entire prefix of where we're getting the file from and construct
                // the full URL from that
                //
                String baseDir = "http://" + LWSSettings.LWS_DOWNLOAD_PREFIX.getValue();
                String fileName = fileString.getValue();
                if (fileName == null) {
                    fileName = fileNameFromURL(urlString.getValue());
                }
                String urlStr = baseDir + urlString.getValue();
                try {
                    //
                    // This will need NO url encoding, and will contain ?'s and &'s
                    // which we want to keep.  So for testing, we can only pass in
                    // URLs that don't contain spaces
                    //
                    URL url = new URL(urlStr); 
                    System.out.println("urlStr:" + urlStr);
                    System.out.println("fileName:" + fileName);
                    System.out.println("idOfTheProgressBarString:" + idOfTheProgressBarString.getValue());
                    RemoteFileDesc rfd = StoreDownloader.createRemoteFileDesc(url, 
                            fileName, null, length); // this make the size looked up
                    //
                    // We'll associate the identity hash code of the downloader
                    // with this file so that the web page can keep track
                    // of this downloader w.r.t this file
                    //
                    File saveDir = SharingSettings.getSaveLWSDirectory();  
                    Downloader d = downloadServices.downloadFromStore(rfd, true, saveDir, fileName);
                    long idOfTheDownloader = System.identityHashCode(d);
                    String result = idOfTheDownloader + " " + idOfTheProgressBarString.getValue();
                    System.out.println("result:"+result);
                    return result;
                } catch (IOException e) {
                    ErrorService.error(e, "Invalid URL: " + urlStr);
                }

                return "invalid.download";
            }
            
            private String fileNameFromURL(String urlString) {
                int ilast = urlString.lastIndexOf("/");
                if (ilast == -1) {
                    ilast = urlString.lastIndexOf("\\");                    
                }
                return urlString.substring(ilast+1);
            }            
        });
        //
        // Add a handler for the LimeWire Store Server so that
        // we can download songs from The Store
        // INPUT
        //  id - to pause
        // OUTPUT
        //  OK
        //
        lwsManager.registerHandler("PauseDownload", new LWSManagerCommandResponseForDownloading("PauseDownload", lwsIntegrationServicesDelegate) {
            @Override
            protected void takeAction(Downloader d) {
                d.pause();
            }          
        });
        //
        // Add a handler for the LimeWire Store Server so that
        // we can download songs from The Store
        // INPUT
        //  id - to pause
        // OUTPUT
        //  OK
        //
        lwsManager.registerHandler("StopDownload", new LWSManagerCommandResponseForDownloading("StopDownload", lwsIntegrationServicesDelegate) {
            @Override
            protected void takeAction(Downloader d) {
                d.stop();
            }              
        }); 
        //
        // Add a handler for the LimeWire Store Server so that
        // we can download songs from The Store
        // INPUT
        //  id - to pause
        // OUTPUT
        //  OK
        //
        lwsManager.registerHandler("ResumeDownload", new LWSManagerCommandResponseForDownloading("ResumeDownload", lwsIntegrationServicesDelegate) {
            @Override
            protected void takeAction(Downloader d) {
                d.resume();
            }           
        });          
    }
    
    /**
     * A class to find a downloader, given an identity hashcode and take an action.
     */
    private abstract class LWSManagerCommandResponseForDownloading extends LWSManagerCommandResponseHandlerWithCallback {
        
        private final LWSIntegrationServicesDelegate del;
        
        LWSManagerCommandResponseForDownloading(String name, LWSIntegrationServicesDelegate del) {
            super(name);
            this.del = del;
        }
        
        protected abstract void takeAction(Downloader d);
        
        protected final String handleRest(Map<String, String> args) {
            //
            // The id of the downloader we want to pause
            // 
            Tagged<String> idOfTheDownloader = LWSUtil.getArg(args, "id", "downloading");
            if (!idOfTheDownloader.isValid()) return idOfTheDownloader.getValue();
            final String id = idOfTheDownloader.getValue();
            //
            // Find the downloader, by System.identityHashCode()
            //
            for (Downloader downloader : del.getAllDownloaders()) {
                if (String.valueOf(System.identityHashCode(downloader)).equals(id)) {
                    takeAction(downloader);
                    break;
                }
            }
            //
            // The response don't matter
            //
            return "OK";
        }         
    }     
}
