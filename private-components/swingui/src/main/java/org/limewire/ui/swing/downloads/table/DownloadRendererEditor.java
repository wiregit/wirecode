package org.limewire.ui.swing.downloads.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.DownloadItem.ErrorState;
import org.limewire.ui.swing.downloads.LimeProgressBar;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 * Renderer and editor for DownloadTables. Editors must be initialized with 
 * <code>editor.initializeEditor(downloadItems)</code>
 */
public class DownloadRendererEditor extends JPanel implements
		TableCellRenderer, TableCellEditor {

	
	//The item being edited by the editor
	private DownloadItem editItem = null;
    
    private DownloadActionHandler actionHandler;
		
	private DownloadButtonPanel buttonPanel;
	private CategoryIconLabel iconLabel;
	private JLabel titleLabel;
	private JLabel statusLabel;
	private LimeProgressBar progressBar;
    private JButton linkButton;
	private JLabel timeLabel;
	private DownloadEditorListener editorListener;
    
    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();

    @Resource
    private Icon warningIcon;
  
    private List<JComponent> textComponents = new ArrayList<JComponent>();

	/**
	 * Create the panel
	 */
	public DownloadRendererEditor() {
		GuiUtils.assignResources(this);
		iconLabel = new CategoryIconLabel(CategoryIconLabel.Size.LARGE);

		titleLabel = new JLabel();
		titleLabel.setFont(new Font("", Font.BOLD, 18));
		titleLabel.setText("Title - title - title");
		textComponents.add(titleLabel);

		statusLabel = new JLabel();
		statusLabel.setFont(new Font("", Font.PLAIN, 9));
		statusLabel.setText("Downloading 2MB of 4322 MPG from Tom");
        textComponents.add(statusLabel);

		progressBar = new LimeProgressBar();
		Dimension size = new Dimension(350, 25);
		progressBar.setMaximumSize(size);
		progressBar.setMinimumSize(size);
		progressBar.setPreferredSize(size);

		editorListener = new DownloadEditorListener();
		
		linkButton = new JXHyperlink();
		linkButton.setActionCommand(DownloadActionHandler.LINK_COMMAND);
        linkButton.setText("link button");
        linkButton.addActionListener(editorListener);

		timeLabel = new JLabel();
		timeLabel.setFont(new Font("", Font.PLAIN, 9));
		timeLabel.setText("13 years remaining");
		
		buttonPanel = new DownloadButtonPanel(editorListener);
		//show color of underlying panel
		buttonPanel.setOpaque(false);

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
		gbc.gridwidth = 3;
		add(progressBar, gbc);
		
		gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx += gbc.gridwidth;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        add(buttonPanel, gbc);  
		
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		add(statusLabel, gbc);
		
		gbc.gridx++;
		add(linkButton, gbc);
		
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.gridx++;
		gbc.gridy = 2;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		add(timeLabel, gbc);	
	}	

	 
	@Override
	public void setForeground(Color color){
	    super.setForeground(color);
	    for(Component component : getComponents()){
            component.setForeground(color);
        }
	}
	
	@Override
	public void addMouseListener(MouseListener listener){
	    super.addMouseListener(listener);
	    for(Component component : getComponents()){
	        component.addMouseListener(listener);
	    }	     
	}
	
	@Override
    public void removeMouseListener(MouseListener listener){
        super.removeMouseListener(listener);
        for(Component component : getComponents()){
            component.removeMouseListener(listener);
        } 
    }

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
        return getCellComponent(table, value, isSelected, hasFocus, row, column);
    }



	@Override
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSel, int row, int col) {
	    editItem = (DownloadItem) value;
		return getCellComponent(table, value, isSel, true, row, col);
	}

	private DownloadRendererEditor getCellComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		DownloadItem item = (DownloadItem) value;
		updateComponent(this, item);
		return this;
	}
	

    /**
     * Binds editor to downloadItems so that the editor automatically updates
     * when downloadItems changes and popup menus work.  This is required for Editors
     */
	public void initializeEditor(EventList<DownloadItem> downloadItems) {
	    actionHandler = new DownloadActionHandler(downloadItems);
        downloadItems.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                // TODO: only update if relevant downloadItem was updated
                if (isVisible()) {
                    updateEditor();
                }
            }
        });
        
      
    }
	
	private void updateEditor(){
	    if (editItem != null) {
            updateComponent(this, editItem);
        }
	}
	
	private void updateComponent(DownloadRendererEditor editor, DownloadItem item){
	    editor.titleLabel.setText(item.getTitle());
	    
        if(item.getState() == DownloadState.ERROR || item.getState() == DownloadState.STALLED ){
            editor.iconLabel.setIcon(warningIcon);
        } else {
            editor.iconLabel.setIcon(item.getCategory());           
        }
        
        long totalSize = item.getTotalSize();
        long curSize = item.getCurrentSize();
        if (curSize < totalSize) {
            editor.progressBar.setHidden(false);
            editor.progressBar.setMaximum((int) item.getTotalSize());
            editor.progressBar.setValue((int) item.getCurrentSize());
        } else {
            progressBar.setHidden(true);
        }
        editor.statusLabel.setText(getMessage(item));
        if(item.getState() == DownloadState.DOWNLOADING){
            editor.timeLabel.setText(CommonUtils.seconds2time(item.getRemainingDownloadTime()));
            editor.timeLabel.setVisible(true);
        } else {
            editor.timeLabel.setVisible(false);
        }
        updateButtons(item);        

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

	private void updateButtons(DownloadItem item) {
	    DownloadState state = item.getState();
		buttonPanel.updateButtons(state);
		if(state == DownloadState.ERROR){
		    linkButton.setVisible(true);
		    linkButton.setText("<html><u>" + getErrorMessage(item.getErrorState()) + "</u></html>");
		} else {
		    linkButton.setVisible(false);
		}
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

		public void actionPerformed(ActionEvent e) {
		    actionHandler.performAction(e.getActionCommand(), editItem);
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
							GuiUtils.toUnitbytes(item.getCurrentSize()), GuiUtils.toUnitbytes(item.getTotalSize()),
							GuiUtils.rate2speed(item.getDownloadSpeed()), item.getDownloadSourceCount() );
		case STALLED:
			return I18n
					.tr("Stalled - {0} of {1} ({2}%) from 0 people",
							new Object[] { GuiUtils.toUnitbytes(item.getCurrentSize()),
					        GuiUtils.toUnitbytes(item.getTotalSize() ), item.getPercentComplete()});
		case ERROR:			
            return I18n.tr("Unable to download: ");
		case PAUSED:
			return I18n.tr("Paused - {0} of {1} ({2}%)", new Object[] {
			        GuiUtils.toUnitbytes(item.getCurrentSize()), GuiUtils.toUnitbytes(item.getTotalSize()),
			        item.getPercentComplete()});
        case LOCAL_QUEUED:
            long queueTime = item.getRemainingQueueTime();
            if(queueTime == DownloadItem.UNKNOWN_TIME){
                return I18n.tr("Queued - remaining time unknown");                
            } else {
                return I18n.tr("Queued - About {0} before download can begin", CommonUtils.seconds2time(queueTime));
            }
        case REMOTE_QUEUED:
            return I18n.tr("Queued - {0} people ahead of you for this file", item.getQueuePosition());
		default:
		    throw new IllegalArgumentException("Unknown DownloadState: " + item.getState());
		}
		
	}
	
	private String getErrorMessage(ErrorState errorState){
	    switch (errorState) {
        case CORRUPT_FILE:
            return I18n.tr("the file is corrupted");
        case DISK_PROBLEM:
            return I18n.tr("there is a disk problem");
        case FILE_NOT_SHARABLE:
            return I18n.tr("the file is not sharable ");
        case UNABLE_TO_CONNECT:
            return I18n.tr("trouble connecting to people");
        default:
            throw new IllegalArgumentException("Unknown ErrorState: " + errorState);
        }
	}
	

}
