package org.limewire.friend.impl.feature;

import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.LibraryChangedNotifier;

public class LibraryChangedNotifierFeature extends Feature<LibraryChangedNotifier> {

    public static final URI ID;

    static {
        try {
            ID = new URI("http://www.limewire.org/library-changed-notifier/2008-12-01");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public LibraryChangedNotifierFeature(LibraryChangedNotifier notifier) {
        super(notifier, ID);
    }
}
