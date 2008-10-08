package org.limewire.xmpp.api.client;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;

public class MockFileMetadata implements FileMetaData {
    private String id;
    private String name;
    
    public MockFileMetadata(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public Date getCreateTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getIndex() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Map<String, String> getMetaData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Set<URN> getURNs() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toXML() {
        // TODO Auto-generated method stub
        return null;
    }

    public RemoteFileDesc toRemoteFileDesc(LimePresence presence, RemoteFileDescFactory rfdFactory)
    throws IOException {
        // TODO Auto-generated method stub
        return null;
    }
}
