package com.limegroup.gnutella.caas.restlet;

import com.limegroup.gnutella.caas.Download;
import com.limegroup.gnutella.caas.DownloadFactory;
import com.limegroup.gnutella.caas.SearchResult;

public class RestletDownloadFactory implements DownloadFactory {

    public Download createDownload(SearchResult sr) {
        return new RestletDownload(sr);
    }

}
