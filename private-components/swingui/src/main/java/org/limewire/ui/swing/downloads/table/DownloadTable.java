package org.limewire.ui.swing.downloads.table;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.downloads.DownloadItemUtils;
import org.limewire.ui.swing.downloads.table.renderer.ButtonRendererEditor;
import org.limewire.ui.swing.downloads.table.renderer.MessageRenderer;
import org.limewire.ui.swing.downloads.table.renderer.DownloadProgressRenderer;
import org.limewire.ui.swing.downloads.table.renderer.DownloadTitleRenderer;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Table showing DownloadItems. Provides popup menus and double click handling.
 * No renderers or editors are set by default.
 */
public class DownloadTable extends AbstractDownloadTable {   
    
    @Resource private int rowHeight;    
    
    private DownloadTableModel model;

    @AssistedInject
	public DownloadTable(ProgressBarDecorator progressBarDecorator, CategoryIconManager iconManager, DownloadTableCellFactory tableCellFactory, DownloadActionHandler actionHandler, 
	        @Assisted EventList<DownloadItem> downloadItems) {	
        

        GuiUtils.assignResources(this);
                
        initialise(downloadItems, actionHandler);
        
        setStripeHighlighterEnabled(false);
        
        setShowGrid(true, false);
        
        setRowSelectionAllowed(true);

        getColumnModel().getColumn(0).setCellRenderer(new DownloadTitleRenderer(iconManager));
        getColumnModel().getColumn(1).setCellRenderer(new DownloadProgressRenderer(progressBarDecorator));
        getColumnModel().getColumn(2).setCellRenderer(new MessageRenderer());
        getColumnModel().getColumn(3).setCellRenderer(new ButtonRendererEditor());
        
        
        setRowHeight(this.rowHeight);
    }
	
	public DownloadItem getDownloadItem(int row){
	    return model.getDownloadItem(convertRowIndexToModel(row));
	}

    private void initialise(EventList<DownloadItem> downloadItems, DownloadActionHandler actionHandler) {
        model = new DownloadTableModel(downloadItems);
        setModel(model);

        TablePopupHandler popupHandler = new DownloadPopupHandler(actionHandler, this);

        setPopupHandler(popupHandler);

        TableDoubleClickHandler clickHandler = new TableDoubleClickHandler() {
            @Override
            public void handleDoubleClick(int row) {
                DownloadItem item = getDownloadItem(row);
                if(item.isLaunchable()) {
                    DownloadItemUtils.launch(item);
                }
            }
        };

        setDoubleClickHandler(clickHandler);
        

        ButtonRendererEditor editor = new ButtonRendererEditor();
        editor.setActionHandler(actionHandler);
        getColumnModel().getColumn(3).setCellEditor(editor);

    }
}