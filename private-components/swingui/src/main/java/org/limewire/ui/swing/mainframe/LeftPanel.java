package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.downloads.DownloadSummaryPanel;
import org.limewire.ui.swing.nav.FilesSharingSummaryPanel;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavSelectionListener;
import org.limewire.ui.swing.nav.NavTree;
import org.limewire.ui.swing.nav.NavigableTree;
import org.limewire.ui.swing.nav.Navigator.NavCategory;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class LeftPanel extends JPanel implements NavigableTree {

    private final NavTree navTree;
    
    /** The color of the lines separating the GUI panels */
    @Resource
    private Color lineColor;

    @Inject
    public LeftPanel(DownloadListManager downloadListManager, FilesSharingSummaryPanel filesSharingPanel) {
    	GuiUtils.assignResources(this);
        this.navTree = new NavTree();
        DownloadSummaryPanel downloadPanel =  new DownloadSummaryPanel(downloadListManager.getDownloads());
        setMinimumSize(new Dimension(150, 0));
        setMaximumSize(new Dimension(150, Integer.MAX_VALUE));
        setPreferredSize(new Dimension(150, 700));
        
        downloadPanel.addMouseListener(GuiUtils.getActionHandListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                showDownloads();
            }            
        }));
             
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.insets = new Insets(19, 0, 0, 0);
        JScrollPane scrollableNav = new JScrollPane(navTree);
        scrollableNav.setOpaque(false);
        scrollableNav.getViewport().setOpaque(false);
        scrollableNav.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollableNav.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollableNav.setBorder(null);
        add(scrollableNav, gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(new Line(lineColor), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 0, 0);
        gbc.weighty = 0;
        add(downloadPanel, gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weighty = 0;
        add(new Line(lineColor), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 0);
        gbc.weighty = 0;
        add(filesSharingPanel, gbc);
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
