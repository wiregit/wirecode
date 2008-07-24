/**
 * 
 */
package org.limewire.ui.swing.downloads.table;

import java.util.Comparator;

import org.limewire.core.api.download.DownloadItem;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;
import ca.odell.glazedlists.swing.EventTableModel;



public class DownloadTableModel extends EventTableModel<DownloadItem> {
	
	private static final long serialVersionUID = 4079559883623594683L;
	private static final int COLUMN_COUNT = 1;
	private EventList<DownloadItem> downloadItems;

	public DownloadTableModel(EventList<DownloadItem> downloadItems) {
		super(downloadItems, new DownloadTableFormat());
		this.downloadItems = downloadItems;
	}


	public DownloadItem getDownloadItem(int index) {
		return downloadItems.get(index);
	}
	

	private static class DownloadTableFormat implements	AdvancedTableFormat<DownloadItem>, WritableTableFormat<DownloadItem> {

		public int getColumnCount() {
			return COLUMN_COUNT;
		}

		public String getColumnName(int column) {
			if (column == 0)
				return "Download Item";

			throw new IllegalStateException("Column "+ column + " out of bounds");
		}

		@Override
		public Object getColumnValue(DownloadItem baseObject, int column) {
			if (column == 0)
				return baseObject;

			throw new IllegalStateException("Column "+ column + " out of bounds");
		}

		@Override
		public Class getColumnClass(int column) {
			return DownloadItem.class;
		}

		@Override
		public Comparator getColumnComparator(int column) {
			// TODO Auto-generated method stub
			return null;
		}

        @Override
        public boolean isEditable(DownloadItem baseObject, int column) {
            if (column == 0)
                return true;
            throw new IllegalStateException("Column "+ column + " out of bounds");
        }

        @Override
        public DownloadItem setColumnValue(DownloadItem baseObject, Object editedValue, int column) {
            if (column == 0)
            return baseObject;
            throw new IllegalStateException("Column "+ column + " out of bounds");
        }

	}


}
