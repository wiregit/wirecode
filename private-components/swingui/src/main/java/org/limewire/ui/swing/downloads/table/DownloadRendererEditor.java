package org.limewire.ui.swing.downloads.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.LimeProgressBar;
import org.limewire.ui.swing.util.GuiUtils;

import org.limewire.ui.swing.util.I18n;


public class DownloadRendererEditor extends JPanel implements
		TableCellRenderer, TableCellEditor {

	//TODO: inject colors
	private Color rolloverBackground=Color.CYAN;
	
	private Color rolloverForeground= Color.BLACK;
	
	//used to maintain proper color of renderer
	private DefaultTableCellRenderer delegateRenderer = new DefaultTableCellRenderer();
	
	private JPanel buttonPanel;
	private JLabel iconLabel;
	private JLabel titleLabel;
	private JLabel statusLabel;
	private LimeProgressBar progressBar;
	private JButton pauseButton;
	private JButton cancelButton;
	private JButton resumeButton;
	private JButton searchAgainButton;
	private JLabel timeLabel;
	private final DownloadEditorListener EDITOR_LISTENER;

	@Resource
	private Icon audioIcon;
	@Resource
	private Icon imageIcon;
	@Resource
	private Icon videoIcon;
	@Resource
	private Icon documentIcon;
	@Resource
	private Icon otherIcon;

	/**
	 * Create the panel
	 */
	public DownloadRendererEditor() {
		GuiUtils.assignResources(this);

		iconLabel = new JLabel();

		titleLabel = new JLabel();
		titleLabel.setFont(new Font("", Font.BOLD, 18));
		titleLabel.setText("Title - title - title");

		statusLabel = new JLabel();
		statusLabel.setFont(new Font("", Font.PLAIN, 9));
		statusLabel.setText("Downloading 2MB of 4322 MPG from Tom");

		progressBar = new LimeProgressBar();
		Dimension size = new Dimension(350, 25);
		progressBar.setMaximumSize(size);
		progressBar.setMinimumSize(size);
		progressBar.setPreferredSize(size);

		EDITOR_LISTENER = new DownloadEditorListener();

		pauseButton = new JButton();
		pauseButton.addActionListener(EDITOR_LISTENER);
		pauseButton.setOpaque(false);
		pauseButton.setText("||");

		cancelButton = new JButton();
		cancelButton.addActionListener(EDITOR_LISTENER);
		cancelButton.setOpaque(false);
		cancelButton.setText("X");

		timeLabel = new JLabel();
		timeLabel.setFont(new Font("", Font.PLAIN, 9));
		timeLabel.setText("13 years remaining");

		resumeButton = new JButton();
		resumeButton.addActionListener(EDITOR_LISTENER);
		resumeButton.setVisible(false);
		resumeButton.setText("O");

		searchAgainButton = new JButton();
		searchAgainButton.addActionListener(EDITOR_LISTENER);
		searchAgainButton.setToolTipText(I18n.tr("Try Again"));
		searchAgainButton.setText("S");
		
		buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		//show color of underlying panel
		buttonPanel.setOpaque(false);
		buttonPanel.add(resumeButton);
		buttonPanel.add(pauseButton);
		buttonPanel.add(searchAgainButton);
		buttonPanel.add(cancelButton);

		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.weightx = 0;
		gbc.weighty = 1;
		gbc.gridheight = 3;
		add(iconLabel, gbc);
		
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.gridx = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.gridheight = 1;
		add(titleLabel, gbc);
		
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		add(progressBar, gbc);
		
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		add(statusLabel, gbc);
		
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.gridx = 2;
		gbc.gridy = 2;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		add(timeLabel, gbc);
		
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.gridx = 3;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		add(buttonPanel, gbc);		

	}
	

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		Component delegate = delegateRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		Component renderer = getCellComponent(table, value, isSelected, hasFocus, row, column);
		renderer.setBackground(delegate.getBackground());
		return renderer;
	}

	private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSel, int row, int col) {
		EDITOR_LISTENER.setDownloadItem((DownloadItem) value);
		Component renderer = getCellComponent(table, value, isSel, true, row, col);
		renderer.setBackground(rolloverBackground);
		renderer.setForeground(rolloverForeground);
		return renderer;
	}

	private Component getCellComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		DownloadItem item = (DownloadItem) value;
		titleLabel.setText(item.getTitle());
		switch (item.getCategory()) {
		case AUDIO:
			iconLabel.setIcon(audioIcon);
			break;
		case DOCUMENT:
			iconLabel.setIcon(documentIcon);
			break;
		case IMAGE:
			iconLabel.setIcon(imageIcon);
			break;
		case VIDEO:
			iconLabel.setIcon(videoIcon);
			break;
		default:
			iconLabel.setIcon(otherIcon);
		}
		//TODO time remaining
		//TODO : doubles in progress bar
		double totalSize = item.getTotalSize();
		double curSize = item.getCurrentSize();
		if (curSize < totalSize) {
			progressBar.setHidden(false);
			progressBar.setMaximum((int) item.getTotalSize());
			progressBar.setValue((int) item.getCurrentSize());
		} else {
			progressBar.setHidden(true);
		}
		statusLabel.setText(getMessage(item));
		if(item.getState() == DownloadState.DOWNLOADING){
		    timeLabel.setText(item.getRemainingDownloadTime());
		    timeLabel.setVisible(true);
		} else {
		    timeLabel.setVisible(false);
		}
		setButtonVisibility(item.getState());

		return this;
	}

	@Override
	public final void addCellEditorListener(CellEditorListener lis) {
		synchronized (listeners) {
			if (!listeners.contains(lis))
				listeners.add(lis);
		}
	}

	@Override
	public final void cancelCellEditing() {
		synchronized (listeners) {
			for (int i = 0, N = listeners.size(); i < N; i++) {
				listeners.get(i).editingCanceled(new ChangeEvent(this));
			}
		}
	}

	@Override
	public final Object getCellEditorValue() {
		return null;
	}

	@Override
	public boolean isCellEditable(EventObject e) {
		return true;
	}

	@Override
	public final void removeCellEditorListener(CellEditorListener lis) {
		synchronized (listeners) {
			if (listeners.contains(lis))
				listeners.remove(lis);
		}
	}

	@Override
	public final boolean shouldSelectCell(EventObject e) {
		return true;
	}

	@Override
	public final boolean stopCellEditing() {
		synchronized (listeners) {
			for (int i = 0, N = listeners.size(); i < N; i++) {
				listeners.get(i).editingStopped(new ChangeEvent(this));
			}
		}
		return true;
	}

	private void setButtonVisibility(DownloadState state) {
		pauseButton.setVisible(state.isPausable());
		resumeButton.setVisible(state.isResumable());
		cancelButton.setVisible(state.isCancellable());
		searchAgainButton.setVisible(state.isSearchAgainable());
		if (state == DownloadState.DOWNLOADING) {
			progressBar.setEnabled(true);
			progressBar.setHidden(false);
		} else if (state == DownloadState.PAUSED) {
			progressBar.setEnabled(false);
			progressBar.setHidden(false);
		} else {
			progressBar.setHidden(true);
		}
	}

	private class DownloadEditorListener implements ActionListener {
		private DownloadItem downloadItem;

		public void setDownloadItem(DownloadItem item) {
			downloadItem = item;
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == cancelButton) {
				downloadItem.cancel();
			} else if (e.getSource() == pauseButton) {
				downloadItem.pause();
			} else if (e.getSource() == resumeButton) {
				downloadItem.resume();
			} else if (e.getSource() == searchAgainButton) {
				downloadItem.resume();
			}
			cancelCellEditing();
		}

	}

	private String getMessage(DownloadItem item) {
		switch (item.getState()) {
		case CANCELLED:
			return I18n.tr("Cancelled");
        case FINISHING:
            return I18n.tr("Finishing download...");
            //TODO: correct time for finishing
            //return I18n.tr("Finishing download, {0} remaining", item.getRemainingStateTime());
		case DONE:
			return I18n.tr("Done");
		case CONNECTING:
			return I18n.tr("Connecting...");
		case DOWNLOADING:
			//TODO : uploaders in DownloadItem & plural
			return I18n.tr("Downloading {0} of {1} ({2}) from {3} people", 
							GuiUtils.toUnitbytes((long)item.getCurrentSize()), GuiUtils.toUnitbytes((long)item.getTotalSize()),
							GuiUtils.rate2speed(item.getDownloadSpeed()), item.getDownloadSourceCount() );
		case STALLED:
			return I18n
					.tr("Stalled - {0} of {1} ({2}%) from 0 people",
							new Object[] { GuiUtils.toUnitbytes((long)item.getCurrentSize()),
					        GuiUtils.toUnitbytes((long)item.getTotalSize() ), item.getPercentComplete()});
		case ERROR:
			switch (item.getErrorState()) {
			//TODO: all error messages should link to: http://wiki.limewire.org/index.php?title=User_Guide_Download
            case CORRUPT_FILE:
                return I18n.tr("Unable to download: the file is corrupted");
            case DISK_PROBLEM:
                return I18n.tr("Unable to download: there is a disk problem");
            case FILE_NOT_SHARABLE:
                return I18n.tr("Unable to download: the file is not sharable ");
            case UNABLE_TO_CONNECT:
                return I18n.tr("Unable to download: trouble connecting to people");
            }
		case PAUSED:
			return I18n.tr("Paused - {0} of {1} ({2}%)", new Object[] {
			        GuiUtils.toUnitbytes((long)item.getCurrentSize()), GuiUtils.toUnitbytes((long)item.getTotalSize()),
			        item.getPercentComplete()});
        case LOCAL_QUEUED:
            return I18n.tr("Queued – About x seconds before download can begin");
        case REMOTE_QUEUED:
            return I18n.tr("Queued – {0} people ahead of you for this file", item.getQueuePosition());
		default:
            return item.getState().toString();
		}
		
	}

}
