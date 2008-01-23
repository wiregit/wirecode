package com.limegroup.gnutella.downloader.serial;

import com.limegroup.bittorrent.BTMetaInfo;

/**
 * Defines an interface from which BitTorrent downloads
 * can be described and recreated.
 */
public interface BTDownloadMemento extends DownloadMemento {

    void setBtMetaInfo(BTMetaInfo btMetaInfo);

    BTMetaInfo getBtMetaInfo();

}