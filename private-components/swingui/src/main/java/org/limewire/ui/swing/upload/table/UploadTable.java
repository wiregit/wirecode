package org.limewire.ui.swing.upload.table;

import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.upload.UploadMediator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;

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
    
    @Resource private int rowHeight;  
    @Resource private int gapMinWidth;  
    @Resource private int gapPrefWidth; 
    @Resource private int gapMaxWidth; 
    @Resource private int titleMinWidth;  
    @Resource private int titlePrefWidth; 
    @Resource private int titleMaxWidth;  
    @Resource private int progressMinWidth;
    @Resource private int progressPrefWidth; 
    @Resource private int progressMaxWidth;  
    @Resource private int messageMinWidth; 
    @Resource private int messagePrefWidth; 
    @Resource private int messageMaxWidth;  
    @Resource private int actionMinWidth; 
    @Resource private int actionPrefWidth;  
    @Resource private int actionMaxWidth; 
    @Resource private int cancelMinWidth; 
    @Resource private int cancelPrefWidth;
    @Resource private int cancelMaxWidth;
    
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
        
        this.iconManager = iconManager;
        this.progressBarDecorator = progressBarDecorator;
        this.uploadActionHandlerFactory = uploadActionHandlerFactory;
        
        GuiUtils.assignResources(this);
        
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
        
        setEmptyRowsPainted(true);
        setRowHeight(rowHeight);
        
        initializeColumns();
        initializeRenderers();
    }

    /**
     * Initializes the columns in the table.
     */
    private void initializeColumns() {
        setColumnWidths(UploadTableFormat.TITLE_COL, titleMinWidth, titlePrefWidth, titleMaxWidth);
        setColumnWidths(UploadTableFormat.TITLE_GAP, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setColumnWidths(UploadTableFormat.PROGRESS_COL, progressMinWidth, progressPrefWidth, progressMaxWidth);
        setColumnWidths(UploadTableFormat.PROGRESS_GAP, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setColumnWidths(UploadTableFormat.MESSAGE_COL, messageMinWidth, messagePrefWidth, messageMaxWidth);
        setColumnWidths(UploadTableFormat.MESSAGE_GAP, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setColumnWidths(UploadTableFormat.ACTION_COL, actionMinWidth, actionPrefWidth, actionMaxWidth);
        setColumnWidths(UploadTableFormat.ACTION_GAP, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setColumnWidths(UploadTableFormat.CANCEL_COL, cancelMinWidth, cancelPrefWidth, cancelMaxWidth);
    }
    
    /**
     * Initializes the cell renderers and editors in the table.
     */
    private void initializeRenderers() {
        // Set column renderers.
        setColumnRenderer(UploadTableFormat.TITLE_COL, new UploadTitleRenderer(iconManager));
        setColumnRenderer(UploadTableFormat.MESSAGE_COL, new UploadMessageRenderer());
        setColumnRenderer(UploadTableFormat.PROGRESS_COL, new UploadProgressRenderer(progressBarDecorator));
// TODO setColumnRenderer(UploadTableFormat.ACTION_COL, new ActionRendererEditor(uploadActionHandlerFactory.get()));
        setColumnRenderer(UploadTableFormat.CANCEL_COL, new UploadCancelRendererEditor(uploadActionHandlerFactory.get()));
        
        // Set column gap renderers.
        TableCellRenderer gapRenderer = new GapRenderer();
        setColumnRenderer(UploadTableFormat.TITLE_GAP, gapRenderer);
        setColumnRenderer(UploadTableFormat.PROGRESS_GAP, gapRenderer);
        setColumnRenderer(UploadTableFormat.MESSAGE_GAP, gapRenderer);
        setColumnRenderer(UploadTableFormat.ACTION_GAP, gapRenderer);
        
        // Set column editors.
        setColumnEditor(UploadTableFormat.CANCEL_COL, new UploadCancelRendererEditor(uploadActionHandlerFactory.get()));
    }
    
    /**
     * Returns the upload item associated with the specified table row.
     */
    public UploadItem getUploadItem(int row) {
//        return model.getElementAt(row);
        return getElementAt(row);
    }
}
