package com.limegroup.bittorrent.swarm;

import java.net.URI;

import org.limewire.swarm.http.SwarmHttpSource;

import com.limegroup.bittorrent.BTMetaInfo;

public class BTSwarmHttpSource extends SwarmHttpSource {

    public BTSwarmHttpSource(BTMetaInfo metaInfo) {
        this(metaInfo, metaInfo.getWebSeeds()[0]);
    }

    public BTSwarmHttpSource(BTMetaInfo metaInfo, URI uri) {
        super(uri, metaInfo.getFileSystem().getTotalSize());
    }
}
