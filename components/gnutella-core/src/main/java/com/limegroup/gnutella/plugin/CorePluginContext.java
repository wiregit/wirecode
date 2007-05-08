package com.limegroup.gnutella.plugin;

import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.dht.DHTManager;

public class CorePluginContext {
    
    public Acceptor getAcceptor() {
        return RouterService.getAcceptor();
    }
    
    public ConnectionManager getConnectionManager() {
        return RouterService.getConnectionManager();
    }
    
    public DownloadManager getDownloadManager() {
        return RouterService.getDownloadManager();
    }
    
    public UploadManager getUploadManager() {
        return RouterService.getUploadManager();
    }
    
    public DHTManager getDHTManager() {
        return RouterService.getDHTManager();
    }
    
    public TorrentManager getTorrentManager() {
        return RouterService.getTorrentManager();
    }
    
    public FileManager getFileManager() {
        return RouterService.getFileManager();
    }
}
