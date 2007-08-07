package com.limegroup.bittorrent;

public interface BTContextFactory {

    public BTContext createBTContext(BTMetaInfo info);

}