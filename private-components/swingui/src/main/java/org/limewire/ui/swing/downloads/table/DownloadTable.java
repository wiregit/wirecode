package org.limewire.ui.swing.downloads.table;

import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import ca.odell.glazedlists.EventList;

/**
 * Table showing DownloadItems. Provides popup menus and double click handling.
 * No renderers or editors are set by default.
 */
public class DownloadTable extends MouseableTable {   
    
    private DownloadTableModel model;

    @AssistedInject
	public DownloadTable(DownloadTableCellFactory tableCellFactory, @Assisted EventList<DownloadItem> downloadItems) {		

        initialise(downloadItems);
        
        setShowGrid(false, false);
        
        DownloadTableCell editorMutator   = tableCellFactory.create();
        DownloadTableCell rendererMutator = tableCellFactory.create();
        
        DownloadTableEditor editor = new DownloadTableEditor(editorMutator);
        editor.initialiseEditor(downloadItems);
        getColumnModel().getColumn(0).setCellEditor(editor);
        
        DownloadTableRenderer renderer = new DownloadTableRenderer(rendererMutator);
        renderer.initialiseRenderer(editor.getEditorListener());
        getColumnModel().getColumn(0).setCellRenderer(renderer);
        
        setRowHeight(56);
    }
	
	public DownloadItem getDownloadItem(int row){
	    return model.getDownloadItem(convertRowIndexToModel(row));
	}

    private void initialise(EventList<DownloadItem> downloadItems) {
        model = new DownloadTableModel(downloadItems);
        setModel(model);

        TablePopupHandler popupHandler = new DownloadPopupHandler(new DownloadActionHandler(downloadItems), this);

        setPopupHandler(popupHandler);

        TableDoubleClickHandler clickHandler = new TableDoubleClickHandler() {
            @Override
            public void handleDoubleClick(int row) {
                final DownloadItem item = getDownloadItem(row);
                BackgroundExecutorService.schedule(new Runnable(){
                    public void run() {
                        if (item.isLaunchable()) {
                            if(item.getCategory() == Category.AUDIO){
                                PlayerUtils.play(item.getFile());
                            } else {
                                NativeLaunchUtils.launchFile(item.getFile());
                            }
                        }
                    }
                });
            }
        };

        setDoubleClickHandler(clickHandler);

    }
}