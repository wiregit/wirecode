package org.limewire.ui.swing.upload.table;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.upload.UploadMediator;
import org.limewire.ui.swing.util.CategoryIconManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * Table to display the list of UploadItems.
 */
public class UploadTable 
    extends TransferTable<UploadItem> {
//    extends MouseableTable {
//    private final DefaultEventTableModel<UploadItem> model;
    
    private final CategoryIconManager iconManager;
    private final ProgressBarDecorator progressBarDecorator;
    private final Provider<UploadActionHandler> uploadActionHandlerFactory;
    
    @Inject
    public UploadTable(@Assisted UploadMediator uploadMediator,
            LibraryMediator libraryMediator, 
            LibraryManager libraryManager,
            CategoryIconManager iconManager,
            ProgressBarDecorator progressBarDecorator,
            FileInfoDialogFactory fileInfoFactory,
            UploadTableRendererEditor editor,
            UploadTableRendererEditor renderer,
            Provider<UploadActionHandler> uploadActionHandlerFactory) {
        super(uploadMediator.getUploadList(), new UploadTableFormat());
        
//        model = new DefaultEventTableModel<UploadItem>(
//                uploadMediator.getUploadList(), new LimeSingleColumnTableFormat<UploadItem>(UploadItem.class));
//        setModel(model);
//        
//        setStripeHighlighterEnabled(false);
//        setEmptyRowsPainted(false);
//        setFillsViewportHeight(false);
        
//        UploadActionHandler actionHandler = uploadActionHandlerFactory.get();        
//        editor.setActionHandler(actionHandler);
//        getColumn(0).setCellEditor(editor);
//        getColumn(0).setCellRenderer(renderer);
//        setRowHeight(editor.getPreferredSize().height);
//        
//        setPopupHandler(new UploadPopupHandler(this, actionHandler, libraryManager));
        
        this.iconManager = iconManager;
        this.progressBarDecorator = progressBarDecorator;
        this.uploadActionHandlerFactory = uploadActionHandlerFactory;
        
        setEmptyRowsPainted(true);
        
        initializeRenderers();
    }

    /**
     * Initializes the cell renderers and editors in the table.
     */
    private void initializeRenderers() {
        setColumnRenderer(TransferTableFormat.TITLE_COL, new TitleRenderer(iconManager));
        setColumnRenderer(TransferTableFormat.MESSAGE_COL, new MessageRenderer());
        setColumnRenderer(TransferTableFormat.PROGRESS_COL, new ProgressRenderer(progressBarDecorator));
        setColumnRenderer(TransferTableFormat.CANCEL_COL, new CancelRendererEditor(uploadActionHandlerFactory.get()));
        
        setColumnEditor(TransferTableFormat.CANCEL_COL, new CancelRendererEditor(uploadActionHandlerFactory.get()));
    }
    
    /**
     * Returns the upload item associated with the specified table row.
     */
    public UploadItem getUploadItem(int row) {
//        return model.getElementAt(row);
        return getElementAt(row);
    }
}
