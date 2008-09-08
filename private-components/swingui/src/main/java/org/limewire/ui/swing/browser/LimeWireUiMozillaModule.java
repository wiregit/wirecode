package org.limewire.ui.swing.browser;

import org.limewire.core.impl.mozilla.LimeMozillaDownloadManagerListenerImpl;

import com.google.inject.AbstractModule;

public class LimeWireUiMozillaModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(LimeMozillaDownloadManagerListenerImpl.class);
        bind(LimeMozillaOverrides.class);
    }

}
