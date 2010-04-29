package org.limewire.mojito2.message;

import org.limewire.mojito.routing.Contact;

public interface NodeResponse extends LookupResponse, SecurityTokenProvider {

    public Contact[] getContacts();
}
