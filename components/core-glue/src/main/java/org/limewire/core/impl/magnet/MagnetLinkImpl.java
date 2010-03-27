package org.limewire.core.impl.magnet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.magnet.MagnetLink;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.browser.MagnetOptions;

public class MagnetLinkImpl implements MagnetLink {
    
    private final MagnetOptions magnetOptions;
    
    public MagnetLinkImpl(MagnetOptions magnetOptions) {
        this.magnetOptions = magnetOptions;
    }

    @Override
    public boolean isGnutellaDownloadable() {
        return magnetOptions.isGnutellaDownloadable();
    }
    
    @Override
    public boolean isTorrentDownloadable() {
        return magnetOptions.isTorrentDownloadable();
    }

    @Override
    public boolean isKeywordTopicOnly() {
        return magnetOptions.isKeywordTopicOnly();
    }
    
    public MagnetOptions getMagnetOptions() {
        return magnetOptions;
    }
    
    @Override
    public String getQueryString() {
        return magnetOptions.getQueryString();
    }
    
    @Override
    public URN getURN() {
        return magnetOptions.getSHA1Urn();
    }
    
    @Override
    public List<URI> getTrackerUrls() {
        List<String> stringResultsList = magnetOptions.getTR();
        List<URI> uriResultsList = new ArrayList<URI>(stringResultsList.size());
        
        for ( String uri : stringResultsList ) {
            try {
                uriResultsList.add(new URI(uri));
            } catch (URISyntaxException e) {
                // Throw out the tracker since its not valid
            }
        }
        
        return uriResultsList;
    }
    
    @Override
    public String getName() {
        String name = magnetOptions.getDisplayName();
        return name != null ? name : magnetOptions.getFileNameForSaving();
    }
}
