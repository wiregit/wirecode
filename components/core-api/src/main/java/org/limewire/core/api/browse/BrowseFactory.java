package org.limewire.core.api.browse;

import org.limewire.io.Address;

public interface BrowseFactory {
    Browse createBrowse(Address address);
}
