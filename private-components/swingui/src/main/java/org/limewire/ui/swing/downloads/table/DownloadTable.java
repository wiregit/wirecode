package org.limewire.ui.swing.downloads.table;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.downloads.DownloadItemUtils;
import org.limewire.ui.swing.downloads.table.renderer.ButtonRendererEditor;
import org.limewire.ui.swing.downloads.table.renderer.DownloadProgressRenderer;
import org.limewire.ui.swing.downloads.table.renderer.DownloadTitleRenderer;
import org.limewire.ui.swing.downloads.table.renderer.MessageRenderer;
import org.limewire.ui.swing.table.TableColors;
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
	public DownloadTable(ProgressBarDecorator progressBarDecorator, CategoryIconManager iconManager, DownloadActionHandler actionHandler, 
	        @Assisted EventList<DownloadItem> downloadItems) {
        
        GuiUtils.assignResources(this);
                
        initialize(downloadItems, actionHandler);
        
        TableColors colors = new TableColors();
        setHighlighters(
                new ColorHighlighter(HighlightPredicate.EVEN, colors.evenColor,
                        colors.evenForeground, colors.selectionColor,
                        colors.selectionForeground),
                new ColorHighlighter(HighlightPredicate.ODD, colors.evenColor,
                        colors.evenForeground, colors.selectionColor,
                        colors.selectionForeground));
        
        setShowGrid(true, false);        

        getColumnModel().getColumn(DownloadTableFormat.TITLE).setCellRenderer(new DownloadTitleRenderer(iconManager));
        getColumnModel().getColumn(DownloadTableFormat.PROGRESS).setCellRenderer(new DownloadProgressRenderer(progressBarDecorator));
        getColumnModel().getColumn(DownloadTableFormat.MESSAGE).setCellRenderer(new MessageRenderer());
        getColumnModel().getColumn(DownloadTableFormat.ACTION).setCellRenderer(new ButtonRendererEditor());
        
        setRowHeight(rowHeight);
    }
	
	public DownloadItem getDownloadItem(int row){
	    return model.getDownloadItem(convertRowIndexToModel(row));
	}

    private void initialize(EventList<DownloadItem> downloadItems, DownloadActionHandler actionHandler) {
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
        getColumnModel().getColumn(DownloadTableFormat.ACTION).setCellEditor(editor);

    }
}