package org.limewire.ui.swing.downloads.table;

import java.util.HashSet;
import java.util.Set;

import javax.swing.RowFilter;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;


/**
 * A row filter that only shows the specified DownloadStates
 */
public class DownloadStateRowFilter extends RowFilter<DownloadTableModel, Integer> {
	private Set<DownloadState> downloadStates = new HashSet<DownloadState>();

	/**
	 * 
	 * @param states the DownloadStates shown
	 */
	public DownloadStateRowFilter(DownloadState... states) {
		for (DownloadState state : states) {
			downloadStates.add(state);
		}
	}

	@Override
	public boolean include(
			Entry<? extends DownloadTableModel, ? extends Integer> entry) {
		for (int i = entry.getValueCount() - 1; i >= 0; i--) {
			int rowIndex = entry.getIdentifier();
			DownloadItem item = entry.getModel().getDownloadItem(rowIndex);

			if (checkState(item.getState())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 
	 * @param testState the state being checked
	 * @return true if state should be shown, false if it is filtered out
	 */
	private boolean checkState(DownloadState testState) {
		if (testState == null)
			return false;
		if (downloadStates.isEmpty())
			return true;

		return downloadStates.contains(testState);
	}
}
