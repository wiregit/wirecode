package org.limewire.core.api.library;

import org.limewire.xmpp.api.client.LimePresence;

public interface PresenceLibrary extends RemoteFileList {
    LimePresence getPresence();
}
