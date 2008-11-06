package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.LimeProgressBar;
import org.limewire.ui.swing.dnd.DownloadableTransferHandler;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.ui.swing.downloads.table.HorizontalDownloadTableModel;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.painter.DownloadSummaryPainter;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.RangeList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;



public class DownloadSummaryPanel extends JXPanel {

    /**
     * Number of download items displayed in the table
     */
	private static final int NUMBER_DISPLAYED = 5;

	private static final int LEFT_MARGIN = 8;

    private JTable table;

	private SummaryPanelHeader header;
	private JLabel moreLabel;
	//private JLabel completeLabel;
	private EventList<DownloadItem> allList;
    private EventList<DownloadItem> unfinishedList;
    private RangeList<DownloadItem> chokeList;
    
//    private JPanel completePanel;
//    private JPanel cardPanel;

//    private static final String TABLE = "TABLE";
//    private static final String COMPLETE = "COMPLETE";
    
  //  private CardLayout cardLayout;

   // @Resource private Color allCompleteColour;
    @Resource private Color fontColour;
    @Resource private Font moreFont;
    @Resource private Font itemFont; 
    @Resource private Icon downloadIcon;
    
    private DownloadStatusPanelRenderer downloadStatusPanelRenderer;

    private void createMoreLabel() {
        moreLabel = new JLabel(I18n.tr("see more downloads")) {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                
                Graphics2D g2 = (Graphics2D) g;
                
                int bottom = this.getHeight();
                
                String label = this.getText();
                Rectangle2D labelRect = this.getFont().getStringBounds(label, g2.getFontRenderContext());
                
                g.setColor(new Color(0x2b,0x5b,0xaa));
                g.drawLine(LEFT_MARGIN, bottom-7, (int)labelRect.getWidth()+LEFT_MARGIN, bottom-7);
            }
      };
      
      moreLabel.setForeground(fontColour);
      moreLabel.setFont(moreFont);
      
      moreLabel.setBorder(BorderFactory.createEmptyBorder(0,LEFT_MARGIN,6,0));
    }
    
    @Inject
	public DownloadSummaryPanel(DownloadListManager downloadListManager, MainDownloadPanel mainDownloadPanel, Navigator navigator) {
	    GuiUtils.assignResources(this);
	    setTransferHandler(new DownloadableTransferHandler(downloadListManager));
	    
        this.allList = downloadListManager.getSwingThreadSafeDownloads();

        setLayout(new BorderLayout());
        setBackgroundPainter(new DownloadSummaryPainter());
                
        //leaving this commented out until we are sure it is gone
//        completePanel = new JPanel(new BorderLayout());
//        completePanel.setOpaque(false);
//        completeLabel = new JLabel("<html><u>" + I18n.tr("Downloads Complete") + "</u></html>", JLabel.CENTER);
//        completeLabel.setForeground(allCompleteColour);
//        completePanel.add(completeLabel);
        
        unfinishedList = GlazedListsFactory.filterList(allList, new DownloadStateExcluder(DownloadState.DONE));
		
		header = new SummaryPanelHeader();

        

        add(header, BorderLayout.WEST);

        createMoreLabel();
        add(moreLabel, BorderLayout.EAST);

        Comparator<DownloadItem> comparator = new Comparator<DownloadItem>() {
            @Override
            public int compare(DownloadItem o1, DownloadItem o2) {
                return getSortPriority(o2.getState()) - getSortPriority(o1.getState());
            }
        };

        SortedList<DownloadItem> sortedList = GlazedListsFactory.sortedList(allList, comparator);		
		
		chokeList = GlazedListsFactory.rangeList(sortedList);
		chokeList.setHeadRange(0, NUMBER_DISPLAYED);
		table = new JTable(new HorizontalDownloadTableModel(sortedList));
		table.setShowHorizontalLines(false);
		table.setShowVerticalLines(false);		
		table.setOpaque(false);
		table.setRowHeight(28);
		
		downloadStatusPanelRenderer = new DownloadStatusPanelRenderer();
		table.setDefaultRenderer(DownloadItem.class, downloadStatusPanelRenderer);

//        cardLayout = new CardLayout();
//        cardPanel = new JPanel(cardLayout);
//        cardPanel.setOpaque(false);
//        cardPanel.setBorder(BorderFactory.createEmptyBorder(7,0,4,0));        

//        add(cardPanel, BorderLayout.CENTER);
//
//        cardPanel.add(completePanel, COMPLETE);
//        cardPanel.add(table, TABLE);
//
//        cardLayout.show(cardPanel, TABLE);
		add(table);
		
		updateTitle();
		adjustVisibility();  
		
		addListeners();
		
        final NavItem item = navigator.createNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME, mainDownloadPanel);
        
        addMouseListener(new ActionHandListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                item.select();
            }            
        }));
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
                        adjustVisibility();
                    }
                });
            }

        });
	}
	    
	//hide panel if there are no downloads.  show it if there are
    private void adjustVisibility() {
        if (allList.size() > 0) {
            
            setVisible(true);

            if (unfinishedList.size() > 0) {//downloads in progress
                
//                cardLayout.show(cardPanel, TABLE);

                if (allList.size() > NUMBER_DISPLAYED) {
                    chokeList.setHeadRange(0, NUMBER_DISPLAYED - 1);
                    moreLabel.setVisible(true);
                } else {
                    chokeList.setHeadRange(0, NUMBER_DISPLAYED);
                    moreLabel.setVisible(false);
                }
                
            } //else {//all downloads complete
           //     cardLayout.show(cardPanel, COMPLETE);
             //   moreLabel.setVisible(false);
         //   }
            
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
        
        for(Component comp : header.getComponents()){
            comp.addMouseListener(listener);
        }
    }

	private void updateTitle(){
	    header.setCount(allList.size());
	}
	
	
	private class DownloadStatusPanelRenderer extends JPanel implements
			TableCellRenderer {
		
	    private final JLabel nameLabel;
		private final LimeProgressBar progressBar;

		public DownloadStatusPanelRenderer() {
			super(new GridBagLayout());
			
			nameLabel = new JLabel();
			progressBar = new LimeProgressBar(0,100);
			progressBar.setPreferredSize(
			        new Dimension((int)progressBar.getPreferredSize().getWidth(), 10));

            nameLabel.setFont(itemFont);
                        
			setOpaque(false);
			GridBagConstraints gbc = new GridBagConstraints();

			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.anchor = GridBagConstraints.WEST;
			nameLabel.setAlignmentY(JLabel.LEFT_ALIGNMENT);
			nameLabel.setOpaque(false);
			gbc.insets = new Insets(0, LEFT_MARGIN, 0, LEFT_MARGIN);
			add(nameLabel, gbc);

			gbc.gridy = 1;
			gbc.weightx = 1;
			add(progressBar, gbc);
		}
		

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
            DownloadItem item = (DownloadItem) value;
            nameLabel.setText(item.getTitle());
            progressBar.setValue(item.getPercentComplete());
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

}
