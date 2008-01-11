package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.util.List;

import org.limewire.collection.Range;

import com.limegroup.gnutella.downloader.CoreDownloader;

/**
 * Allows all downloads to be written & read from disk.
 */
public interface DownloadSerializer {
    
    public List<SavedDownloadInfo> readFromDisk();
    
    public void writeToDisk(List<? extends SavedDownloadInfo> downloads);
    
    public class SavedDownloadInfo {
        private final CoreDownloader downloader;
        private final List<Range> ranges;
        private final File incompleteFile;
        
        public SavedDownloadInfo(CoreDownloader downloader, List<Range> ranges, File incompleteFile) {
            this.downloader = downloader;
            this.ranges = ranges;
            this.incompleteFile = incompleteFile;
        }

        public CoreDownloader getDownloader() {
            return downloader;
        }

        public List<Range> getRanges() {
            return ranges;
        }
        
        public File getIncompleteFile() {
            return incompleteFile;
        }
        
    }
    
}
