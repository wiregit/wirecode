package org.limewire.ui.swing;

import org.limewire.ui.swing.downloads.LimeWireUiDownloadsModule;
import org.limewire.ui.swing.friends.LimeWireUiFriendsModule;
import org.limewire.ui.swing.images.LimeWireUiImagesModule;
import org.limewire.ui.swing.library.LimeWireUiLibraryModule;
import org.limewire.ui.swing.mainframe.LimeWireUiMainframeModule;
import org.limewire.ui.swing.nav.LimeWireUiNavModule;
import org.limewire.ui.swing.options.LimeWireUiOptionsModule;
import org.limewire.ui.swing.player.LimeWireUiPlayerModule;
import org.limewire.ui.swing.search.LimeWireUiSearchModule;
import org.limewire.ui.swing.sharing.LimeWireUiSharingModule;
import org.limewire.ui.swing.statusbar.LimeWireUiStatusbarModule;
import org.limewire.ui.swing.tray.LimeWireUiTrayModule;
import org.limewire.ui.swing.util.LimeWireUiUtilModule;

import com.google.inject.AbstractModule;

public class LimeWireSwingUiModule extends AbstractModule {
    
    @Override
    protected void configure() {
        install(new LimeWireUiUtilModule());
        install(new LimeWireUiSearchModule());
        install(new LimeWireUiNavModule());
        install(new LimeWireUiMainframeModule());
        install(new LimeWireUiTrayModule());
        install(new LimeWireUiFriendsModule());
        install(new LimeWireUiPlayerModule());
        install(new LimeWireUiSharingModule());
        install(new LimeWireUiImagesModule());
        install(new LimeWireUiLibraryModule());
        install(new LimeWireUiDownloadsModule());
        install(new LimeWireUiOptionsModule());
        install(new LimeWireUiStatusbarModule());
    }
}