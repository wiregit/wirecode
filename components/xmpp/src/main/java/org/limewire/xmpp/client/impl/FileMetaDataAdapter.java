package org.limewire.xmpp.client.impl;

import org.jivesoftware.smackx.packet.StreamInitiation;
import org.limewire.xmpp.client.service.FileMetaData;

/**
 * An adapter between the xmpp component class <code>FileMetaData</code> and the
 * smack class <code>StreamInitiation.File</code>
 */
class FileMetaDataAdapter extends StreamInitiation.File implements FileMetaData {
    
    FileMetaDataAdapter(FileMetaData file) {
        super(file.getName(), file.getSize());
        setHash(file.getId());
        setDate(file.getDate());
        setDesc(file.getDescription());
    }
    
    FileMetaDataAdapter(StreamInitiation.File file) {
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
