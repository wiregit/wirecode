package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.limewire.core.api.download.SaveLocationException;

import com.limegroup.gnutella.downloader.CoreDownloader;

/**
 * Public interface for the facade that BitTorrent downloaders use to connect to
 * LimeWire's core download system.
 */
public interface BTDownloader extends CoreDownloader {

    void init(File torrent) throws IOException;

    File getCompleteFile();

    List<File> getIncompleteFiles();

    List<File> getCompleteFiles();

    File getIncompleteFile();
    
    void checkTargetLocation() throws SaveLocationException;
    
    void checkActiveAndWaiting() throws SaveLocationException;
}