package com.limegroup.gnutella.downloader;

import java.util.Collection;

import com.limegroup.gnutella.RemoteFileDesc;

public interface MeshHandler {
    void informMesh(RemoteFileDesc rfd, boolean good);
    void addPossibleSources(Collection<? extends RemoteFileDesc> hosts);
}