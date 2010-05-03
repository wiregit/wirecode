package org.limewire.core.impl.search.torrentweb;

public interface TorrentRobotsTxtStore {

    public static final int MAX_ROBOTS_TXT_SIZE = 5 * 1024;
    
    public String getRobotsTxt(String host);
    
    public void storeRobotsTxt(String host, String robotsTxt);
    
}
