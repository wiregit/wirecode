package org.limewire.ui.swing.painter;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.util.LimeWireUtils;

public class LimeWireUiPainterModule extends AbstractModule {

    @Override
    protected void configure() {
        if (LimeWireUtils.isPro()) {
            bind(ProgressPainterFactory.class).to(ProProgressPainterFactory.class);
        } else {
            bind(ProgressPainterFactory.class).to(BasicProgressPainterFactory.class);
        }
        
    }
    
}
