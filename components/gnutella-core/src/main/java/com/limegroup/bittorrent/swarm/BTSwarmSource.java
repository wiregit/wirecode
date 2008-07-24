package com.limegroup.bittorrent.swarm;

import java.net.URI;

import org.limewire.swarm.http.SourceImpl;

import com.limegroup.bittorrent.BTMetaInfo;

public class BTSwarmSource extends SourceImpl {

    public BTSwarmSource(BTMetaInfo metaInfo) {
        this(metaInfo, metaInfo.getWebSeeds()[0]);
    }

    public BTSwarmSource(BTMetaInfo metaInfo, URI uri) {
        super(uri, metaInfo.getFileSystem().getTotalSize());
    }
}
