package org.limewire.ui.swing.browser;

import com.google.inject.AbstractModule;

public class LimeWireMozillaUiModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(LimeMozillaInitializer.class);
    }

}
