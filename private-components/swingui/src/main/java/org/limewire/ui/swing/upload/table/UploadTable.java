package org.limewire.ui.swing.upload.table;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.ui.swing.components.RemoteHostWidgetFactory;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.LimeSingleColumnTableFormat;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GlazedListsSwingFactory;

import ca.odell.glazedlists.swing.EventTableModel;

public class UploadTable extends MouseableTable {
    private EventTableModel<UploadItem> model;

    public UploadTable(UploadListManager uploadListManager, CategoryIconManager categoryIconManager, 
            ProgressBarDecorator progressBarFactory, PropertiesFactory<UploadItem> propertiesFactory,
            LibraryNavigator libraryNavigator, LibraryManager libraryManager,
            RemoteHostWidgetFactory remoteHostWidgetFactory) {
        model = GlazedListsSwingFactory.eventTableModel(uploadListManager.getSwingThreadSafeUploads(), new LimeSingleColumnTableFormat<UploadItem>(UploadItem.class));
        setModel(model);
        
        setStripeHighlighterEnabled(false);
        setStripesPainted(false);
        setFillsViewportHeight(false);
        
        UploadActionHandler actionHandler = new UploadActionHandler(uploadListManager, propertiesFactory, libraryNavigator);
        
        UploadTableRendererEditor editor = new UploadTableRendererEditor(categoryIconManager, progressBarFactory, remoteHostWidgetFactory);
        editor.setActionHandler(actionHandler);
        getColumn(0).setCellEditor(editor);
        getColumn(0).setCellRenderer(new UploadTableRendererEditor(categoryIconManager, progressBarFactory, remoteHostWidgetFactory));
        setRowHeight(editor.getPreferredSize().height);
        
        setPopupHandler(new UploadPopupHandler(this, actionHandler, libraryManager));
    }

    public UploadItem getUploadItem(int popupRow) {
        return model.getElementAt(popupRow);
    }

}
