package com.limegroup.gnutella.downloader;


import com.limegroup.gnutella.lws.server.LWSManager;

/**
 * A class that initializes listeners to the passed in instance of
 * {@link LWSManager}.
 */
public interface LWSIntegrationServices {

    /**
     * The prefix with which to start download URLS. For example, if we call the
     * <code>Download</code> command with a <code>url</code> argument
     * <code>/SomeURL</code>, then if we passed in <code>limewire.org</code>
     * to this method, the resulting download would come from
     * <code>http://limewire.org/SomeURL</code>.
     * 
     * @param downloadPrefix new download prefix
     */
    void setDownloadPrefix(String downloadPrefix);
    
}
