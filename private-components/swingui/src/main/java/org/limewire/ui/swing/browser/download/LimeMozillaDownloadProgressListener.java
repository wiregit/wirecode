package org.limewire.ui.swing.browser.download;

//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.atomic.AtomicLong;
//
//import org.mozilla.interfaces.nsIDOMDocument;
//import org.mozilla.interfaces.nsIDownload;
//import org.mozilla.interfaces.nsIDownloadProgressListener;
//import org.mozilla.interfaces.nsIRequest;
//import org.mozilla.interfaces.nsISupports;
//import org.mozilla.interfaces.nsIWebProgress;
//import org.mozilla.xpcom.Mozilla;
//
//import com.google.inject.internal.base.Objects;
//import com.limegroup.bittorrent.SimpleBandwidthTracker;
//import com.limegroup.gnutella.InsufficientDataException;

/**
 * This class listens to a specific Mozilla download and tracks some statistics
 * for us.
 * 
 * TODO remove this class if we decide we will not need to use mozilla to download files.
 */
public class LimeMozillaDownloadProgressListener /*implements nsIDownloadProgressListener */{
//    private final long downloadId;
//
//    private final AtomicInteger state;
//
//    private final AtomicLong selfProgress;
//
//    private final AtomicLong totalProgress;
//
//    private final SimpleBandwidthTracker down;
//
//    public LimeMozillaDownloadProgressListener(nsIDownload download, short state) {
//        Objects.nonNull(download, "download");
//        this.downloadId = download.getId();
//        this.state = new AtomicInteger(state);
//        this.selfProgress = new AtomicLong();
//        this.totalProgress = new AtomicLong();
//        this.down = new SimpleBandwidthTracker();
//    }
//
//    @Override
//    public void onDownloadStateChange(short state, nsIDownload download) {
//        if (this.downloadId == download.getId()) {
//            // this is my download
//            this.state.set(state);
//        }
//    }
//
//    @Override
//    public void onProgressChange(nsIWebProgress webProgress, nsIRequest request,
//            long curSelfProgress, long maxSelfProgress, long curTotalProgress,
//            long maxTotalProgress, nsIDownload download) {
//
//        if (this.downloadId == download.getId()) {
//            // this is my download
//            long diff = curTotalProgress - totalProgress.longValue();
//            down.count(diff);
//            selfProgress.set(curSelfProgress);
//            totalProgress.set(curTotalProgress);
//        }
//    }
//
//    @Override
//    public void onSecurityChange(nsIWebProgress webProgress, nsIRequest request, long state,
//            nsIDownload download) {
//        // don't care about this event.
//    }
//
//    @Override
//    public void onStateChange(nsIWebProgress webProgress, nsIRequest request, long stateFlags,
//            long status, nsIDownload download) {
//        // no longer used by mozilla api
//    }
//
//    @Override
//    public void setDocument(nsIDOMDocument document) {
//        // no mozilla window to use
//    }
//
//    @Override
//    public nsIDOMDocument getDocument() {
//        // no mozilla window to use
//        return null;
//    }
//
//    @Override
//    public nsISupports queryInterface(String uuid) {
//        return Mozilla.queryInterface(this, uuid);
//    }
//
//    public long getDownloadId() {
//        return downloadId;
//    }
//
//    public float getAverageBandwidth() {
//        return down.getAverageBandwidth();
//    }
//
//    public float getMeasuredBandwidth() {
//        try {
//            return down.getMeasuredBandwidth();
//        } catch (InsufficientDataException e) {
//            return 0;
//        }
//    }
//
//    public void measureBandwidth() {
//        down.measureBandwidth();
//    }
}