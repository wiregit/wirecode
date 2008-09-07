package com.limegroup.bittorrent.metadata;

import java.io.File;
import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.metadata.MetaDataFactory;
import com.limegroup.gnutella.metadata.MetaReader;
import com.limegroup.gnutella.metadata.MetaReaderFactory;

@Singleton
public class TorrentMetaReaderFactory implements MetaReaderFactory {

    @Override
    public MetaReader createReader(File file) throws IOException {
        return new TorrentMetaReader(file);
    }

    @Inject
    public void register(MetaDataFactory factory) {
        factory.registerReaderFactory(this, "torrent");
    }
}
