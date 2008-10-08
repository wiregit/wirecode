package org.limewire.ui.swing.downloads.table;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.LimeProgressBar;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
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
		
    
    private JPanel mainFrame;
    
    private CardLayout statusViewLayout;
    private final static String FULL_LAYOUT = "Full download display";
    private JPanel fullPanel;
    private final static String MIN_LAYOUT = "Condensed download display";
    private JPanel minPanel;
    

    private DownloadButtonPanel minButtonPanel;
    private CategoryIconLabel minIconLabel;
    private JLabel minTitleLabel;
    private JLabel minStatusLabel;
    private JXHyperlink minLinkButton;
    
	private DownloadButtonPanel fullButtonPanel;
	private CategoryIconLabel fullIconLabel;
	private JLabel fullTitleLabel;
	private JLabel fullStatusLabel;
	private LimeProgressBar fullProgressBar;
    private JLabel fullTimeLabel;
	
	private DownloadEditorListener editorListener;
    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();

    @Resource
    private Icon warningIcon;
    
    private static final int PROGRESS_BAR_WIDTH = 538;
    
    private Color itemLabelColour     = new Color(0x21,0x52,0xa6);
    private Color statusLabelColour   = new Color(0x31,0x31,0x31);
    private Color stalledLabelColour  = new Color(0xb3,0x1c,0x20);
    private Color finishedLabelColour = new Color(0x16,0x7e,0x11);
    private Color linkColour          = new Color(0x2b,0x5b,0xaa);
    private Color progressBarBorderColour = new Color(0x8a,0x8a,0x8a);
    
    private static final Font statusPlain = new Font("Arial", Font.PLAIN, 10);
    private static final Font statusBold = new Font("Arial", Font.BOLD, 10);
    
    private List<JComponent> textComponents = new ArrayList<JComponent>();

    
    
    private void initComponents() {

        editorListener = new DownloadEditorListener();
        
        minIconLabel = new CategoryIconLabel(CategoryIconLabel.Size.SMALL);
        
        minTitleLabel = new JLabel();
        minTitleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        minTitleLabel.setForeground(itemLabelColour);
        textComponents.add(minTitleLabel);

        minStatusLabel = new JLabel();
        minStatusLabel.setFont(statusPlain);
        minStatusLabel.setForeground(statusLabelColour);
        textComponents.add(minStatusLabel);

        minButtonPanel = new DownloadButtonPanel(editorListener);
        minButtonPanel.setOpaque(false);

        minLinkButton = new JXHyperlink();
        minLinkButton.setActionCommand(DownloadActionHandler.TRY_AGAIN_COMMAND);
        minLinkButton.addActionListener(editorListener);
        minLinkButton.setForeground(linkColour);
        minLinkButton.setFont(statusPlain);
                                
        fullIconLabel = new CategoryIconLabel(CategoryIconLabel.Size.SMALL);

        fullTitleLabel = new JLabel();
        fullTitleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        fullTitleLabel.setForeground(itemLabelColour);
        textComponents.add(fullTitleLabel);

        fullStatusLabel = new JLabel();
        fullStatusLabel.setFont(statusPlain);
        fullStatusLabel.setForeground(statusLabelColour);
        textComponents.add(fullStatusLabel);

        fullProgressBar = new LimeProgressBar();
        Dimension size = new Dimension(PROGRESS_BAR_WIDTH, 16);
        fullProgressBar.setMaximumSize(size);
        fullProgressBar.setMinimumSize(size);
        fullProgressBar.setPreferredSize(size);
        fullProgressBar.setBorder(BorderFactory.
                createLineBorder(progressBarBorderColour));
        

        fullTimeLabel = new JLabel();
        fullTimeLabel.setFont(statusPlain);
        
        fullButtonPanel = new DownloadButtonPanel(editorListener);
        fullButtonPanel.setOpaque(false);
    }
    
    private void createMinView() {
        GridBagConstraints gbc = new GridBagConstraints();

        Insets insets = new Insets(0,10,0,0);
        
        gbc.insets = insets;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridheight = 3;
        minPanel.add(minIconLabel, gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        minPanel.add(minTitleLabel, gbc);
        
        gbc.insets = new Insets(0,30,0,0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        minPanel.add(minButtonPanel, gbc);  
        
        gbc.insets = insets;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 0;
        minPanel.add(minStatusLabel, gbc);
       
        gbc.insets = new Insets(0,0,0,0);
        gbc.gridx++;
        minPanel.add(minLinkButton, gbc);
        
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 3;
        gbc.gridheight = 0;
        minPanel.add(Box.createHorizontalStrut(PROGRESS_BAR_WIDTH-16), gbc);
            
    }
    
    private void createFullView() {
        GridBagConstraints gbc = new GridBagConstraints();

        Insets insets = new Insets(0,10,0,0);
        
        gbc.insets = insets;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        fullPanel.add(fullIconLabel, gbc);
        
        gbc.insets = new Insets(0,5,0,0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        fullPanel.add(fullTitleLabel, gbc);
        
        gbc.insets = new Insets(3,10,0,0);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 3;
        fullPanel.add(fullProgressBar, gbc);
        
        gbc.insets = new Insets(0,30,0,0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.gridx += gbc.gridwidth;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        fullPanel.add(fullButtonPanel, gbc);  
        
        gbc.insets = insets;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        fullPanel.add(fullStatusLabel, gbc);
        
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        fullPanel.add(fullTimeLabel, gbc);    
    }
    
	/**
	 * Create the panel
	 */
	public DownloadRendererEditor() {
		GuiUtils.assignResources(this);

		statusViewLayout = new CardLayout();
		mainFrame = new JPanel(statusViewLayout);
		
		fullPanel = new JPanel(new GridBagLayout());
		minPanel  = new JPanel(new GridBagLayout());
		
		fullPanel.setOpaque(false);
	    minPanel.setOpaque(false);
	    mainFrame.setOpaque(false);

	    Border blankBorder = BorderFactory.createEmptyBorder();
	    fullPanel.setBorder(blankBorder);
        minPanel.setBorder(blankBorder);
        mainFrame.setBorder(blankBorder);
        this.setBorder(blankBorder);

	    initComponents();
	    createFullView();
	    createMinView();
        
		this.setLayout(new BorderLayout());
		
		mainFrame.add(fullPanel, FULL_LAYOUT);
	    mainFrame.add( minPanel, MIN_LAYOUT);
	    this.statusViewLayout.show(mainFrame, FULL_LAYOUT);
	        
		
	    this.add(this.mainFrame, BorderLayout.WEST);
		
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
	
	private void updateMin(DownloadRendererEditor editor, DownloadItem item) {
        
        editor.minTitleLabel.setText(item.getTitle());
        
        switch (item.getState()) {
        
        
        case ERROR :
        case STALLED :
            
            editor.minIconLabel.setIcon(warningIcon);
            editor.minStatusLabel.setForeground(stalledLabelColour);
            editor.minStatusLabel.setFont(statusBold);
            
            break;
            
        case DONE :
            
            editor.minIconLabel.setIcon(item.getCategory());
            editor.minStatusLabel.setForeground(finishedLabelColour);
            editor.minStatusLabel.setFont(statusBold);
            
            break;
            
        default :
            editor.minIconLabel.setIcon(item.getCategory());     
            editor.minStatusLabel.setForeground(statusLabelColour);
            editor.minStatusLabel.setFont(statusPlain);
            
        }
        
        
        editor.minStatusLabel.setText(getMessage(item));
        
        updateButtonsMin(item);      
    }
    
	
	
	private void updateFull(DownloadRendererEditor editor, DownloadItem item) {
	    
	    editor.fullIconLabel.setIcon(item.getCategory());
        editor.fullTitleLabel.setText(item.getTitle());
        
        long totalSize = item.getTotalSize();
        long curSize = item.getCurrentSize();
        if (curSize < totalSize) {
            editor.fullProgressBar.setHidden(false);
            editor.fullProgressBar.setMaximum((int) item.getTotalSize());
            editor.fullProgressBar.setValue((int) item.getCurrentSize());
        }
        
        editor.fullStatusLabel.setText(getMessage(item));
        
        editor.fullTimeLabel.setText(CommonUtils.seconds2time(item.getRemainingDownloadTime()));
        editor.fullTimeLabel.setVisible(true);
         
        
        updateButtonsFull(item);      
	}
	
	private void updateComponent(DownloadRendererEditor editor, DownloadItem item){
	    if (item.getState() == DownloadState.DOWNLOADING) {
	        editor.statusViewLayout.show(mainFrame, FULL_LAYOUT);
	        updateFull(editor, item);
	    } 
	    else {
	        editor.statusViewLayout.show(mainFrame, MIN_LAYOUT);
	        updateMin(editor, item);
	    }
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

	   
    private void updateButtonsMin(DownloadItem item) {
        DownloadState state = item.getState();
        minButtonPanel.updateButtons(state);
        
        switch (state) {
        
            case ERROR :
                minLinkButton.setVisible(true);
                minLinkButton.setText("<html><u>" + I18n.tr(item.getErrorState().getMessage()) + "</u></html>");
                
                break;
                
            case STALLED :

                minLinkButton.setVisible(true);
                minLinkButton.setText("<html><u>Refresh Now</u></html>");

                break;
                
            default:
                minLinkButton.setVisible(false);
        }
    }
	
	private void updateButtonsFull(DownloadItem item) {
	    DownloadState state = item.getState();
		fullButtonPanel.updateButtons(state);

		if (state == DownloadState.DOWNLOADING) {
			fullProgressBar.setEnabled(true);
			fullProgressBar.setHidden(false);
		} else if (state == DownloadState.PAUSED) {
			fullProgressBar.setEnabled(false);
			fullProgressBar.setHidden(false);
		} else {
			fullProgressBar.setHidden(true);
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
			//TODO : uploaders in DownloadItem & plural, not sure if this TODO is addressed
		    // with adding proper plural handling?
		    // {0}: current file size, {2} final file size, {3}, number of people
			return I18n.trn("Downloading {0} of {1} ({2}) from {3} person",
			        "Downloading {0} of {1} ({2}) from {3} people",
			        item.getDownloadSourceCount(),
			        GuiUtils.toUnitbytes(item.getCurrentSize()), 
			        GuiUtils.toUnitbytes(item.getTotalSize()),
			        GuiUtils.rate2speed(item.getDownloadSpeed()), 
			        item.getDownloadSourceCount());
		case STALLED:
			return I18n
					.tr("Stalled - ");
		case ERROR:			
            return I18n.tr("Unable to download: ");
		case PAUSED:
			return I18n.tr("Paused - {0} of {1} ({2}%)", 
			        GuiUtils.toUnitbytes(item.getCurrentSize()), GuiUtils.toUnitbytes(item.getTotalSize()),
			        item.getPercentComplete());
        case LOCAL_QUEUED:
            long queueTime = item.getRemainingQueueTime();
            if(queueTime == DownloadItem.UNKNOWN_TIME){
                return I18n.tr("Queued - remaining time unknown");                
            } else {
                return I18n.tr("Queued - About {0} before download can begin", CommonUtils.seconds2time(queueTime));
            }
        case REMOTE_QUEUED:
            return I18n.trn("Queued - {0} person ahead of you for this file",
                    "Queued - {0} people ahead of you for this file",
                    item.getQueuePosition(), item.getQueuePosition());
		default:
		    throw new IllegalArgumentException("Unknown DownloadState: " + item.getState());
		}
		
	}
}
