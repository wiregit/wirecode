package org.limewire.core.impl.download;

import org.limewire.core.api.download.DownloadListener;

public interface DownloadListenerList {

    void addDownloadListener(DownloadListener listener);

    void removeDownloadListener(DownloadListener listener);

}
