package org.limewire.core.api.browse;

import org.limewire.net.address.Address;

public interface BrowseFactory {
    Browse createBrowse(Address address);
}
