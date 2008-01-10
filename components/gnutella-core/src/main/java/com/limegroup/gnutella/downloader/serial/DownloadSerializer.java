package com.limegroup.gnutella.downloader.serial;

import java.util.List;

import org.limewire.collection.Range;

import com.limegroup.gnutella.downloader.AbstractDownloader;

/**
 * Allows all downloads to be written & read from disk.
 */
public interface DownloadSerializer {
    
    public List<SavedDownloadInfo> readFromDisk();
    
    public void writeToDisk(List<? extends SavedDownloadInfo> downloads);
    
    public class SavedDownloadInfo {
        private final AbstractDownloader downloader;
        private final List<Range> ranges;
        
        public SavedDownloadInfo(AbstractDownloader downloader, List<Range> ranges) {
            this.downloader = downloader;
            this.ranges = ranges;
        }

        public AbstractDownloader getDownloader() {
            return downloader;
        }

        public List<Range> getRanges() {
            return ranges;
        }
        
    }
    
}
