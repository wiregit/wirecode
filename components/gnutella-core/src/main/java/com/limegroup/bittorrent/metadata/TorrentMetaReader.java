package com.limegroup.bittorrent.metadata;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.limewire.util.FileUtils;
import org.limewire.util.NameValue;

import com.limegroup.bittorrent.BTDataImpl;
import com.limegroup.bittorrent.ValueException;
import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.metadata.MetaReader;

public class TorrentMetaReader implements MetaReader {

    private final TorrentMetaData torrentMetaData;

    public TorrentMetaReader(File file) throws IOException {
        byte[] contents = FileUtils.readFileFully(file);
        Object obj = Token.parse(contents);
        if (!(obj instanceof Map)) {
            throw new ValueException("expected map");
        }
        torrentMetaData = new TorrentMetaData(new BTDataImpl((Map)obj));
    }

    @Override
    public MetaData getMetaData() {
        return torrentMetaData;
    }

    @Override
    public String getSchemaURI() {
        return TorrentMetaData.TORRENT_SCHEMA;
    }

    @Override
    public List<NameValue<String>> toNameValueList() {
        return torrentMetaData.toNameValueList();
    }

}
