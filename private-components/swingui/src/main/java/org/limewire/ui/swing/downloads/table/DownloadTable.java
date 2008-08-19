package org.limewire.ui.swing.downloads.table;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;

/**
 * Table showing DownloadItems. Provides popup menus and double click handling.
 * No renderers or editors are set by default.
 */
public class DownloadTable extends MouseableTable {   
    
    private DownloadTableModel model;

	public DownloadTable(EventList<DownloadItem> downloadItems) {		
		model = new DownloadTableModel(downloadItems);
		setModel(model);

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
