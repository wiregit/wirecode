package org.limewire.ui.swing.downloads;

import org.limewire.ui.swing.downloads.table.LimeWireUiDownloadsTableModule;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiDownloadsModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LimeWireUiDownloadsTableModule());

        bind(AllDownloadPanelFactory.class).toProvider(
                FactoryProvider.newFactory(
                        AllDownloadPanelFactory.class, AllDownloadPanel.class));
        
        bind(DownloadHeaderFactory.class).toProvider(
                FactoryProvider.newFactory(
                        DownloadHeaderFactory.class, DownloadHeaderPanel.class));
            
    }

}
