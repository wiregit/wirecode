package org.limewire.ui.swing.downloads;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.Comparator;
import java.util.EventObject;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimeProgressBar;
import org.limewire.ui.swing.components.LimeProgressBarFactory;
import org.limewire.ui.swing.dnd.DownloadableTransferHandler;
import org.limewire.ui.swing.downloads.table.AbstractDownloadTable;
import org.limewire.ui.swing.downloads.table.DownloadActionHandler;
import org.limewire.ui.swing.downloads.table.DownloadPopupHandler;
import org.limewire.ui.swing.downloads.table.HorizontalDownloadTableModel;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.painter.BarPainterFactory;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.TableColumnDoubleClickHandler;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.ui.swing.util.VisibilityListener;
import org.limewire.ui.swing.util.VisibilityListenerList;
import org.limewire.ui.swing.util.VisibleComponent;
import org.limewire.util.CommonUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.RangeList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class DownloadSummaryPanel extends JXPanel implements VisibleComponent {

    /**
     * Number of download items displayed in the table
     */
	private static final int NUMBER_DISPLAYED = 5;

	//private static final int LEFT_MARGIN = 25;

	private final LimeProgressBarFactory progressBarFactory;
    private final Navigator navigator;
	
	private final VisibilityListenerList visibilityListenerList = new VisibilityListenerList();
    private AbstractDownloadTable table;

	private JLabel header;
	private JButton moreButton;
	private EventList<DownloadItem> allList;
    private RangeList<DownloadItem> chokeList;    

   // private JButton minimizeButton;

    @Resource private Font itemFont; 
    @Resource private Color fontColor; 
    @Resource private Icon downloadIcon;
//    @Resource private Icon downloadCompletedIcon;
//    @Resource private Icon minimizeIcon;
//    @Resource private Icon minimizeIconHover;
//    @Resource private Icon minimizeIconDown;


    private DownloadSummaryPanelRendererEditor downloadStatusPanelRenderer;
    private DownloadSummaryPanelRendererEditor downloadStatusPanelEditor;

    
    @Inject
	public DownloadSummaryPanel(DownloadListManager downloadListManager, DownloadMediator downloadMediator, MainDownloadPanel mainDownloadPanel, 
	        Navigator navigator, PropertiesFactory<DownloadItem> propertiesFactory, 
	        LimeProgressBarFactory progressBarFactory, BarPainterFactory barPainterFactory) {
	    this.navigator = navigator;
        GuiUtils.assignResources(this);
	    
        this.progressBarFactory = progressBarFactory;
        
        setTransferHandler(new DownloadableTransferHandler(downloadListManager));
	    
        this.allList = downloadMediator.getDownloadList();//downloadListManager.getSwingThreadSafeDownloads();

        setLayout(new MigLayout());
        setBackgroundPainter(barPainterFactory.createDownloadSummaryBarPainter());
                
		header = new JLabel(downloadIcon);

        Comparator<DownloadItem> comparator = new Comparator<DownloadItem>() {
            @Override
            public int compare(DownloadItem o1, DownloadItem o2) {
                return getSortPriority(o2.getState()) - getSortPriority(o1.getState());
            }
        };
        
        navigator.createNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME, mainDownloadPanel);

        SortedList<DownloadItem> sortedList = GlazedListsFactory.sortedList(allList, comparator);		
		
		chokeList = GlazedListsFactory.rangeList(sortedList);
		chokeList.setHeadRange(0, NUMBER_DISPLAYED);
		final HorizontalDownloadTableModel tableModel = new HorizontalDownloadTableModel(sortedList);
		table = new AbstractDownloadTable() {
            @Override
            public DownloadItem getDownloadItem(int row) {
                // row is actually a column here
                return tableModel.getDownloadItem(row);
            }
        };
        table.setModel(tableModel);
		table.setShowHorizontalLines(false);
		table.setShowVerticalLines(false);		
		table.setOpaque(false);
		
		downloadStatusPanelRenderer = new DownloadSummaryPanelRendererEditor();
		table.setDefaultRenderer(DownloadItem.class, downloadStatusPanelRenderer);
        table.setRowHeight(45);
        
        downloadStatusPanelEditor = new DownloadSummaryPanelRendererEditor();
        table.setDefaultEditor(DownloadItem.class, downloadStatusPanelEditor);
        
        table.setColumnDoubleClickHandler(new DownloadClickHandler());
        TablePopupHandler popupHandler = new DownloadPopupHandler(new DownloadActionHandler(sortedList, propertiesFactory), table){
            @Override
            protected int getPopupRow(int x, int y){
                //columns and rows are reversed in this table
                return table.columnAtPoint(new Point(x, y));
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
                table.getColumnModel().getColumn(columnIndex).setCellEditor(downloadStatusPanelEditor);
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
        

        moreButton = new HyperLinkButton(I18n.tr("See All"));
        FontUtils.bold(moreButton);
        
        MouseListener navMouseListener = createDownloadNavListener();
        moreButton.addMouseListener(navMouseListener);
        header.addMouseListener(navMouseListener);

        add(header, "aligny 50%");
        add(tableScroll, "aligny 50%, growx, push");
        add(moreButton, "aligny 50%");
		
		updateTitle();
		addListeners();
	}


    private MouseListener createDownloadNavListener() {
        final NavItem item = navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME);
        MouseListener navMouseListener = new ActionHandListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                item.select();
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
	    setCount(allList.size());
	}
		
	private class DownloadSummaryPanelRendererEditor extends TableRendererEditor {
		
	    private final JLabel nameLabel;
		private final LimeProgressBar progressBar;
	//	private final Border mouseOverBorder;
	    private JButton pauseButton;
	    private JButton resumeButton; 
	    private Painter mouseOverPainter;
	    private JLabel statusLabel;
		
	//    @Resource private Color mouseOverColor;
	    @Resource private Icon pauseIcon;
	    @Resource private Icon pauseIconHover;
	    @Resource private Icon pauseIconDown;
	    @Resource private Icon resumeIcon;
	    @Resource private Icon resumeIconHover;
	    @Resource private Icon resumeIconDown;

		public DownloadSummaryPanelRendererEditor() {
            //setLayout(new GridBagLayout());
            setLayout(new MigLayout("nogrid, ins 0, gap 0! 0!, novisualpadding"));
			GuiUtils.assignResources(this);
			
			//mouseOverBorder = new LineBorder(mouseOverColor, 3);
			
			mouseOverPainter = new AbstractPainter<JComponent>(){
                @Override
                protected void doPaint(Graphics2D g, JComponent object, int width, int height) {
                    g.setPaint(Color.WHITE);
                    g.fillRoundRect(0, 0, width, height, 10, 10);
                }
			};
			
			statusLabel = new JLabel();			
			nameLabel = new JLabel();
            nameLabel.setFont(itemFont);
            nameLabel.setForeground(fontColor);
            
			progressBar = progressBarFactory.create(0, 100);
			Dimension size = new Dimension(173, 8);
            progressBar.setPreferredSize(size);
            progressBar.setMaximumSize(size);
            progressBar.setMinimumSize(size);

            pauseButton = new IconButton(pauseIcon, pauseIconHover, pauseIconDown);
            resumeButton = new IconButton(resumeIcon, resumeIconHover, resumeIconDown);
                        
			setOpaque(false);
			
			add(nameLabel, "wrap");
			add(progressBar, "hidemode 3");
            add(statusLabel, "hidemode 3");
            add(pauseButton, "hidemode 3");
            add(resumeButton, "hidemode 3");
			
//	        GridBagConstraints gbc = new GridBagConstraints();
//
//            gbc.gridx = 0;
//            gbc.gridy = 0;
//            gbc.fill = GridBagConstraints.NONE;
//            gbc.anchor = GridBagConstraints.SOUTHWEST;
//            nameLabel.setAlignmentY(JLabel.LEFT_ALIGNMENT);
//            nameLabel.setOpaque(false);
//            gbc.insets = new Insets(0, LEFT_MARGIN, 6, 0);
//            add(nameLabel, gbc);
//
//            gbc.gridy = 1;
//            gbc.anchor = GridBagConstraints.NORTHWEST;
//            add(progressBar, gbc);	
//            
//            gbc.gridx++;
//            add(pauseButton, gbc);
//            add(resumeButton, gbc);
            
//            MouseListener navMouseListener = createDownloadNavListener();
//            addMouseListener(navMouseListener);
//            nameLabel.addMouseListener(navMouseListener);
//            progressBar.addMouseListener(navMouseListener);
            
		}
		

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
            updateCell((DownloadItem) value);
            setBackgroundPainter(null);
            return this;
        }


        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            updateCell((DownloadItem) value);
            setBackgroundPainter(mouseOverPainter);
            return this;
        }


        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }
        
        private void updateCell(DownloadItem item){
            nameLabel.setText(item.getTitle());
            progressBar.setVisible(item.getState() == DownloadState.DOWNLOADING || item.getState() == DownloadState.PAUSED);
            if (progressBar.isVisible()){
            progressBar.setValue(item.getPercentComplete());
            }
            pauseButton.setVisible(item.getState().isPausable());
            resumeButton.setVisible(item.getState().isResumable());
            statusLabel.setVisible(item.getState() != DownloadState.DOWNLOADING && item.getState() != DownloadState.PAUSED);
            if (statusLabel.isVisible()){
                statusLabel.setText(getMessage(item));
            }
        }
        
        private String getMessage(DownloadItem item) {
            switch (item.getState()) {
            case CANCELLED:
                return I18n.tr("Cancelled");
            case FINISHING:
                return I18n.tr("Finishing download...");
            case DONE:
                return I18n.tr("Done");
            case CONNECTING:
                return I18n.tr("Connecting...");           
            case STALLED:
                return I18n.tr("Stalled - ");
            case ERROR:         
                return I18n.tr("Unable to download: ");            
            case LOCAL_QUEUED:
                long queueTime = item.getRemainingQueueTime();
                if(queueTime == DownloadItem.UNKNOWN_TIME){
                    return I18n.tr("Queued - remaining time unknown");                
                } else {
                    return I18n.tr("Queued - About {0} before download can begin", CommonUtils.seconds2time(queueTime));
                }
            case REMOTE_QUEUED:
                if(item.getQueuePosition() == -1){
                    return I18n.tr("Queued - About {0} before download can begin", CommonUtils.seconds2time(item.getRemainingQueueTime()));
                }
                return I18n.trn("Queued - {0} person ahead of you for this file",
                        "Queued - {0} people ahead of you for this file",
                        item.getQueuePosition(), item.getQueuePosition());
            default:
                throw new IllegalArgumentException("Unknown DownloadState: " + item.getState());
            }
            
        }
     
	}
	
	private int getSortPriority(DownloadState state){
	    switch(state){
        case DOWNLOADING:
            return 8;
        case DONE:
            return 7;
        case PAUSED:
            return 6;
        case CONNECTING:
            return 5;
        case FINISHING:
            return 4;
        case LOCAL_QUEUED:
        case REMOTE_QUEUED:
            return 3;
        case STALLED:
            return 2;
	    case ERROR:
            return 1;
        case CANCELLED:
            return 0;
	    
	    }
	    return 0;
	}
	
    private void setCount(int count){
        moreButton.setText(I18n.tr("See All {0}", Integer.toString(count)));
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
	
	@Override
	public void toggleVisibility() {
	    boolean visible = !isVisible(); 
	    setVisible(visible);
	    visibilityListenerList.visibilityChanged(visible);
	}

    @Override
    public void addVisibilityListener(VisibilityListener listener) {
        visibilityListenerList.addVisibilityListener(listener);        
    }

    @Override
    public void removeVisibilityListener(VisibilityListener listener) {
        visibilityListenerList.removeVisibilityListener(listener);
    }
}
