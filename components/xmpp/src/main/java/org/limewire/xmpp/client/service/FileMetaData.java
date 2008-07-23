package org.limewire.xmpp.client.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * The file meta-data necessary to do a file exchange
 */
public interface FileMetaData {

    /**
     * @return the unique id of the file, i.e., its sha1
     */
    public String getId();

    /**
     * @return  the file name
     */
    public String getName();

    /**
     * the size of the file in bytes
     * @return
     */
    public long getSize();

    /**
     * @return the description of the file, as input by the user
     */
    public String getDescription();

    public long getIndex();
    public Map<String, String> getMetaData();
    public Set<URI> getURIs() throws URISyntaxException;
    public Date getCreateTime();
    public String toXML();
}
