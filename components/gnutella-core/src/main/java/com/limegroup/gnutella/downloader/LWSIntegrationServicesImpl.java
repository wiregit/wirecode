package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.service.ErrorService;
import org.limewire.util.Version;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.lws.server.LWSManager;
import com.limegroup.gnutella.lws.server.LWSManagerCommandResponseHandlerWithCallback;
import com.limegroup.gnutella.lws.server.LWSUtil;
import com.limegroup.gnutella.settings.LWSSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.EncodingUtils;
import com.limegroup.gnutella.util.LimeWireUtils;
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
        //          <ID> ' ' <percentage-downloaded> ':' <download-status> [ '|' <ID> ' ' <percentage-downloaded> ':' <download-status> ]
        //  This ID is the identity hash code
        //
        lwsManager.registerHandler("GetDownloadProgress", new LWSManagerCommandResponseHandlerWithCallback("GetDownloadProgress") {
            
            /** 
             * We maintain a collection of ever-active downloaders, so that when we iterate
             * over all the current downloaders, we know that if one appears in the ever-active
             * collection, but not the current we need to inspect further.
             * 
             * More specifically, that downloader could have completed normally or it could have 
             * been completed by being cancelled. If the downloader completed:
             *  - Normally: Remove it from the ever-active collection and pass back a progress of 1.0
             *  - By being cancelled: Pass back the String 'X' denoting it was cancelled.
             * 
             * The reason we need to do this is because, when the Store is on the download page and connected
             * to the Client, it will poll the Client for the progress of all active and waiting downloads.
             * In the case that a downloader is either (1) finished normally or (2) cancelled, and the Client polls
             * after this occurs, we don't know which occured.  Basically this allows the Store to
             * sync with the Client, so we know when a download actually completes. 
             * 
             * Furthermore, if the download is at 99% and then completes, and the Store polls after this occurs
             * the progress bars on the Store web site will remain at 99%, because we never passed back
             * notification that the download completed.
             */
            private final Map<String, AbstractDownloader> everActiveDownloaderIDs2Downloaders = new HashMap<String, AbstractDownloader>();            

            protected String handleRest(Map<String, String> args) {

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
                //
                // Remember the downloader that are actually active, so we see if there are any that once existed
                // but are completed and take the correct action.  See the comment for 'everActiveDownloaderIDs'
                // for why we have to do this
                //
                final Set<String> activeDownloaderIDS = new HashSet<String>();
                synchronized (lwsIntegrationServicesDelegate) {
                    for (AbstractDownloader d : lwsIntegrationServicesDelegate.getAllDownloaders()) {
                        if (d == null) continue;
                        String id = String.valueOf(System.identityHashCode(d));
                        everActiveDownloaderIDs2Downloaders.put(id, d);
                        activeDownloaderIDS.add(id);
                        urnLoop: for (String downloaderID : downloaderIDs) {
                            if (downloaderID.equals(id)) {
                                recordProgress(d, res, downloaderID);
                                break urnLoop;
                            }
                        }
                    }
                }
                //
                // Now check whether the list of ever-active downloaders contains a downloader
                // not in the current list
                //
                final Collection<String> idsToRemove = new ArrayList<String>();
                for (String downloaderID : everActiveDownloaderIDs2Downloaders.keySet()) {
                    AbstractDownloader d = everActiveDownloaderIDs2Downloaders.get(downloaderID);
                    if (!activeDownloaderIDS.contains(downloaderID)) {
                        //
                        // We must have removed one of the previous from the list, so record it
                        // once then remove it from this list so we don't put it on again -- because
                        // that isn't neeed
                        //
                        recordProgress(d, res, downloaderID);
                        idsToRemove.add(downloaderID);
                    }
                }
                for (String idToRemove: idsToRemove) {
                    everActiveDownloaderIDs2Downloaders.remove(idToRemove);                    
                }
                return res.toString(); 
            }  
            
            private void recordProgress(final AbstractDownloader d, final StringBuffer res, String id) {
                long read = d.getAmountRead();
                long total = d.getContentLength();
                String ratio = String.valueOf((float)read / (float)total);
                String status = downloadStatusToString(d.getState());
                res.append(id);
                res.append(" ");
                res.append(ratio);
                res.append(":");
                res.append(status);
                res.append("|");                
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
                    return idOfTheDownloader + " " + idOfTheProgressBarString.getValue();
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
        //
        // Add a handler for the LimeWire Store Server so that we can find the
        // version of the client running
        // INPUT
        //  --
        // OUTPUT
        //  <version>
        //
        lwsManager.registerHandler("GetVersion", new LWSManagerCommandResponseHandlerWithCallback("GetVersion") {

            @Override
            protected String handleRest(Map<String, String> args) {
                return LimeWireUtils.getLimeWireVersion();
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
    
    /**
     * Returns a printable version of a {@link DownloadStatus}.
     * 
     * @param s status in question
     * @return a printable version of a {@link DownloadStatus}.
     */
    private static String downloadStatusToString(DownloadStatus s) {
        if (s == DownloadStatus.INITIALIZING) {
            return "Initializing";
        }
        if (s == DownloadStatus.QUEUED) {
            return "Queued";
        }
        if (s == DownloadStatus.CONNECTING) {
            return "Connecting";
        }
        if (s == DownloadStatus.DOWNLOADING) {
            return "Downloading";
        }
        if (s == DownloadStatus.BUSY) {
            return "Busy";
        }
        if (s == DownloadStatus.COMPLETE) {
            return "Complete";
        }
        if (s == DownloadStatus.ABORTED) {
            return "Aborted";
        }
        if (s == DownloadStatus.GAVE_UP) {
            return "Gave up";
        }
        if (s == DownloadStatus.DISK_PROBLEM) {
            return "Disk problem";
        }
        if (s == DownloadStatus.WAITING_FOR_GNET_RESULTS) {
            return "Waiting for gnet results";
        }
        if (s == DownloadStatus.CORRUPT_FILE) {
            return "Corrupt_FILE";
        }
        if (s == DownloadStatus.REMOTE_QUEUED) {
            return "Remote_QUEUED";
        }
        if (s == DownloadStatus.HASHING) {
            return "Hashing";
        }
        if (s == DownloadStatus.SAVING) {
            return "Saving";
        }
        if (s == DownloadStatus.WAITING_FOR_USER) {
            return "Waiting for user";
        }
        if (s == DownloadStatus.WAITING_FOR_CONNECTIONS) {
            return "Waiting for connections";
        }
        if (s == DownloadStatus.ITERATIVE_GUESSING) {
            return "Iterative guessing";
        }
        if (s == DownloadStatus.QUERYING_DHT) {
            return "Querying DHT";
        }
        if (s == DownloadStatus.IDENTIFY_CORRUPTION) {
            return "Identify corruption";
        }
        if (s == DownloadStatus.RECOVERY_FAILED) {
            return "Recovery failed";
        }
        if (s == DownloadStatus.PAUSED) {
            return "Paused";
        }
        if (s == DownloadStatus.INVALID) {
            return "Invalid";
        }
        if (s == DownloadStatus.RESUMING) {
            return "Resuming";
        }
        if (s == DownloadStatus.FETCHING) {
            return "Fetching";
        }
        return null;
    }
}
