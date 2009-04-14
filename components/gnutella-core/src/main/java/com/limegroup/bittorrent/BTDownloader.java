package com.limegroup.bittorrent;

import java.io.File;
import java.util.List;

import com.limegroup.gnutella.downloader.CoreDownloader;

/**
 * Public interface for the facade that BitTorrent downloaders use to connect to
 * LimeWire's core download system.
 */
public interface BTDownloader extends CoreDownloader {

    void init(File torrent);

    File getCompleteFile();

    List<File> getIncompleteFiles();

    List<File> getFiles();
}