package org.limewire.ui.swing.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.VerticalLayout;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.browse.BrowseSearch;
import org.limewire.ui.swing.search.model.browse.BrowseStatus;
import org.limewire.ui.swing.search.model.browse.BrowseStatus.BrowseState;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class BrowseStatusPanel extends JXPanel {

    private BrowseStatus status;

    private JButton warningButton;
    private JXPanel refreshPanel;
    private JButton refreshButton;
    
    @Resource
    private Icon warningIcon;

    private SearchResultsModel searchResultsModel;
    
    public BrowseStatusPanel(SearchResultsModel searchResultsModel){
        GuiUtils.assignResources(this);
        this.searchResultsModel = searchResultsModel;
        
        setOpaque(false);        
        initializeComponents();        
        layoutComponents();
        setInitialVisibility();
    }

    private void setInitialVisibility() {
        warningButton.setVisible(false);
        refreshPanel.setVisible(false);        
    }

    private void initializeComponents() {        
        warningButton = new IconButton(warningIcon);
        warningButton.setAction(new WarningAction());
        
        refreshPanel = new JXPanel();
        refreshButton = new HyperlinkButton(new RefreshAction());
    }

    private void layoutComponents() {        
        refreshPanel.add(new JLabel(I18n.tr("There are updates!")));
        refreshPanel.add(refreshButton);
        
        add(warningButton);
        add(refreshPanel);
    }

    public void setBrowseStatus(BrowseStatus status) {
        this.status = status;
        update();
    }

    private void update() {
        warningButton.setVisible(status != null && status.getState() == BrowseState.PARTIAL_FAIL);
        refreshPanel.setVisible(status != null && status.getState() == BrowseState.UPDATED);
    }
    
    private void showFailedBrowses(){
        //TODO: present this properly
        List<String> friends = new ArrayList<String>();
        List<String> p2pUsers = new ArrayList<String>();
        for(Friend person : status.getFailed()){
            if(person.isAnonymous()){
                p2pUsers.add(person.getRenderName());
            } else {
                friends.add(person.getRenderName());
            }            
        }
        JXPanel titlePanel = new JXPanel(); 
        titlePanel.setBackground(Color.BLACK);
        titlePanel.add(new JLabel(I18n.tr("Failed Browses")));
        //TODO close button
        
        JXPanel centerPanel = new JXPanel(new VerticalLayout());
        if(friends.size() > 0){
            centerPanel.add(new JLabel(I18n.tr("Friends")));
        }
        for (String name : friends){
            centerPanel.add(new JLabel(name));
        }
        
        if(p2pUsers.size() > 0){
            centerPanel.add(new JLabel(I18n.tr("P2P Users")));
        }
        for (String name : p2pUsers){
            centerPanel.add(new JLabel(name));
        }
        
        JXPanel mainPanel = new JXPanel(new BorderLayout());
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        JPopupMenu popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.add(mainPanel);
        popup.show(warningButton, 0, 0);
    }
    
    private class RefreshAction extends AbstractAction {
        public RefreshAction(){
            super(I18n.tr("Refresh"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            BrowseSearch browseSearch = status.getBrowseSearch();
            
            browseSearch.stop();
            searchResultsModel.clear();
            setBrowseStatus(null);
            browseSearch.repeat();
        }        
    }
  
    
    private class WarningAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            showFailedBrowses();
        }        
    }
}
