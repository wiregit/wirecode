package org.limewire.mojito.message;

import org.limewire.mojito.KUID;

public interface LookupRequest extends RequestMessage {

    public KUID getLookupId();
}
