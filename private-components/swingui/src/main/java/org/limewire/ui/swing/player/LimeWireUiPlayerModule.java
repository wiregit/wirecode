package org.limewire.ui.swing.player;

import com.google.inject.AbstractModule;

public class LimeWireUiPlayerModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PlayerMediator.class).to(PlayerMediatorImpl.class);
    }
}
