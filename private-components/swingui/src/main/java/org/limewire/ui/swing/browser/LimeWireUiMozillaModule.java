package org.limewire.ui.swing.browser;

import org.limewire.ui.swing.browser.download.LimeMozillaDownloadManager;
import org.limewire.ui.swing.browser.download.LimeMozillaDownloadManagerListener;

import com.google.inject.AbstractModule;

public class LimeWireUiMozillaModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(LimeMozillaDownloadManager.class);
        bind(LimeMozillaDownloadManagerListener.class);
    }

}
