package org.limewire.ui.swing.downloads.table;

import org.limewire.core.api.download.DownloadItem;

import ca.odell.glazedlists.EventList;

/**
 * Fancy list view of downloads, using DownloadRendererEditor. FancyDownloadTable inherits popup and double
 * click handling from DownloadTable.
 */
public class FancyDownloadTable extends DownloadTable {

	public FancyDownloadTable(EventList<DownloadItem> downloadItems) {		
	    super(downloadItems); 
		
		setShowGrid(false, false);

		DownloadRendererEditor editor = new DownloadRendererEditor();
        editor.initializeEditor(downloadItems);
        getColumnModel().getColumn(0).setCellEditor(editor);
        
		DownloadRendererEditor renderer = new DownloadRendererEditor();		
		getColumnModel().getColumn(0).setCellRenderer(renderer);
		
		setRowHeight(renderer.getPreferredSize().height);
    
    }

}
