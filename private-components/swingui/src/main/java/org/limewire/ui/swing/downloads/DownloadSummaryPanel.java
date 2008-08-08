package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.downloads.table.DownloadTableModel;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavSelectionListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.Navigator.NavCategory;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.RangeList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import org.limewire.ui.swing.util.I18n;



public class DownloadSummaryPanel extends JPanel {

    /**
     * Number of download items displayed in the table
     */
	private static final int NUMBER_DISPLAYED = 4;


    private JTable table;

	
	private JLabel titleLabel;
	private JLabel moreLabel;
	private JLabel completeLabel;
	private EventList<DownloadItem> allList;
    private EventList<DownloadItem> warningList;
    private EventList<DownloadItem> unfinishedList;
    private RangeList<DownloadItem> chokeList;
    
    private JPanel completePanel;
    private JPanel cardPanel;

    private static final String TABLE = "TABLE";
    private static final String COMPLETE = "COMPLETE";
    
    private CardLayout cardLayout;

    @Resource
    private Icon warningIcon;

    @Resource
    private Color moreColor;
    @Resource
    private Color allCompleteColor;

    @Resource
    private Color highlightColor;
    @Resource
    private Color highlightFontColor;
    @Resource
    private Color fontColor;

    private Navigator navigator;


    private boolean selected;


    private DownloadStatusPanelRenderer downloadStatusPanelRenderer;


     

	/**
	 * Create the panel
	 * @param navigator2 
	 */
	public DownloadSummaryPanel(final EventList<DownloadItem> itemList, Navigator navigator) {
	    GuiUtils.assignResources(this);
	    
	    this.navigator = navigator;
        this.allList = itemList;

        setLayout(new BorderLayout());
                
        completePanel = new JPanel(new BorderLayout());
        completePanel.setOpaque(false);
        completeLabel = new JLabel("<html><u>" + I18n.tr("Downloads Complete") + "</u></html>", JLabel.CENTER);
        completeLabel.setForeground(allCompleteColor);
        completePanel.add(completeLabel);
        
        unfinishedList = new FilterList<DownloadItem>(allList, new DownloadStateExcluder(DownloadState.DONE));
        warningList = new FilterList<DownloadItem>(allList, new DownloadStateMatcher(DownloadState.ERROR, DownloadState.STALLED)); 
		
		titleLabel = new JLabel();
		titleLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        FontUtils.changeStyle(titleLabel, Font.BOLD);
		add(titleLabel, BorderLayout.NORTH);
		moreLabel = new JLabel("<html><u>" + I18n.tr("More...") + "</u></html>", JLabel.RIGHT);
		moreLabel.setForeground(moreColor);
		add(moreLabel, BorderLayout.SOUTH);
			

		
		Comparator<DownloadItem> comparator = new Comparator<DownloadItem>(){

            @Override
            public int compare(DownloadItem o1, DownloadItem o2) {
                return getSortPriority(o2.getState()) - getSortPriority(o1.getState());
            }
		    
		};
		
		SortedList<DownloadItem> sortedList = new SortedList<DownloadItem>(allList, comparator);		
		
		chokeList = new RangeList<DownloadItem>(sortedList);
		chokeList.setHeadRange(0, NUMBER_DISPLAYED);
		table = new JTable(new DownloadTableModel(chokeList));
		table.setShowHorizontalLines(false);
		table.setShowVerticalLines(false);		
		table.setOpaque(false);
		
		//TODO: sorting
		downloadStatusPanelRenderer = new DownloadStatusPanelRenderer();
		table.setDefaultRenderer(DownloadItem.class, downloadStatusPanelRenderer);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        add(cardPanel, BorderLayout.CENTER);

        cardPanel.add(completePanel, COMPLETE);
        cardPanel.add(table, TABLE);

        cardLayout.show(cardPanel, TABLE);
		
		updateTitle();
		adjustVisibility();  
		
		addListeners();
		
	}
	
    
	private void addListeners(){
	    //update title when number of downloads changes and hide or show panel as necessary
        allList.addListEventListener(new ListEventListener<DownloadItem>() {

            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                updateTitle();
                adjustVisibility();
            }

        });

        //show warning icon when something is added to warningList
        warningList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {

                boolean addWarning = false;

                while (listChanges.next()) {
                    if (listChanges.getType() == ListEvent.INSERT) {
                        addWarning = true;
                    }
                }

                if (addWarning) {
                    setWarningVisible(true);
                }
            }

        });
        
        //hide warning icon when clicked
        addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e){
                setWarningVisible(false);
            }
        });
        
        navigator.addNavListener(new NavSelectionListener(){
            @Override
            public void navItemSelected(NavCategory category, NavItem navItem) {
               setSelected(category == NavCategory.DOWNLOAD);
            }            
        });
	}
	
	private void setSelected(boolean selected) {
	    if (isSelected() != selected) {
            this.selected = selected;
            setOpaque(selected);
            if (selected) {
                setBackground(highlightColor);
                setBorder(BorderFactory.createLoweredBevelBorder());
                titleLabel.setForeground(highlightFontColor);
                moreLabel.setForeground(highlightFontColor);
                completeLabel.setForeground(highlightFontColor);
                downloadStatusPanelRenderer.setForeground(highlightFontColor);
            } else {
             //   setBackground(Color.WHITE);
                setBorder(null);
                titleLabel.setForeground(fontColor);
                moreLabel.setForeground(moreColor);
                completeLabel.setForeground(allCompleteColor);
                downloadStatusPanelRenderer.setForeground(fontColor);
            }
        }
    }
	
	private boolean isSelected(){
	    return selected;
	}
    
	//hide panel if there are no downloads.  show it if there are
    private void adjustVisibility() {
        if (allList.size() > 0) {
            
            setVisible(true);

            if (unfinishedList.size() > 0) {//downloads in progress
                
                cardLayout.show(cardPanel, TABLE);

                if (allList.size() > NUMBER_DISPLAYED) {
                    chokeList.setHeadRange(0, NUMBER_DISPLAYED - 1);
                    moreLabel.setVisible(true);
                } else {
                    chokeList.setHeadRange(0, NUMBER_DISPLAYED);
                    moreLabel.setVisible(false);
                }
                
            } else {//all downloads complete
                cardLayout.show(cardPanel, COMPLETE);
                moreLabel.setVisible(false);
            }
            
        } else {
            //Nothing to show
            setVisible(false);
        }
        revalidate();
    }

    //Overridden to add listener to all components in this container so that mouse clicks will work anywhere
    @Override
    public void addMouseListener(MouseListener listener){
        super.addMouseListener(listener);
        for(Component comp : getComponents()){
            comp.addMouseListener(listener);
        }
        
        for(Component comp : cardPanel.getComponents()){
            comp.addMouseListener(listener);
        }
    }

	private void updateTitle(){
	    if (unfinishedList.size() > 0) {
            titleLabel.setText(I18n.tr("Downloads ({0})", unfinishedList.size()));
        } else {
            titleLabel.setText(I18n.tr("Downloads"));
        }
	    
        if (warningList.size() == 0) {
            setWarningVisible(false);
        }
	}
	
	private void setWarningVisible(boolean visible){
	    if(visible){
	        titleLabel.setIcon(warningIcon);
	    } else {
	        titleLabel.setIcon(null);
	    }
	}
	
	private class DownloadStatusPanelRenderer extends JPanel implements
			TableCellRenderer {
		private JLabel nameLabel = new JLabel();

		private JLabel percentLabel = new JLabel();

		public DownloadStatusPanelRenderer() {
			super(new GridBagLayout());
			nameLabel = new JLabel();
            percentLabel = new JLabel();

	        FontUtils.changeSize(nameLabel, -1);
	        FontUtils.changeSize(percentLabel, -1);
			setOpaque(false);
			GridBagConstraints gbc = new GridBagConstraints();

			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.anchor = GridBagConstraints.WEST;
			nameLabel.setAlignmentY(JLabel.LEFT_ALIGNMENT);
			nameLabel.setOpaque(false);
			add(nameLabel, gbc);

			gbc.gridx = 1;
			gbc.weightx = 0;
			gbc.anchor = GridBagConstraints.EAST;
			percentLabel.setAlignmentY(JLabel.RIGHT_ALIGNMENT);
			
			add(percentLabel, gbc);
		}
		
		@Override
		public void setForeground(Color c) {
            super.setForeground(c);
            //prevent NPE at super construction
            if (nameLabel != null) {
                nameLabel.setForeground(c);
            }
            if (percentLabel != null) {
                percentLabel.setForeground(c);
            }
        }

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
            DownloadItem item = (DownloadItem) value;
            nameLabel.setText(item.getTitle());
            percentLabel.setText(item.getPercentComplete() + "%");
            return this;
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

}
