package org.limewire.ui.swing.downloads;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimeProgressBar;
import org.limewire.ui.swing.components.LimeProgressBarFactory;
import org.limewire.ui.swing.dnd.DownloadableTransferHandler;
import org.limewire.ui.swing.downloads.table.AbstractDownloadTable;
import org.limewire.ui.swing.downloads.table.DownloadActionHandler;
import org.limewire.ui.swing.downloads.table.DownloadActionHandlerFactory;
import org.limewire.ui.swing.downloads.table.DownloadPopupHandler;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.downloads.table.DownloadTableCell;
import org.limewire.ui.swing.downloads.table.DownloadTableEditor;
import org.limewire.ui.swing.downloads.table.DownloadTableRenderer;
import org.limewire.ui.swing.downloads.table.HorizontalDownloadTableModel;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.SimpleNavSelectable;
import org.limewire.ui.swing.painter.BarPainterFactory;
import org.limewire.ui.swing.table.TableColumnDoubleClickHandler;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.EnabledListener;
import org.limewire.ui.swing.util.EnabledListenerList;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.ForceInvisibleComponent;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.ui.swing.util.VisibilityListener;
import org.limewire.ui.swing.util.VisibilityListenerList;
import org.limewire.util.CommonUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.RangeList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class DownloadSummaryPanel extends JXPanel implements ForceInvisibleComponent {

    /**
     * Number of download items displayed in the table
     */
	private static final int NUMBER_DISPLAYED = 5;

	//private static final int LEFT_MARGIN = 25;

	private final LimeProgressBarFactory progressBarFactory;
    private final Navigator navigator;
    private final TrayNotifier notifier;
	
	private final VisibilityListenerList visibilityListenerList = new VisibilityListenerList();
    private final EnabledListenerList enabledListenerList = new EnabledListenerList();
    private AbstractDownloadTable table;
    private final HorizontalDownloadTableModel horizontalTableModel;

    private JLabel header;
	private JXHyperlink moreButton;
	private EventList<DownloadItem> allList;
    private RangeList<DownloadItem> chokeList;    

    private JXHyperlink minimizeButton;
    private JXHyperlink clearButton;
    private JLabel clearDivider;

    @Resource private Font seeAllFont; 
    @Resource private Color blueForeground; 
    @Resource private Color orangeForeground; 
    @Resource private Color greenForeground; 
    @Resource private Font itemFont; 
    @Resource private Color fontColor; 
    @Resource private Icon downloadIcon;
    @Resource private Icon downloadCompletedIcon;
    @Resource private int panelHeight;
    @Resource private int nameLabelWidth;
    @Resource private Font minimizeFont;
    @Resource private Color minimizeColor;

    private DownloadMediator downloadMediator;


    
    @Inject
	public DownloadSummaryPanel(DownloadListManager downloadListManager, DownloadMediator downloadMediator, MainDownloadPanel mainDownloadPanel, 
	        Navigator navigator, LimeProgressBarFactory progressBarFactory, BarPainterFactory barPainterFactory, SaveLocationExceptionHandler saveLocationExceptionHandler,
	        DownloadActionHandlerFactory downloadActionHandlerFactory, TrayNotifier notifier) {
	    this.navigator = navigator;        
        this.progressBarFactory = progressBarFactory;
        this.notifier = notifier;
        this.allList = downloadMediator.getDownloadList();
        this.downloadMediator = downloadMediator;
        
        GuiUtils.assignResources(this);        
        
        setTransferHandler(new DownloadableTransferHandler(downloadListManager, saveLocationExceptionHandler));	    
        
        setBackgroundPainter(barPainterFactory.createDownloadSummaryBarPainter());
                
		header = new JLabel(downloadIcon);

        
        navigator.createNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME, mainDownloadPanel);	
		
		chokeList = GlazedListsFactory.rangeList(allList);
		chokeList.setHeadRange(0, NUMBER_DISPLAYED);
		horizontalTableModel = new HorizontalDownloadTableModel(allList);
		table = new AbstractDownloadTable() {
		    private JLabel blankRenderer = new JLabel();
		    
            @Override
            public DownloadItem getDownloadItem(int row) {
                // row is actually a column here
                return horizontalTableModel.getDownloadItem(row);
            } 
            
            @Override
            public Component prepareEditor(TableCellEditor editor, int row, int column) {
                if (!isColumnFullyVisible(column)){
                    return blankRenderer;
                }
                return super.prepareEditor(editor, row, column);
            }           
            
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row,
                    int column) {
                if (!isColumnFullyVisible(column)){
                    return blankRenderer;
                }
                return super.prepareRenderer(renderer, row, column);
            }
        };
        table.setModel(horizontalTableModel);
		table.setShowHorizontalLines(false);
		table.setShowVerticalLines(false);		
		table.setOpaque(false);

		
		DownloadSummaryPanelRendererEditor rendererPanel = new DownloadSummaryPanelRendererEditor();
		DownloadTableRenderer renderer = new DownloadTableRenderer(rendererPanel);
		table.setDefaultRenderer(DownloadItem.class, renderer);
        table.setRowHeight(panelHeight);
        

        final DownloadSummaryPanelRendererEditor editorPanel = new DownloadSummaryPanelRendererEditor();
        Painter mouseOverPainter = new AbstractPainter<JComponent>(){
            @Override
            protected void doPaint(Graphics2D g, JComponent object, int width, int height) {
                g.setPaint(Color.WHITE);
                g.fillRoundRect(6, 6, 199, 39, 6, 6);
            }
        };
        editorPanel.setBackgroundPainter(mouseOverPainter);
        final DownloadTableEditor editor = new DownloadTableEditor(editorPanel);
        editor.initialiseEditor(allList, downloadActionHandlerFactory.create(allList));
        table.setDefaultEditor(DownloadItem.class, editor);
        
        table.setColumnDoubleClickHandler(new DownloadClickHandler());
        TablePopupHandler popupHandler = new DownloadPopupHandler(downloadActionHandlerFactory.create(allList), table){
            @Override
            protected int getPopupRow(int x, int y){
                //columns and rows are reversed in this table
                return table.columnAtPoint(new Point(x, y));
            }
            @Override
            public void maybeShowPopup(Component component, int x, int y) {
                if(isColumnFullyVisible(getPopupRow(x, y))){
                    super.maybeShowPopup(component, x, y);
                }
            }
                
        };

        table.setPopupHandler(popupHandler);
		
		table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            @Override
            public void columnAdded(TableColumnModelEvent e) {
                int columnIndex = e.getToIndex();
                table.getColumnModel().getColumn(columnIndex).setWidth(225);
                table.getColumnModel().getColumn(columnIndex).setMaxWidth(225);
                table.getColumnModel().getColumn(columnIndex).setMinWidth(225);
                table.getColumnModel().getColumn(columnIndex).setCellEditor(editor);
            }

            @Override
            public void columnMarginChanged(ChangeEvent e) {
            }

            @Override
            public void columnMoved(TableColumnModelEvent e) {
            }

            @Override
            public void columnRemoved(TableColumnModelEvent e) {
            }

            @Override
            public void columnSelectionChanged(ListSelectionEvent e) {
            }
        });
		

		table.setTableHeader(null);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setOpaque(false);
        tableScroll.getViewport().setOpaque(false);
        tableScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        tableScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        

        moreButton = new JXHyperlink();
        moreButton.setText(I18n.tr("<HTML><U><B>See all</B> {0}</U></HTML>", "   "));
        moreButton.setUnclickedColor(blueForeground);
        moreButton.setClickedColor(blueForeground);
        moreButton.setFont(seeAllFont);
        
        MouseListener navMouseListener = createDownloadNavListener();
        moreButton.addMouseListener(navMouseListener);
        header.addMouseListener(navMouseListener);
        
        minimizeButton = new JXHyperlink();
        minimizeButton.setText("<HTML><U>" +I18n.tr("Hide") + "</U></HTML>");
        minimizeButton.setFont(minimizeFont);
        minimizeButton.setMinimumSize(minimizeButton.getPreferredSize());
        minimizeButton.setForeground(minimizeColor);
        minimizeButton.setClickedColor(minimizeColor);
        minimizeButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                forceInvisibility(true);
            }
        });
        
        clearButton = new JXHyperlink();
        clearButton.setText("<HTML><U>" + I18n.tr("Clear finished") + "</U></HTML>");
        clearButton.setFont(minimizeFont);
        clearButton.setMinimumSize(clearButton.getPreferredSize());
        clearButton.setForeground(minimizeColor);
        clearButton.setClickedColor(minimizeColor);
        clearButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                DownloadSummaryPanel.this.downloadMediator.clearFinished();
            }
        });
        clearButton.setVisible(false);
        
        clearDivider = new JLabel("|");
        clearDivider.setVisible(false);
        
        initializeDownloadCompleteListener();
        initializeDownloadAddedListener();

        setLayout(new MigLayout("nocache, ins 0, gap 0! 0!, novisualpadding"));
        
        JPanel rightPanel = new JPanel(new MigLayout("nogrid, ins 0, gap 0! 0!, novisualpadding"));
        rightPanel.setOpaque(false);
        rightPanel.add(clearButton, "gaptop 2, aligny 0%, alignx 100%");
        rightPanel.add(clearDivider, "gaptop 2, aligny 0%, alignx 50%");
        rightPanel.add(minimizeButton, "gaptop 2, gapright 2, aligny 0%, alignx 100%, wrap");
        rightPanel.add(moreButton, "push, pad " + (-clearButton.getPreferredSize().height) + " 0 0 0, aligny 50%, alignx 50%");
        
        add(header, "gapleft 8, aligny 50%");
        add(tableScroll, "gapleft 12, aligny 50%, growx, growy, push");
        add(rightPanel, "growy, shrinkprio 0");
       
		
		updateTitle();
		addListeners();
		
		setMinimumSize(new Dimension(0, panelHeight));
		
		setVisibility(false);
	}
    
    private boolean isColumnFullyVisible(int column) {
        Rectangle cellRect = table.getCellRect(0, column, false);
        Rectangle visibleRect = table.getVisibleRect();
        return visibleRect.contains(cellRect);
    }

    private void initializeDownloadAddedListener() {
        final NavItem item = navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME);
        allList.addListEventListener(new ListEventListener<DownloadItem>(){
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
               if(allList.size() == 0){
                   setVisibility(false);
               } else if (!item.isSelected()){
                 setVisibility(true);
               }
            }});
    }

    private void initializeDownloadCompleteListener(){
        final EventList<DownloadItem> completeDownloads = GlazedListsFactory.filterList(allList, new DownloadStateMatcher(DownloadState.DONE));
        completeDownloads.addListEventListener(new ListEventListener<DownloadItem>(){
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                clearButton.setVisible(completeDownloads.size() > 0);
                clearDivider.setVisible(completeDownloads.size() > 0);
                
                allList.getReadWriteLock().readLock().lock();
                try {
                    while(listChanges.nextBlock()) {
                        if(listChanges.getType() == ListEvent.INSERT){
                            //only change icon if MainDownloadPanel is not showing
                            if (!navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME).isSelected()) {
                                header.setIcon(downloadCompletedIcon);
                            }
                            // loop thru the inserted elements, and add
                            for (int i=listChanges.getBlockStartIndex(); i<=listChanges.getBlockEndIndex(); i++) {
                                final DownloadItem downloadItem = listChanges.getSourceList().get(i);
                                if (shouldShowNotifier(downloadItem)) {
                                    notifier.showMessage(new Notification(I18n.tr("Download Complete"), downloadItem.getFileName(), new AbstractAction() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            ActionMap map = Application.getInstance().getContext().getActionManager()
                                            .getActionMap();
                                            map.get("restoreView").actionPerformed(e);
                                            NavItem item = navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME);
                                            item.select(SimpleNavSelectable.create(downloadItem));
                                        }
                                    }));
                                }
                            }
                        }
                    }
                } finally {
                    allList.getReadWriteLock().readLock().unlock();
                }
            }

            /**
             * Only show tray notifier if there is no other indication that the user's download has finished.
             *
             * 1. Main Limewire application window is minimized, or not in focus
             * 2. OR User's download is not visible in the download tray
             * 3. But if the main download panel is showing, do not show tray notification
             */
            private boolean shouldShowNotifier(DownloadItem item) {
                return (!GuiUtils.getMainFrame().isActive() ||
                        (!table.isColumnVisible(horizontalTableModel.indexOf(item)) &&
                         !navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME).isSelected()));
            }
        });
    }

    private MouseListener createDownloadNavListener() {
        final NavItem item = navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME);
        MouseListener navMouseListener = new ActionHandListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                item.select();
            }            
        });
        item.addNavItemListener(new NavItemListener(){
            @Override
            public void itemRemoved() {
                //do nothing
            }

            @Override
            public void itemSelected(boolean selected) {
                boolean isVisible = !selected;
                setVisibility(isVisible && allList.size() > 0);
                header.setIcon(downloadIcon);
            }
        });
        return navMouseListener;
    }
	
    
	private void addListeners(){
	    //update title when number of downloads changes and hide or show panel as necessary
        allList.addListEventListener(new ListEventListener<DownloadItem>() {

            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateTitle();
                        //TODO put adjsut view back in? What is going to be the logic for showing the download list? since it is user driven as well as automatic.
                        //waiting to hear back from mike s
                    }
                });
            }

        });
	}
	
	public int getDownloadCount(){
	    return allList.size();
	}
	    
    //Overridden to add listener to all components in this container so that mouse clicks will work anywhere
    @Override
    public void addMouseListener(MouseListener listener){
        super.addMouseListener(listener);
        for(Component comp : getComponents()){
            comp.addMouseListener(listener);
        }
        
        for(Component comp : header.getComponents()){
            comp.addMouseListener(listener);
        }
        table.addMouseListener(listener);
    }

	private void updateTitle(){
	    setCount(getDownloadCount());
	}
		
	private class DownloadSummaryPanelRendererEditor extends JXPanel implements DownloadTableCell{
		
	    private final JLabel nameLabel;
		private final LimeProgressBar progressBar;

        private final JLabel statusLabel;
	    private final JButton pauseButton;
	    private final JButton resumeButton; 
        private final JXHyperlink launchButton; 
        private final JXHyperlink tryAgainButton;
        private final JXHyperlink removeButton;

        private final JButton[] linkButtons;
        private final JButton[] buttons;
		
	    @Resource private Icon pauseIcon;
	    @Resource private Icon pauseIconHover;
	    @Resource private Icon pauseIconDown;
	    @Resource private Icon resumeIcon;
	    @Resource private Icon resumeIconHover;
	    @Resource private Icon resumeIconDown;

		public DownloadSummaryPanelRendererEditor() {
			GuiUtils.assignResources(this);
			
			statusLabel = new JLabel();		
			statusLabel.setFont(itemFont);
			nameLabel = new JLabel();
            nameLabel.setFont(itemFont);
            nameLabel.setForeground(fontColor);
            nameLabel.setMaximumSize(new Dimension(nameLabelWidth, Integer.MAX_VALUE));
            
			progressBar = progressBarFactory.create(0, 100);
			Dimension size = new Dimension(173, 8);
            progressBar.setPreferredSize(size);
            progressBar.setMaximumSize(size);
            progressBar.setMinimumSize(size);

            pauseButton = new IconButton(pauseIcon, pauseIconHover, pauseIconDown);
            pauseButton.setActionCommand(DownloadActionHandler.PAUSE_COMMAND);
            
            resumeButton = new IconButton(resumeIcon, resumeIconHover, resumeIconDown);
            resumeButton.setActionCommand(DownloadActionHandler.RESUME_COMMAND);

            launchButton = new JXHyperlink();
            launchButton.setText(I18n.tr("Launch"));
            launchButton.setFont(itemFont);
            launchButton.setUnclickedColor(blueForeground);
            launchButton.setClickedColor(blueForeground);
            launchButton.setActionCommand(DownloadActionHandler.LAUNCH_COMMAND);
            FontUtils.bold(launchButton);
            FontUtils.underline(launchButton);            
          
            tryAgainButton = new JXHyperlink();
            tryAgainButton.setText(I18n.tr("Try again"));
            tryAgainButton.setFont(itemFont);
            tryAgainButton.setUnclickedColor(blueForeground);
            tryAgainButton.setClickedColor(blueForeground);
            tryAgainButton.setActionCommand(DownloadActionHandler.RESUME_COMMAND);
            FontUtils.bold(tryAgainButton);
            FontUtils.underline(tryAgainButton);

            removeButton = new JXHyperlink();
            removeButton.setText(I18n.tr("Remove"));
            removeButton.setFont(itemFont);
            removeButton.setUnclickedColor(blueForeground);
            removeButton.setClickedColor(blueForeground);
            removeButton.setActionCommand(DownloadActionHandler.RESUME_COMMAND);
            FontUtils.bold(removeButton);
            FontUtils.underline(removeButton);

            linkButtons = new JButton[]{launchButton, tryAgainButton, removeButton};
            buttons = new JButton[]{pauseButton, resumeButton};
                        
			setOpaque(false);

            setLayout(new MigLayout("fill, ins 0 0 0 0 , nogrid, gap 0! 0!, novisualpadding"));
          
            add(nameLabel, "bottom, left, gapleft 15, gapright 15, wrap");
			add(progressBar, "top, left,gapleft 15, gapright 2, gaptop 6, hidemode 3");
            add(statusLabel, "top, left, gapleft 15, gapright 2, gaptop 6, hidemode 3");
            for(JButton button : linkButtons){
                add(button, "top, hidemode 3, gaptop 6, gapright 15");
            }            

            for(JButton button : buttons){
                add(button, "top, hidemode 3, gaptop 4, gapright 15");
            }
		}
		

        @Override
        public void update(DownloadItem item){
            nameLabel.setText(item.getTitle());
            progressBar.setVisible(item.getState() == DownloadState.DOWNLOADING || item.getState() == DownloadState.PAUSED);
            if (progressBar.isVisible()) { 
                if (item.getTotalSize() != 0) {
                    progressBar.setValue((int)(100 * item.getCurrentSize() / item.getTotalSize()));
                } 
                else {
                    progressBar.setValue(0);
                }
                progressBar.setEnabled(item.getState() == DownloadState.DOWNLOADING);
            }
            
            pauseButton.setVisible(item.getState() == DownloadState.DOWNLOADING);
            resumeButton.setVisible(item.getState() == DownloadState.PAUSED);
            tryAgainButton.setVisible(item.getState() == DownloadState.STALLED);
            launchButton.setVisible(item.isLaunchable() && item.getState() == DownloadState.DONE);
            removeButton.setVisible(item.getState() == DownloadState.ERROR);
            
            statusLabel.setVisible(item.getState() != DownloadState.DOWNLOADING && item.getState() != DownloadState.PAUSED);
            if (statusLabel.isVisible()){
                statusLabel.setText(getMessage(item));
                if(item.getState() == DownloadState.ERROR || item.getState() == DownloadState.STALLED){
                    statusLabel.setForeground(orangeForeground);
                } else {
                    statusLabel.setForeground(greenForeground);
                }
            }
        }
        
        private String getMessage(DownloadItem item) {
            switch (item.getState()) {
            case CANCELLED:
                return I18n.tr("Cancelled");
            case FINISHING:
                return I18n.tr("Finishing download...");
            case DONE:
                return I18n.tr("Done - ");
            case CONNECTING:
                return I18n.tr("Connecting...");           
            case STALLED:
                return I18n.tr("Stalled - ");
            case ERROR:         
                return I18n.tr("Unable to download: ");            
            case LOCAL_QUEUED:
                return getQueueTime(item.getRemainingQueueTime());                
            case REMOTE_QUEUED:
                if(item.getQueuePosition() == -1 || item.getQueuePosition() == Integer.MAX_VALUE){
                    return getQueueTime(item.getRemainingQueueTime());
                } 
                return I18n.trn("Waiting - Next in line",
                        "Waiting - {0} in line",
                        item.getQueuePosition(), item.getQueuePosition());
            default:
                throw new IllegalArgumentException("Unknown DownloadState: " + item.getState());
            }            
        }
        
        private String getQueueTime(long queueTime){
            if(queueTime == DownloadItem.UNKNOWN_TIME){
                return I18n.tr("Waiting - remaining time unknown");                
            } else {
                return I18n.tr("Waiting - Starting in {0}", CommonUtils.seconds2time(queueTime));
            }
        }


        @Override
        public Component getComponent() {
            return this;
        }


        @Override
        public void setEditorListener(ActionListener editorListener) {
            for (JButton button : linkButtons){
                button.addActionListener(editorListener);
            }
            
            for (JButton button : buttons){
                button.addActionListener(editorListener);
            }
        }
     
	}
	

	
    private void setCount(int count){
        moreButton.setText(I18n.tr("<HTML><U><B>See all</B> {0}</U></HTML>", Integer.toString(count)));
        moreButton.setMinimumSize(moreButton.getPreferredSize());
    }
	

	private class DownloadClickHandler implements TableColumnDoubleClickHandler {
	    @Override
	    public void handleDoubleClick(int col) {
	        DownloadItem item = table.getDownloadItem(col);
	        if (item.isLaunchable()) {
	            DownloadItemUtils.launch(item);
	        }
	    }
	}
	
	private boolean forceInvisible;
	
	@Override
	public void forceInvisibility(boolean isInvisible){
	    forceInvisible = isInvisible;
	    setVisibility(!forceInvisible);
	}
	
	@Override
	public void setVisibility(boolean visible){
	    setVisible(!forceInvisible && visible);
        visibilityListenerList.visibilityChanged(isVisible());
	}
	
	@Override
	public void toggleVisibility() {
	    if(!forceInvisible)
	    setVisibility(!isVisible());
	}

    @Override
    public void addVisibilityListener(VisibilityListener listener) {
        visibilityListenerList.addVisibilityListener(listener);        
    }

    @Override
    public void removeVisibilityListener(VisibilityListener listener) {
        visibilityListenerList.removeVisibilityListener(listener);
    }

    @Override
    public void addEnabledListener(EnabledListener listener) {
        enabledListenerList.addEnabledListener(listener);
    }

    @Override
    public void removeEnabledListener(EnabledListener listener) {
        enabledListenerList.removeEnabledListener(listener);
    }

    /**
     * Returns true if the component is enabled for use.  Always true. 
     */
    @Override
    public boolean isActionEnabled() {
        return true;
    }
    
}
