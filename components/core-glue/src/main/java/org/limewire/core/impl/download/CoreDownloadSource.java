package org.limewire.core.impl.download;

import org.limewire.core.api.download.DownloadSource;

public class CoreDownloadSource implements DownloadSource {
    private String name;

    public CoreDownloadSource(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}
