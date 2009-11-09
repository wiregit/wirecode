package org.limewire.core.impl.bittorrent;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.limewire.bittorrent.ProxySetting;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.bittorrent.TorrentIpFilter;

import com.google.inject.Inject;

public class MockTorrentManager implements TorrentManager {

    private final TorrentManagerSettings torrentSettings;
    
    @Inject
    public MockTorrentManager(@TorrentSettingsAnnotation TorrentManagerSettings torrentSettings) {
        this.torrentSettings = torrentSettings;
    }
    
    @Override
    public TorrentManagerSettings getTorrentManagerSettings() {
        return torrentSettings;
    }

    @Override
    public boolean isDownloadingTorrent(File torrentFile) {
        return false;
    }

    @Override
    public Torrent getTorrent(File torrentFile) {
        return null;
    }

    @Override
    public Torrent getTorrent(String sha1) {
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean addTorrent(Torrent torrent) {
        return false;
    }

    @Override
    public void removeTorrent(Torrent torrent) {
        
    }

    @Override
    public void setTorrentManagerSettings(TorrentManagerSettings settings) {
        
    }

    @Override
    public void initialize() {
        
    }

    @Override
    public void setIpFilter(TorrentIpFilter ipFilter) {
        
    }

    @Override
    public void start() {
        
    }

    @Override
    public void stop() {
        
    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public List<Torrent> getTorrents() {
        return Collections.emptyList();
    }

    @Override
    public Lock getLock() {
        return null;
    }

    @Override
    public boolean isDHTStarted() {
        return false;
    }

    @Override
    public void startDHT(File dhtStateFile) {
        
    }

    @Override
    public void stopDHT() {
        
    }

    @Override
    public void saveDHTState(File dhtStateFile) {
        
    }

    @Override
    public boolean isUPnPStarted() {
        return false;
    }

    @Override
    public void startUPnP() {
        
    }

    @Override
    public void stopUPnP() {
        
    }

    @Override
    public void setPeerProxy(ProxySetting proxy) {
        
    }

    @Override
    public void setDHTProxy(ProxySetting proxy) {
        
    }

    @Override
    public void setTrackerProxy(ProxySetting proxy) {
        
    }

    @Override
    public void setWebSeedProxy(ProxySetting proxy) {
        
    }
}
