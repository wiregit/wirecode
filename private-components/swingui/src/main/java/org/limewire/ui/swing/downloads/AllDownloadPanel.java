package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.downloads.table.DownloadTable;
import org.limewire.ui.swing.downloads.table.DownloadTableFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

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
	
	@AssistedInject
	public AllDownloadPanel(DownloadTableFactory downloadTableFactory, @Assisted EventList<DownloadItem> list) {
		super(new BorderLayout());

		table = downloadTableFactory.create(list);
		table.setTableHeader(null);
		JScrollPane pane = new JScrollPane(table);
		pane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		add(pane, BorderLayout.CENTER);
	}
}