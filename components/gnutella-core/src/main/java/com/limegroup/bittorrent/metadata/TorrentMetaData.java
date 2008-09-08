package com.limegroup.bittorrent.metadata;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.http.URIUtils;
import org.limewire.util.NameValue;
import org.limewire.util.StringUtils;

import com.limegroup.bittorrent.BTData;
import com.limegroup.bittorrent.BTData.BTFileData;
import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class TorrentMetaData implements MetaData {

    public static final String TORRENT_SCHEMA = "http://www.limewire.com/schemas/torrent.xsd";

    private final BTData data;

    private List<NameValue<String>> nameValues;

    public TorrentMetaData(BTData data) throws IOException {
        this.data = data;
        nameValues = Collections.unmodifiableList(buildNameValueList());
    }

    @Override
    public String getSchemaURI() {
        return TORRENT_SCHEMA;
    }

    @Override
    public void populate(LimeXMLDocument doc) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    private List<NameValue<String>> buildNameValueList() throws IOException {
        NameValueListBuilder builder = new NameValueListBuilder();
        builder.add("infohash", StringUtils.toUTF8String(data.getInfoHash()));
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
            try {
                filePaths.add(URIUtils.toURI("file:///" + file.getPath()).toASCIIString());
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
            fileLengths.add(file.getLength());
        }
        builder.add("filepaths", StringUtils.explode(filePaths, "\t"));
        builder.add("filelenghts", StringUtils.explode(fileLengths, "\t"));
        return builder.toList();
    }
    
    @Override
    public List<NameValue<String>> toNameValueList() {
        return nameValues;
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
