package com.limegroup.gnutella.xmpp;

import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.URN;
import org.limewire.service.ErrorService;
import org.limewire.xmpp.client.service.FileMetaData;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FileMetaDataAdapter implements FileMetaData {
    protected FileDetails fileDetails;

    public FileMetaDataAdapter(FileDetails fileDetails) {
        this.fileDetails = fileDetails;
    }

    public String getId() {
        return fileDetails.getSHA1Urn().toString();
    }

    public String getName() {
        return fileDetails.getFileName();
    }

    public long getSize() {
        return fileDetails.getSize();
    }

    public String getDescription() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long getIndex() {
        return fileDetails.getIndex();
    }

    public Map<String, String> getMetaData() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<URI> getURIs() throws URISyntaxException {
        return toURIs(fileDetails.getUrns());
    }

    private Set<URI> toURIs(Set<URN> urns) {
        Set<URI> uris = new HashSet<URI>();
        for(URN urn : urns) {
            try {
                uris.add(new URI(urn.toString()));
            } catch (URISyntaxException e) {
                // TODO is this necessary?
                ErrorService.error(e);
            }
        }
        return uris;
    }

    public Date getCreateTime() {
        return new Date(fileDetails.getCreationTime());
    }

    public String toXML() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
