package org.limewire.ui.swing.player;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;
import com.google.inject.name.Names;

public class LimeWireUiPlayerModule extends AbstractModule {

    @Override
    protected void configure() {
      //TODO:  this is not the best way to handle player access but it gets it working for now.
        requestStaticInjection(PlayerUtils.class);

        bind(PlayerMediator.class).annotatedWith(Names.named("audio")).to(AudioPlayerMediator.class);
        bind(PlayerMediator.class).annotatedWith(Names.named("video")).to(VideoPlayerMediator.class);
        bind(VideoPanelFactory.class).toProvider(FactoryProvider.newFactory(VideoPanelFactory.class, VideoPanel.class));
    }

}
