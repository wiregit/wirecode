package org.limewire.xmpp.client.service;

import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * The file meta-data necessary to do a jingle file exchange
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
     * @return the last modified time of the file
     */
    public Date getDate();

    /**
     * @return the description of the file, as input by the user
     */
    public String getDescription();

    public int getIndex();
    public Map<String, String> getMetaData();
    public Set<URI> getURIs();
    public Date getCreateTime();
    public HostMetaData getHostMetaData();
}
