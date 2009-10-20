package org.limewire.ui.swing.upload;

import org.limewire.ui.swing.upload.table.UploadTable;
import org.limewire.ui.swing.upload.table.UploadTableFactory;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

/**
 * Module to configure Guice bindings for the Uploads UI classes.
 */
public class LimeWireUiUploadModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(UploadTableFactory.class).toProvider(
                FactoryProvider.newFactory(UploadTableFactory.class, UploadTable.class));
    }
}
