package com.limegroup.bittorrent.metadata;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.limewire.service.ErrorService;
import org.limewire.util.NameValue;
import org.limewire.util.StringUtils;

import com.limegroup.bittorrent.BTData;
import com.limegroup.bittorrent.BTData.BTFileData;
import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class TorrentMetaData implements MetaData {

    public static final String TORRENT_SCHEMA = "http://www.limewire.com/schemas/torrent.xsd";

    private final BTData data;

    public TorrentMetaData(BTData data) {
        this.data = data;
    }

    @Override
    public String getSchemaURI() {
        return TORRENT_SCHEMA;
    }

    @Override
    public void populate(LimeXMLDocument doc) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public List<NameValue<String>> toNameValueList() {
        NameValueListBuilder builder = new NameValueListBuilder();
        try {
            builder.add("info_hash", new String(data.getInfoHash(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            ErrorService.error(e);
        }
        builder.add("announce", data.getAnnounce());
        builder.add("length", data.getLength());
        builder.add("name", data.getName());
        builder.add("piecelength", data.getPieceLength());
        builder.add("private", Boolean.toString(data.isPrivate()));

        String uris = StringUtils.explode(data.getWebSeeds(), "\t");
        if (uris.length() > 0) {
            builder.add("webseeds", uris);
        }
        
        List<BTFileData> files = data.getFiles();
        List<String> filePaths = new ArrayList<String>(files.size());
        List<Long> fileLengths = new ArrayList<Long>(files.size());
        for (BTFileData file : files) {
            filePaths.add(file.getPath());
            fileLengths.add(file.getLength());
        }
        builder.add("filepaths", StringUtils.explode(filePaths, "\t"));
        builder.add("filelenghts", StringUtils.explode(fileLengths, "\t"));

        return builder.toList();
    }

    private static class NameValueListBuilder {

        private List<NameValue<String>> values = new ArrayList<NameValue<String>>();

        public void add(String name, String value) {
            values.add(new NameValue<String>(name, value));
        }

        public void add(String name, URI uri) {
            values.add(new NameValue<String>(name, uri.toASCIIString()));
        }

        public void add(String name, long value) {
            values.add(new NameValue<String>(name, Long.toString(value)));
        }

        List<NameValue<String>> toList() {
            return values;
        }
    }

}
