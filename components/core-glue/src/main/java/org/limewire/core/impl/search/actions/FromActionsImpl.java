package org.limewire.core.impl.search.actions;

import java.net.UnknownHostException;

import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.core.api.search.actions.FromActions;
import org.limewire.io.Address;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.core.api.endpoint.RemoteHost;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FromActionsImpl implements FromActions {
    private static final Log LOG = LogFactory.getLog(FromActionsImpl.class);

    private final BrowseFactory browseFactory;

    @Inject
    public FromActionsImpl(BrowseFactory browseFactory) {
        this.browseFactory = browseFactory;
    }

    @Override
    public void chatWith(RemoteHost person) {
        LOG.debugf("chatWith: {0}", person.getName());
    }

    @Override
    public void showFilesSharedBy(RemoteHost person) {
        LOG.debugf("showFilesSharedBy: {0}", person.getName());
    }

    @Override
    public void viewLibraryOf(RemoteHost person) {
        LOG.debugf("viewLibraryOf: {0}", person.getName());
        viewLibrary(person, new BrowseListener() {
            @Override
            public void handleBrowseResult(SearchResult searchResult) {
                LOG.debugf("Browsed File: {0}", searchResult.getProperty(PropertyKey.NAME));
                System.out.println("Browsed File: " + searchResult.getProperty(PropertyKey.NAME));
            }

            @Override
            public void browseFinished(boolean success) {
                LOG.debugf("Browsed Finished.");
                System.out.println("Browsed Finished.");
            }
        });
    }

    private void viewLibrary(RemoteHost person, BrowseListener browseListener) {
        
        //TODO make sure we are supporting all the various address cases.
        //direct connection
        //firewalled connections
        //push proxies
        
        Address address;
        try {
            address = person.getAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        Browse browse = browseFactory.createBrowse(address);
        browse.start(browseListener);
    }

}
