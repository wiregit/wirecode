package org.limewire.xmpp.client;

import java.util.Date;

public class File {
    protected String id;
    protected String name;
    private long size;
    private Date date;
    private String description;
    
    public File(String id, String name) {
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
