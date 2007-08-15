package com.limegroup.bittorrent;

public interface BTDownloaderFactory {

    public BTDownloader createBTDownloader(BTMetaInfo info);

}