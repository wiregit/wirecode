package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.application.Resource;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.downloads.DownloadMediator;
import org.limewire.ui.swing.downloads.DownloadStatusPanel;
import org.limewire.ui.swing.nav.FilesSharingSummaryPanel;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavSelectionListener;
import org.limewire.ui.swing.nav.NavTree;
import org.limewire.ui.swing.nav.NavigableTree;
import org.limewire.ui.swing.nav.SearchBar;
import org.limewire.ui.swing.nav.Navigator.NavCategory;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.Line;

class LeftPanel extends JPanel implements NavigableTree {

    private final SearchBar searchBar;
    private final NavTree navTree;
    private final DownloadStatusPanel downloadPanel;
    private final FilesSharingSummaryPanel filesPanel;
    private SearchHandler searchHandler;
    
    /** The color of the lines separating the GUI panels */
    @Resource
    private Color lineColor;

    public LeftPanel(DownloadMediator downloadMediator) {
    	GuiUtils.injectFields(this);
        this.searchBar = new SearchBar();
        this.navTree = new NavTree();
        this.downloadPanel =  new DownloadStatusPanel(downloadMediator.getUnfilteredList());
        this.filesPanel = new FilesSharingSummaryPanel();
        setMinimumSize(new Dimension(150, 0));
        setMaximumSize(new Dimension(150, Integer.MAX_VALUE));
        setPreferredSize(new Dimension(150, 700));
        
        searchBar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchHandler.doSearch(new DefaultSearchInfo(searchBar.getText(), SearchCategory.ALL));
            }
        });
        
        downloadPanel.addMouseListener(new MouseAdapter(){

            public void mouseClicked(MouseEvent arg0) {
                showDownloads();
            }            
        });
             
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        gbc.gridx = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(15, 10, 15, 10);
        add(searchBar, gbc);
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.gridx = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(0, 0, 0, 0);
        JScrollPane scrollableNav = new JScrollPane(navTree);
        scrollableNav.setOpaque(false);
        scrollableNav.getViewport().setOpaque(false);
        scrollableNav.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollableNav.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollableNav.setBorder(null);
        add(scrollableNav, gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        gbc.gridx = GridBagConstraints.REMAINDER;
        add(new Line(lineColor), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 0, 0);
        gbc.weighty = 0;
        gbc.gridx = GridBagConstraints.REMAINDER;
        add(downloadPanel, gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weighty = 0;
        gbc.gridx = GridBagConstraints.REMAINDER;
        add(new Line(lineColor), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 0);
        gbc.weighty = 0;
        gbc.gridx = GridBagConstraints.REMAINDER;
        add(filesPanel, gbc);
    }

    public void setSearchHandler(SearchHandler searchHandler) {
        this.searchHandler = searchHandler;
    }

    @Override
    public void addNavigableItem(NavCategory category, NavItem navItem,
            boolean userRemovable) {
        navTree.addNavigableItem(category, navItem, userRemovable);
    }

    @Override
    public void addNavSelectionListener(NavSelectionListener listener) {
        navTree.addNavSelectionListener(listener);
    }
    
    @Override
    public void removeNavigableItem(NavCategory category, NavItem navItem) {
        navTree.removeNavigableItem(category, navItem);
    }
    
    @Override
    public void selectNavigableItem(NavCategory category, NavItem navItem) {
        navTree.selectNavigableItem(category, navItem);
    }
    
    public void goHome() {
        navTree.goHome();
    }
    
    public void showDownloads() {
        navTree.showDownloads();
    }

}
