package org.limewire.xmpp.client;

import org.jivesoftware.smackx.packet.StreamInitiation;

public class FileMetaDataAdapter extends StreamInitiation.File implements FileMetaData {
    
    public FileMetaDataAdapter(FileMetaData file) {
        super(file.getName(), file.getSize());
        setHash(file.getId());
        setDate(file.getDate());
        setDesc(file.getDescription());
    }
    
    public FileMetaDataAdapter(StreamInitiation.File file) {
        super(file.getName(), file.getSize());
        setHash(file.getHash());
        setDate(file.getDate());
        setDesc(file.getDesc());
    }

    public String getId() {
        return getHash();
    }

    public String getDescription() {
        return getDesc();
    }
}
