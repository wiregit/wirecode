package org.limewire.ui.swing.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.painter.ComponentBackgroundPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.browse.BrowseStatus;
import org.limewire.ui.swing.search.model.browse.BrowseStatus.BrowseState;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class BrowseStatusPanel extends JXPanel {

    private BrowseStatus status;

    private JButton warningButton;
    private JXPanel refreshPanel;
    private JButton refreshButton;
    private JLabel updatesLabel;
    
    @Resource private Icon warningIcon;    
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color innerBorder;
    @Resource private Color innerBackground;
    @Resource private Color bevelLeft;
    @Resource private Color bevelTop1;
    @Resource private Color bevelTop2;
    @Resource private Color bevelRight;
    @Resource private Color bevelBottom;
    @Resource private Font font;
    @Resource private Color foreground;
    @Resource private Color headerForeground;
    @Resource private Font headerFont;

    private SearchResultsModel searchResultsModel;
    
    public BrowseStatusPanel(SearchResultsModel searchResultsModel){
        GuiUtils.assignResources(this);
        this.searchResultsModel = searchResultsModel;
        
        setOpaque(false);        
        initializeComponents();        
        layoutComponents();
        update();
    }

    private void initializeComponents() {        
        warningButton = new IconButton(warningIcon);
        warningButton.addActionListener(new WarningAction());
        
        refreshPanel = new JXPanel();
        refreshPanel.setOpaque(false);
        refreshPanel.setBackgroundPainter(createRefreshBackgroundPainter());
        
        updatesLabel = new JLabel(I18n.tr("There are updates!"));
        updatesLabel.setFont(font);
        updatesLabel.setForeground(foreground);
        
        refreshButton = new HyperlinkButton(new RefreshAction());
        refreshButton.setFont(font);
    }

    private void layoutComponents() {    
        refreshPanel.setLayout(new MigLayout("insets 6 8 6 8, gap 0, novisualpadding, aligny 50%, fill"));        
        
        refreshPanel.add(updatesLabel);
        refreshPanel.add(refreshButton, "gapleft 3");
        
        setLayout(new MigLayout("insets 0, gap 0, novisualpadding, filly"));
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
        
        for(Friend person : status.getFailedFriends()){
            if(person.isAnonymous()){
                p2pUsers.add(person.getRenderName());
            } else {
                friends.add(person.getRenderName());
            }            
        }
        
        final JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        
        JXPanel titlePanel = new JXPanel(); 
        titlePanel.setBackground(Color.BLACK);
        JLabel title = new JLabel(I18n.tr("Failed Browses"));
        title.setForeground(Color.WHITE);
        //TODO: close icon
        JButton closeButton = new JButton("X");
        closeButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                popup.setVisible(false);
            }            
        });      
        
        titlePanel.add(title);
        titlePanel.add(closeButton);
        
        JXPanel centerPanel = new JXPanel(new MigLayout("insets 0 5 5 5, gap 0, novisualpadding, fill"));
        if(friends.size() > 0){
            centerPanel.add(createHeaderLabel(I18n.tr("Friends")), "gaptop 5, wrap");
        }
        for (String name : friends){
            centerPanel.add(createItemLabel(name), "wrap");
        }
        
        if(p2pUsers.size() > 0){
            centerPanel.add(createHeaderLabel(I18n.tr("P2P Users")), "gaptop 5, wrap");
        }
        for (String name : p2pUsers){
            centerPanel.add(createItemLabel(name), "wrap");
        }
        
        JXPanel mainPanel = new JXPanel(new BorderLayout());
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        popup.setLayout(new BorderLayout());
        popup.add(mainPanel);
        popup.show(warningButton, 0, 0);
    }
    
    private JLabel createHeaderLabel(String text){
        JLabel header = new JLabel(text);
        header.setForeground(headerForeground);
        header.setFont(headerFont);
        return header;
    }
    
    private JLabel createItemLabel(String text){
        JLabel item = new JLabel(text);
        item.setForeground(foreground);
        item.setFont(font);
        return item;
    }
    
    private Painter<JXPanel> createRefreshBackgroundPainter() {
        return new ComponentBackgroundPainter<JXPanel>(innerBackground, innerBorder, bevelLeft, bevelTop1, 
                bevelTop2, bevelRight,bevelBottom, arcWidth, arcHeight,
                AccentType.NONE);
    }
        
    private class RefreshAction extends AbstractAction {
        public RefreshAction(){
            super(I18n.tr("Refresh"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new BrowserRefresher(status.getBrowseSearch(), searchResultsModel).refresh();
        }        
    }
  
    
    private class WarningAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            showFailedBrowses();
        }        
    }
}
