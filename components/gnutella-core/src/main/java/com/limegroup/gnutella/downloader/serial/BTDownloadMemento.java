package com.limegroup.gnutella.downloader.serial;


/**
 * Defines an interface from which BitTorrent downloads
 * can be described and recreated.
 */
public interface BTDownloadMemento extends DownloadMemento {

    void setBtMetaInfo(BTMetaInfoMemento btMetaInfo);

    BTMetaInfoMemento getBtMetaInfo();

}