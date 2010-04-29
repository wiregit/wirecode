package org.limewire.mojito2.message;

import org.limewire.mojito2.KUID;

public interface LookupRequest extends RequestMessage {

    public KUID getLookupId();
}
