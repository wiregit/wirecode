package org.limewire.xmpp.api.client;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * The file meta-data necessary to do a file exchange
 */
public interface FileMetaData {

    public String getId();
    public String getName();
    public long getSize();
    public String getDescription();
    public long getIndex();
    public Map<String, String> getMetaData();
    public Set<String> getURNsAsString();
    public Date getCreateTime();
    public String toXML();
}
