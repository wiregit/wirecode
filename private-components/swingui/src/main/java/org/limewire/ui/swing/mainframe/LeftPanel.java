package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.limewire.ui.swing.nav.DownloadSummaryPanel;
import org.limewire.ui.swing.nav.FilesSharingSummaryPanel;
import org.limewire.ui.swing.nav.NavSelectionListener;
import org.limewire.ui.swing.nav.NavTree;
import org.limewire.ui.swing.nav.NavigableTree;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.SearchBar;
import org.limewire.ui.swing.util.Line;

class LeftPanel extends JPanel implements NavigableTree {

    private final SearchBar searchBar;
    private final NavTree navTree;
    private final DownloadSummaryPanel downloadPanel;
    private final FilesSharingSummaryPanel filesPanel;
    
    public LeftPanel() {
        this.searchBar = new SearchBar();
        this.navTree = new NavTree();
        this.downloadPanel = new DownloadSummaryPanel();
        this.filesPanel = new FilesSharingSummaryPanel();
        
        setMinimumSize(new Dimension(150, 0));
        setMaximumSize(new Dimension(150, Integer.MAX_VALUE));
        setPreferredSize(new Dimension(150, 700));
        
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
        add(new Line(Color.BLACK), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 0, 0);
        gbc.weighty = 0;
        gbc.gridx = GridBagConstraints.REMAINDER;
        add(downloadPanel, gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weighty = 0;
        gbc.gridx = GridBagConstraints.REMAINDER;
        add(new Line(Color.BLACK), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 0);
        gbc.weighty = 0;
        gbc.gridx = GridBagConstraints.REMAINDER;
        add(filesPanel, gbc);
    }
    
    @Override
    public void addNavigableItem(Navigator.NavItem navItem, String name) {
        navTree.addNavigableItem(navItem, name);
    }
    
    @Override
    public void removeNavigableItem(Navigator.NavItem navItem, String name) {
        navTree.removeNavigableItem(navItem, name);
    }
    
    @Override
    public void addNavSelectionListener(NavSelectionListener listener) {
        navTree.addNavSelectionListener(listener);
    }
    
    public void goHome() {
        navTree.goHome();
    }

}
