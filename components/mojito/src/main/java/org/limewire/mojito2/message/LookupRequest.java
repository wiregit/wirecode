package org.limewire.mojito2.message;

import org.limewire.mojito.KUID;

public interface LookupRequest extends RequestMessage {

    public KUID getLookupId();
}
