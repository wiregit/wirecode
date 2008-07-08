package org.limewire.xmpp.client.impl;

import org.jivesoftware.smackx.packet.StreamInitiation;
import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.HostMetaData;

import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.net.URI;

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

    public int getIndex() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<String, String> getMetaData() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<URI> getURIs() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date getCreateTime() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public HostMetaData getHostMetaData() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
