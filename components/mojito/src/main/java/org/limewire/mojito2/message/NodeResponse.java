package org.limewire.mojito2.message;

import org.limewire.mojito2.routing.Contact;

public interface NodeResponse extends LookupResponse, SecurityTokenProvider {

    public Contact[] getContacts();
}
