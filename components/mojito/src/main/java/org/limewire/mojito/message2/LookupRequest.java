package org.limewire.mojito.message2;

import org.limewire.mojito.KUID;

public interface LookupRequest extends RequestMessage {

    public KUID getLookupId();
}
