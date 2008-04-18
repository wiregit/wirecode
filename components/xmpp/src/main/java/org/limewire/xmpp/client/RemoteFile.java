package org.limewire.xmpp.client;

import com.limegroup.gnutella.URN;

public class RemoteFile {
    protected String id;
    protected String name;
    
    public RemoteFile(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
