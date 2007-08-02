package com.limegroup.store;

import java.util.EventListener;

public interface StoreDownloadListener extends EventListener {
    public void handleStoreDownloadEvent( StoreDownloadEvent event );
}
