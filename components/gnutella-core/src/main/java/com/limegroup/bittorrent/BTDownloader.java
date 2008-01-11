package com.limegroup.bittorrent;

import com.limegroup.gnutella.downloader.CoreDownloader;

public interface BTDownloader extends CoreDownloader {

    public void initBtMetaInfo(BTMetaInfo btMetaInfo);
}