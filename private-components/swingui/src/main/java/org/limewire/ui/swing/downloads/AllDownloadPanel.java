package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.downloads.table.FancyDownloadTable;

import ca.odell.glazedlists.EventList;


public class AllDownloadPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4695453105493573743L;
	private JXTable table;

	/**
	 * Create the panel
	 */
	public AllDownloadPanel(EventList<DownloadItem> list) {
		super(new BorderLayout());

		table = new FancyDownloadTable(list);
		table.setTableHeader(null);
		add(new JScrollPane(table), BorderLayout.CENTER);
	}

}
