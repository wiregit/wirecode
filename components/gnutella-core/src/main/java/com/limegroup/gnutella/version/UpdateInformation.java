package com.limegroup.gnutella.version;

import org.apache.commons.httpclient.URI;

/**
 * Simple interface for retrieving the most recent update info.
 */
public interface UpdateInformation {
    
    public URI getUpdateURI();
    
    public String getUpdateText();
    
    public String getVersion();
    
}