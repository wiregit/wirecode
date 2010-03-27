package org.limewire.core.impl;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentAlert;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentInfo;
import org.limewire.bittorrent.TorrentPeer;
import org.limewire.bittorrent.TorrentPiecesInfo;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.bittorrent.TorrentTracker;
import org.limewire.listener.EventListener;
import org.limewire.util.Base32;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 * A Torrent file implemented from xml data. This contains a very limited
 * subset of information about the Torrent.
 */
public class XMLTorrent implements Torrent {

    private final LimeXMLDocument xmlDocument;
    private final List<TorrentFileEntry> torrentFileEntries;
    private final List<URI> trackers;
    private final boolean isPrivate;
    private final String sha1;
    
    public XMLTorrent(LimeXMLDocument xmlDocument) {
        this.xmlDocument = xmlDocument;
        this.torrentFileEntries = parsePathEntries(xmlDocument.getValue(LimeXMLNames.TORRENT_FILE_PATHS), xmlDocument.getValue(LimeXMLNames.TORRENT_FILE_SIZES));
        String privateValue = xmlDocument.getValue(LimeXMLNames.TORRENT_PRIVATE);
        this.isPrivate = privateValue != null && Boolean.parseBoolean(privateValue);
        String hash = xmlDocument.getValue(LimeXMLNames.TORRENT_INFO_HASH);
        if(!StringUtils.isEmpty(hash)) {
            byte[] bytes = Base32.decode(hash);
            sha1 = StringUtils.toHexString(bytes);
        } else {
            sha1 = null;
        }
        
        String[] trackerStrings = xmlDocument.getValue(LimeXMLNames.TORRENT_TRACKERS).split(" ");
        
        trackers = new ArrayList<URI>(trackerStrings.length);
        for ( String trackerString : trackerStrings ) {
            try {
                trackers.add(new URI(trackerString));
            } catch (URISyntaxException e) {
                // Discard
            }
        }
    }
    
    @Override
    public void addListener(EventListener<TorrentEvent> listener) {}

    @Override
    public void addTracker(String url, int tier) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forceReannounce() {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getDownloadRate() {
        return 0;
    }

    @Override
    public File getFastResumeFile() {
        return null;
    }

    @Override
    public Lock getLock() {
        return null;
    }

    @Override
    public String getName() {
        return xmlDocument.getValue(LimeXMLNames.TORRENT_NAME);
    }

    @Override
    public int getNumConnections() {
        return 0;
    }

    @Override
    public int getNumPeers() {
        return 0;
    }

    @Override
    public int getNumUploads() {
        return 0;
    }

    @Override
    public TorrentPiecesInfo getPiecesInfo() {
        return null;
    }

    @Override
    public <T> T getProperty(String key, T defaultValue) {
        return null;
    }

    @Override
    public float getSeedRatio() {
        return 0;
    }

    @Override
    public String getSha1() {
        return sha1;
    }

    @Override
    public long getStartTime() {
        return -1;
    }

    @Override
    public TorrentStatus getStatus() {
        return null;
    }

    @Override
    public File getTorrentDataFile() {
        return null;
    }

    @Override
    public File getTorrentDataFile(TorrentFileEntry torrentFileEntry) {
        return null;
    }

    @Override
    public File getTorrentFile() {
        return null;
    }

    @Override
    public List<TorrentFileEntry> getTorrentFileEntries() {
        return torrentFileEntries;
    }

    @Override
    public TorrentInfo getTorrentInfo() {
        return null;
    }

    @Override
    public List<TorrentPeer> getTorrentPeers() {
        return Collections.emptyList();
    }

    @Override
    public long getTotalUploaded() {
        return 0;
    }

    @Override
    public List<URI> getTrackerURIS() {
        return trackers;
    }

    @Override
    public List<TorrentTracker> getTrackers() {
        return null;
    }

    @Override
    public float getUploadRate() {
        return 0;
    }

    @Override
    public void handleFastResumeAlert(TorrentAlert alert) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasMetaData() {
        return true;
    }

    @Override
    public boolean isAutoManaged() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public boolean isPrivate() {
        return isPrivate;
    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void moveTorrent(File directory) {
        throw new UnsupportedOperationException(); 
    }

    @Override
    public void pause() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeListener(EventListener<TorrentEvent> listener) {
        return false;
    }

    @Override
    public void removeTracker(String url, int tier) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resume() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveFastResumeData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void scrapeTracker() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAutoManaged(boolean autoManaged) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFastResumeFile(File fastResumeFile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProperty(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTorrenFileEntryPriority(TorrentFileEntry torrentFileEntry, int priority) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTorrentFile(File torrentFile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateStatus(TorrentStatus torrentStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEditable() {
        return false;
    }
    

    @Override
    public int getMaxDownloadBandwidth() {
        return -1;
    }

    @Override
    public int getMaxUploadBandwidth() {
        return -1;
    }

    @Override
    public void setMaxDownloadBandwidth(int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMaxUploadBandwidth(int value) {
        throw new UnsupportedOperationException();
    }
    
    private List<TorrentFileEntry> parsePathEntries(String encodedPath, String encodedSizes) {
        String[] paths = encodedPath.split("//");
        String[] sizes = encodedSizes.split(" ");
        if (paths.length != sizes.length) {
            return Collections.emptyList();
        }
        List<TorrentFileEntry> entries = new ArrayList<TorrentFileEntry>(paths.length);
        for (int i = 0; i < paths.length; i++) {
            try {
                entries.add(new LimeXMLTorrentFileEntry(paths[i].substring(1), Long.parseLong(sizes[i]), i));
            } catch (NumberFormatException nfe){
                return Collections.emptyList();
            }
        }
        return Collections.unmodifiableList(entries);
    }
    
    private static class LimeXMLTorrentFileEntry implements TorrentFileEntry {

        private final String path;
        private final long size;
        private final int index;

        public LimeXMLTorrentFileEntry(String path, long size, int index) {
            this.path = path;
            this.size = size;
            this.index = index;
        }
        
        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public float getProgress() {
            return 0;
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public long getTotalDone() {
            return 0;
        }
    }
}
