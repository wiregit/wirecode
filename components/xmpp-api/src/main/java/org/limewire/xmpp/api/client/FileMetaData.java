package org.limewire.xmpp.api.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.io.IOException;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;

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
    public Set<URN> getURNs() throws IOException;
    public Date getCreateTime();
    public String toXML();
    public RemoteFileDesc toRemoteFileDesc(LimePresence presence, RemoteFileDescFactory rfdFactory) throws IOException;
}
