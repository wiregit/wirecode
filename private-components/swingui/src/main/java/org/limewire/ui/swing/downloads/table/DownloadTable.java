package org.limewire.ui.swing.downloads.table;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;

public class DownloadTable extends MouseableTable {
    
    private DownloadRendererEditor editor;
    
    private DownloadTableModel model;
    
//TODO: rename DownloadTable & pull out common base class for this class and SimpleDownloadTable
	public DownloadTable(EventList<DownloadItem> downloadItems) {		
		model = new DownloadTableModel(downloadItems);
		setModel(model);
		
		setShowGrid(false, false);

		editor = new DownloadRendererEditor();
        editor.initializeEditor(downloadItems);
        getColumnModel().getColumn(0).setCellEditor(editor);
        
		DownloadRendererEditor renderer = new DownloadRendererEditor();		
		getColumnModel().getColumn(0).setCellRenderer(renderer);
		
		setRowHeight(renderer.getPreferredSize().height);

        TablePopupHandler popupHandler = new DownloadPopupHandler(new DownloadActionHandler(downloadItems), this);

        setPopupHandler(popupHandler);

        TableDoubleClickHandler clickHandler = new TableDoubleClickHandler() {
            @Override
            public void handleDoubleClick(int row) {
                DownloadItem item = getDownloadItem(row);
                if (item.isLaunchable()) {
                    NativeLaunchUtils.launchFile(item.getFile());
                }
            }
        };

        setDoubleClickHandler(clickHandler);
    }
	
	
	public DownloadItem getDownloadItem(int row){
	    return model.getDownloadItem(convertRowIndexToModel(row));
	}

}
