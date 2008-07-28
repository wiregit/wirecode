package org.limewire.ui.swing;

import org.limewire.ui.swing.mainframe.LimeWireUiMainframeModule;
import org.limewire.ui.swing.nav.LimeWireUiNavModule;
import org.limewire.ui.swing.search.LimeWireUiSearchModule;
import org.limewire.ui.swing.xmpp.LimeWireUiXMPPModule;

import com.google.inject.AbstractModule;

public class LimeWireSwingUiModule extends AbstractModule {
    
    @Override
    protected void configure() {
        install(new LimeWireUiSearchModule());
        install(new LimeWireUiNavModule());
        install(new LimeWireUiMainframeModule());
        install(new LimeWireUiXMPPModule());
    }

}
