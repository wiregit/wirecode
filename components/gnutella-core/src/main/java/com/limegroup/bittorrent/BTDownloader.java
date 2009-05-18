package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;

import com.limegroup.gnutella.downloader.CoreDownloader;

/**
 * Public interface for the facade that BitTorrent downloaders use to connect to
 * LimeWire's core download system.
 */
public interface BTDownloader extends CoreDownloader {

    void init(File torrent) throws IOException;

    File getIncompleteFile();
    
    File getTorrentFile();
    
    void register();
}