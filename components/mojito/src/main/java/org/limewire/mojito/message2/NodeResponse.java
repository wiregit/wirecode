package org.limewire.mojito.message2;

import org.limewire.mojito.routing.Contact;

public interface NodeResponse extends LookupResponse, SecurityTokenProvider {

    public Contact[] getContacts();
}
