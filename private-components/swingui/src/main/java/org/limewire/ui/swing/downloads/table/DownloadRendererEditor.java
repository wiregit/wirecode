package org.limewire.ui.swing.downloads.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
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
import org.limewire.ui.swing.util.SwingUtils;

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

    private static final String ERROR_URL = "http://wiki.limewire.org/index.php?title=User_Guide_Download";

	
	//The item being edited by the editor
	private DownloadItem editItem = null;

    private DownloadItem menuEditItem = null;
		
	private JPanel buttonPanel;
	private JLabel iconLabel;
	private JLabel titleLabel;
	private JLabel statusLabel;
	private LimeProgressBar progressBar;
    private JButton pauseButton;
    private JButton cancelButton;
    private JButton resumeButton;
    private JButton tryAgainButton;
    private JButton linkButton;
    private JMenuItem launchMenuItem;
    private JMenuItem previewMenuItem;
    private JMenuItem removeMenuItem;
    private JMenuItem pauseMenuItem;
    private JMenuItem locateMenuItem;
    private JMenuItem cancelMenuItem;
    private JMenuItem resumeMenuItem;
    private JMenuItem tryAgainMenuItem;
    private JMenuItem propertiesMenuItem;
	private JLabel timeLabel;
	private DownloadEditorListener editorListener;
	private MenuListener menuListener;
    private JPopupMenu popupMenu = new JPopupMenu();;
    
    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();

    private EventList<DownloadItem> downloadItems;

    private final static String PAUSE_COMMAND = "pause";
    private final static String CANCEL_COMMAND = "cancel";
    private final static String RESUME_COMMAND = "resume";
    private final static String TRY_AGAIN_COMMAND = "try again";
    private final static String LAUNCH_COMMAND = "launch";
    private final static String PREVIEW_COMMAND = "preview";
    private final static String REMOVE_COMMAND = "remove";
    private final static String LOCATE_COMMAND = "locate";
    private final static String PROPERTIES_COMMAND = "properties";
    private final static String LINK_COMMAND = "link";

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
    @Resource
    private Icon warningIcon;
    

    @Resource
    private Icon pauseIcon;    
    @Resource
    private Icon pauseIconPressed;    
    @Resource
    private Icon pauseIconRollover;
    @Resource
    private Icon cancelIcon;
    @Resource
    private Icon cancelIconPressed;
    @Resource
    private Icon cancelIconRollover;
    @Resource
    private Icon resumeIcon;
    @Resource
    private Icon resumeIconPressed;
    @Resource
    private Icon resumeIconRollover;
    @Resource
    private Icon tryAgainIcon;
    @Resource
    private Icon tryAgainIconPressed;
    @Resource
    private Icon tryAgainIconRollover;
    
    private List<JComponent> textComponents = new ArrayList<JComponent>();

	/**
	 * Create the panel
	 */
	public DownloadRendererEditor() {
		GuiUtils.assignResources(this);
		iconLabel = new JLabel();

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
		menuListener = new MenuListener();
		
		linkButton = new JXHyperlink();
		linkButton.setActionCommand(LINK_COMMAND);
        linkButton.setText("link button");
        linkButton.addActionListener(editorListener);


		pauseButton = createIconButton(pauseIcon, pauseIconRollover, pauseIconPressed);
		pauseButton.setActionCommand(PAUSE_COMMAND);
		pauseButton.addActionListener(editorListener);
		pauseButton.setToolTipText(I18n.tr("Pause"));
		
		pauseMenuItem = new JMenuItem(I18n.tr("Pause"));
        pauseMenuItem.setActionCommand(PAUSE_COMMAND);
		pauseMenuItem.addActionListener(menuListener);
		

		cancelButton = createIconButton(cancelIcon, cancelIconRollover, cancelIconPressed);
		cancelButton.setActionCommand(CANCEL_COMMAND);
		cancelButton.addActionListener(editorListener);
        cancelButton.setToolTipText(I18n.tr("Cancel Download"));
		
		cancelMenuItem = new JMenuItem(I18n.tr("Cancel Download"));
        cancelMenuItem.setActionCommand(CANCEL_COMMAND);
		cancelMenuItem.addActionListener(menuListener);

		timeLabel = new JLabel();
		timeLabel.setFont(new Font("", Font.PLAIN, 9));
		timeLabel.setText("13 years remaining");

		resumeButton = createIconButton(resumeIcon, resumeIconRollover, resumeIconPressed);
		resumeButton.setActionCommand(RESUME_COMMAND);
		resumeButton.addActionListener(editorListener);
		resumeButton.setVisible(false);
        resumeButton.setToolTipText(I18n.tr("Resume"));
		
		resumeMenuItem = new JMenuItem(I18n.tr("Resume"));
		resumeMenuItem.setActionCommand(RESUME_COMMAND);
		resumeMenuItem.addActionListener(menuListener);

		tryAgainButton = createIconButton(tryAgainIcon, tryAgainIconRollover, tryAgainIconPressed);
		tryAgainButton.setActionCommand(TRY_AGAIN_COMMAND);
		tryAgainButton.addActionListener(editorListener);
		tryAgainButton.setToolTipText(I18n.tr("Try Again"));
		
		tryAgainMenuItem = new JMenuItem(I18n.tr("Try Again"));
		tryAgainMenuItem.setActionCommand(TRY_AGAIN_COMMAND);
		tryAgainMenuItem.addActionListener(menuListener);

        launchMenuItem = new JMenuItem(I18n.tr("Launch file"));
        launchMenuItem.setActionCommand(LAUNCH_COMMAND);
        launchMenuItem.addActionListener(menuListener);
        
        removeMenuItem = new JMenuItem(I18n.tr("Remove from list"));
        removeMenuItem.setActionCommand(REMOVE_COMMAND);
        removeMenuItem.addActionListener(menuListener);
        
        locateMenuItem = new JMenuItem(I18n.tr("Locate file"));
        locateMenuItem.setActionCommand(LOCATE_COMMAND);
        locateMenuItem.addActionListener(menuListener);
        
        propertiesMenuItem = new JMenuItem(I18n.tr("Properties"));
        propertiesMenuItem.setActionCommand(PROPERTIES_COMMAND);
        propertiesMenuItem.addActionListener(menuListener);
        
        previewMenuItem =  new JMenuItem(I18n.tr("Preview File"));
        previewMenuItem.setActionCommand(PREVIEW_COMMAND);
        previewMenuItem.addActionListener(menuListener);
		
		buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		//show color of underlying panel
		buttonPanel.setOpaque(false);
		buttonPanel.add(resumeButton);
		buttonPanel.add(pauseButton);
		buttonPanel.add(tryAgainButton);
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
	
	public void showPopupMenu(Component c, int x, int y){	    
	    menuEditItem = editItem;
	    popupMenu.removeAll();
	    DownloadState state = menuEditItem.getState();
	   
	    switch(state){
	    
	    case DONE:
	        popupMenu.add(launchMenuItem);
	        popupMenu.add(locateMenuItem);
	        popupMenu.add(removeMenuItem);
	        break;
	        
	    case CONNECTING:
        case ERROR:
        case FINISHING:
        case LOCAL_QUEUED:
        case REMOTE_QUEUED:
	        if (menuEditItem.getCurrentSize() > 0){
	            if (menuEditItem.isLaunchable()) {
                    popupMenu.add(previewMenuItem);
                }
	            popupMenu.add(locateMenuItem);
	            popupMenu.add(new JSeparator());
	        } 
                popupMenu.add(cancelMenuItem);
            break;
            
	    case DOWNLOADING:
            popupMenu.add(pauseMenuItem);
            if (menuEditItem.isLaunchable()) {
                popupMenu.add(previewMenuItem);
            }
            popupMenu.add(locateMenuItem);
            popupMenu.add(new JSeparator());
            popupMenu.add(cancelMenuItem);
            break;
            
            
	    case PAUSED:
            popupMenu.add(resumeMenuItem);
            if (menuEditItem.getCurrentSize() > 0){
                if (menuEditItem.isLaunchable()) {
                    popupMenu.add(previewMenuItem);
                }
                popupMenu.add(locateMenuItem);
                popupMenu.add(new JSeparator());
            } 
                popupMenu.add(cancelMenuItem);
            break;
            
	    case STALLED:
	        popupMenu.add(tryAgainMenuItem);
	        if (menuEditItem.getCurrentSize() > 0){
	            if (menuEditItem.isLaunchable()) {
                    popupMenu.add(previewMenuItem);
                }
                popupMenu.add(locateMenuItem);
                popupMenu.add(new JSeparator());
            } 
                popupMenu.add(cancelMenuItem);
            break;
	    }
	    
	    popupMenu.add(new JSeparator());
	    popupMenu.add(propertiesMenuItem);
	    
        popupMenu.show(c, x, y);
	}
	
	/**
     * Binds editor to downloadItems so that the editor automatically updates
     * when downloadItems changes and popup menus work.  This is required for Editors
     */
	public void initializeEditor(EventList<DownloadItem> downloadItems) {
	    this.downloadItems = downloadItems;
        downloadItems.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                //TODO: only update if relevant downloadItem was updated
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (isVisible()) {
                            updateEditor();
                        }
                    }
                });
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
            switch (item.getCategory()) {
            case AUDIO:
                editor.iconLabel.setIcon(audioIcon);
                break;
            case DOCUMENT:
                editor.iconLabel.setIcon(documentIcon);
                break;
            case IMAGE:
                editor.iconLabel.setIcon(imageIcon);
                break;
            case VIDEO:
                editor.iconLabel.setIcon(videoIcon);
                break;
            default:
                editor.iconLabel.setIcon(otherIcon);
            }
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

	public boolean isItemMenuVisible(DownloadItem item) {
        return item.equals(menuEditItem) && popupMenu.isVisible();
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
		pauseButton.setVisible(state.isPausable());
		resumeButton.setVisible(state.isResumable());
		cancelButton.setVisible(state.isCancellable());
		tryAgainButton.setVisible(state.isSearchAgainable());
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

	//TODO: combine these listeners
	private class DownloadEditorListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			performAction(e.getActionCommand(), editItem);
			cancelCellEditing();
		}

	}
	
	private class MenuListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            performAction(e.getActionCommand(), menuEditItem);
            cancelCellEditing();
        }

    }
	
	private void performAction(String actionCommmand, DownloadItem item){
	    if (actionCommmand == CANCEL_COMMAND) {
	        item.cancel();
        } else if (actionCommmand == PAUSE_COMMAND) {
            item.pause();
        } else if (actionCommmand == RESUME_COMMAND) {
            item.resume();
        } else if (actionCommmand == TRY_AGAIN_COMMAND) {
            item.resume();
        } else if (actionCommmand == LINK_COMMAND){
            GuiUtils.openURL(ERROR_URL);
        } else if (actionCommmand == PREVIEW_COMMAND){
            //TODO preview
            throw new RuntimeException("Implement "+ actionCommmand + " " + item.getTitle() + "!");
        } else if (actionCommmand == LOCATE_COMMAND){
            //TODO locate
            throw new RuntimeException("Implement "+ actionCommmand  + " " + item.getTitle() + "!");
        } else if (actionCommmand == LAUNCH_COMMAND){
            //TODO launch
            throw new RuntimeException("Implement "+ actionCommmand  + " " + item.getTitle() + "!");
        } else if (actionCommmand == PROPERTIES_COMMAND){
            //TODO properties
            throw new RuntimeException("Implement "+ actionCommmand  + " " + item.getTitle() + "!");
        } else if (actionCommmand == REMOVE_COMMAND){
            downloadItems.remove(item);
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
	
	//TODO: move this out of renderer
	private JButton createIconButton(Icon icon, Icon rolloverIcon, Icon pressedIcon) {
        JButton button = new JButton();
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setIcon(icon);
        button.setRolloverIcon(rolloverIcon);
        button.setPressedIcon(pressedIcon);
        button.setHideActionText(true);
        return button;
    }

}
