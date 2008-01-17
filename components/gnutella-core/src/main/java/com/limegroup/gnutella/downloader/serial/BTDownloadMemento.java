package com.limegroup.gnutella.downloader.serial;

import com.limegroup.bittorrent.BTMetaInfo;

public interface BTDownloadMemento extends DownloadMemento {

    void setBtMetaInfo(BTMetaInfo btMetaInfo);

    BTMetaInfo getBtMetaInfo();

}