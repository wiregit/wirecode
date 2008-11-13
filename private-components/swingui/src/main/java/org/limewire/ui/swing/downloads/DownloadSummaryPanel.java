package org.limewire.ui.swing.downloads;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.Comparator;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.components.LimeProgressBar;
import org.limewire.ui.swing.dnd.DownloadableTransferHandler;
import org.limewire.ui.swing.downloads.table.AbstractDownloadTable;
import org.limewire.ui.swing.downloads.table.DownloadActionHandler;
import org.limewire.ui.swing.downloads.table.DownloadPopupHandler;
import org.limewire.ui.swing.downloads.table.HorizontalDownloadTableModel;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.painter.DownloadSummaryPainter;
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

	private static final int LEFT_MARGIN = 25;

	private final VisibilityListenerList visibilityListenerList = new VisibilityListenerList();
    private AbstractDownloadTable table;

	private SummaryPanelHeader header;
	private JButton moreButton;
	private EventList<DownloadItem> allList;
    private RangeList<DownloadItem> chokeList;    
    

    @Resource private Font itemFont; 
    @Resource private Color fontColor; 
    @Resource private Icon downloadIcon;

    private DownloadStatusPanelRendererEditor downloadStatusPanelRenderer;
    private DownloadStatusPanelRendererEditor downloadStatusPanelEditor;

    
    @Inject
	public DownloadSummaryPanel(DownloadListManager downloadListManager, MainDownloadPanel mainDownloadPanel, 
	        Navigator navigator, PropertiesFactory<DownloadItem> propertiesFactory) {
	    GuiUtils.assignResources(this);
	    setTransferHandler(new DownloadableTransferHandler(downloadListManager));
	    
        this.allList = downloadListManager.getSwingThreadSafeDownloads();

        setLayout(new MigLayout());
        setBackgroundPainter(new DownloadSummaryPainter());
                
		header = new SummaryPanelHeader();

        Comparator<DownloadItem> comparator = new Comparator<DownloadItem>() {
            @Override
            public int compare(DownloadItem o1, DownloadItem o2) {
                return getSortPriority(o2.getState()) - getSortPriority(o1.getState());
            }
        };

        SortedList<DownloadItem> sortedList = GlazedListsFactory.sortedList(allList, comparator);		
		
		chokeList = GlazedListsFactory.rangeList(sortedList);
		chokeList.setHeadRange(0, NUMBER_DISPLAYED);
		final HorizontalDownloadTableModel tableModel = new HorizontalDownloadTableModel(sortedList);
		table = new AbstractDownloadTable(){
            @Override
            public DownloadItem getDownloadItem(int row) {
                //row is actually a column here
                return tableModel.getDownloadItem(row);
            }};
            table.setModel(tableModel);
		table.setShowHorizontalLines(false);
		table.setShowVerticalLines(false);		
		table.setOpaque(false);
		
		downloadStatusPanelRenderer = new DownloadStatusPanelRendererEditor();
		table.setDefaultRenderer(DownloadItem.class, downloadStatusPanelRenderer);
        table.setRowHeight(45);
        
        downloadStatusPanelEditor = new DownloadStatusPanelRendererEditor();
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
        

        moreButton = new HyperLinkButton(I18n.tr("See All"));
        FontUtils.bold(moreButton);
        
        final NavItem item = navigator.createNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME, mainDownloadPanel);
        MouseListener navMouseListener = new ActionHandListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                item.select();
            }            
        });
        moreButton.addMouseListener(navMouseListener);
        header.addMouseListener(navMouseListener);
        for(Component c : header.getComponents()){
            c.addMouseListener(navMouseListener);
        }
        

        add(header, "aligny 50%");
        add(tableScroll, "aligny 50%, growx, push");
        add(moreButton, "aligny 50%");
		
		updateTitle();
		addListeners();
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
	    header.setCount(allList.size());
	}
	
	
	private class DownloadStatusPanelRendererEditor extends TableRendererEditor {
		
	    private final JLabel nameLabel;
		private final LimeProgressBar progressBar;
		private final Border mouseOverBorder;
		
	    @Resource private Color progressBarBorderColor;
	    @Resource private Color mouseOverColor;

		public DownloadStatusPanelRendererEditor() {
            setLayout(new GridBagLayout());
			GuiUtils.assignResources(this);
			
			mouseOverBorder = new LineBorder(mouseOverColor, 3);
			
			nameLabel = new JLabel();
            nameLabel.setFont(itemFont);
            nameLabel.setForeground(fontColor);
            
			progressBar = new LimeProgressBar(0,100);
			Dimension size = new Dimension(173, 8);
            progressBar.setPreferredSize(size);
            progressBar.setMaximumSize(size);
            progressBar.setMinimumSize(size);
			progressBar.setBorder(BorderFactory.createLineBorder(progressBarBorderColor));
                        
			setOpaque(false);
			
	        GridBagConstraints gbc = new GridBagConstraints();

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.SOUTHWEST;
            nameLabel.setAlignmentY(JLabel.LEFT_ALIGNMENT);
            nameLabel.setOpaque(false);
            gbc.insets = new Insets(0, LEFT_MARGIN, 6, 0);
            add(nameLabel, gbc);

            gbc.gridy = 1;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            add(progressBar, gbc);			
		}
		

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
            updateCell((DownloadItem) value);
            setBorder(null);
            return this;
        }


        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            updateCell((DownloadItem) value);
            setBorder(mouseOverBorder);
            return this;
        }


        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }
        
        private void updateCell(DownloadItem item){
            nameLabel.setText(item.getTitle());
            progressBar.setValue(item.getPercentComplete());
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
	
	private class SummaryPanelHeader extends JPanel {
	    private JLabel countLabel;
	    public SummaryPanelHeader(){
	        super(new MigLayout());
	        setOpaque(false);
	        
	        JLabel dlLabel = new JLabel(I18n.tr("DL"), JLabel.RIGHT);
	        FontUtils.bold(dlLabel);
	        
	        countLabel = new JLabel(Integer.toString(0, JLabel.LEFT));
	        add(new JLabel(downloadIcon), "spanx 3, grow, wrap");
	        add(dlLabel);
	        add(new JLabel("|"));
	        add(countLabel);
	    }
	    
	    public void setCount(int count){
	        countLabel.setText(Integer.toString(count));
	    }
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
