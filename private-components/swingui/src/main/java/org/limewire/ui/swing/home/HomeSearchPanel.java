package org.limewire.ui.swing.home;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.TextTabPanel;

public class HomeSearchPanel extends JXPanel {
    
    private final JTextField textField;
    private final List<HomeAction> searchCategories;
    private final TextTabPanel actionPanel;
    private final JXButton search;
    private final JXButton searchFriends;
    private final SearchHandler searchHandler;
    
    public HomeSearchPanel(SearchHandler searchHandler) {
        this.searchHandler = searchHandler;
        this.textField = new JTextField();
        this.searchCategories = new ArrayList<HomeAction>();
        searchCategories.add(new HomeAction("All", SearchCategory.ALL, true));
        searchCategories.add(new HomeAction("Audio", SearchCategory.AUDIO));
        searchCategories.add(new HomeAction("Video", SearchCategory.VIDEO));
        searchCategories.add(new HomeAction("Images", SearchCategory.IMAGES));
        searchCategories.add(new HomeAction("Documents", SearchCategory.DOCUMENTS));
        this.search = new JXButton("Search");
        this.searchFriends = new JXButton("Search Friends");
        this.actionPanel = new TextTabPanel(searchCategories);
        
        SearchAction action = new SearchAction();
        search.addActionListener(action);
        searchFriends.addActionListener(action);
        textField.addActionListener(action);
        
        RectanglePainter<JXPanel> painter = new RectanglePainter<JXPanel>(5, 5, 5, 5, 50, 50, true, Color.LIGHT_GRAY, 0f, Color.GRAY);
        setBackgroundPainter(painter);
        setLayout(new GridBagLayout());
        search.setOpaque(false);
        searchFriends.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        
        // A little glue to offset the 'Advanced Search' on the right.
        gbc.gridheight = 3;
        gbc.ipadx = 75;
        add(Box.createGlue(), gbc);
        
        gbc.ipadx = 0;
        gbc.gridheight = 1;
        
        // To push in the 'All | Audio | Video...'
        gbc.weightx = 1;
        add(Box.createGlue(), gbc);
        
        gbc.weightx = 0;
        add(actionPanel, gbc);

        // To push in the 'All | Audio | Video...'
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        add(Box.createGlue(), gbc);
        
        // To be ontop of the 'Advanced Search'
        gbc.weightx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(Box.createGlue(), gbc);
        
        gbc.weightx = 1;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        add(textField, gbc);
        
        gbc.weightx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.insets = new Insets(0, 5, 5, 15);
        JLabel advancedSearch = new JLabel("Advanced Search");
        FontUtils.changeFontSize(advancedSearch, -1);
        add(advancedSearch, gbc);
        
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 0, 0, 0);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new GridBagLayout());
        add(buttonPanel, gbc);
        
        // To be ontop of the 'Advanced Search'
        gbc.weightx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(Box.createGlue(), gbc);
        
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 25, 0, 0);
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        buttonPanel.add(search, gbc);
        gbc.insets = new Insets(0, 50, 0, 25);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        buttonPanel.add(searchFriends, gbc);
             
        setMinimumSize(new Dimension(500, 100));
        setPreferredSize(new Dimension(500, 100));
    }
    
    private SearchCategory getSelectedSearchCategory() {
        for(HomeAction action : searchCategories) {
            if(action.getValue(Action.SELECTED_KEY) == Boolean.TRUE)
                return action.getSearchCategory();
        }
        return null;
    }
    
    @Override
    public boolean requestFocusInWindow() {
        return textField.requestFocusInWindow();
    }
    
    SearchInfo getSearchInfo() {
        return new DefaultSearchInfo(textField.getText(), getSelectedSearchCategory());
    }
    
    private class SearchAction extends AbstractAction { 
        
        @Override
        public void actionPerformed(ActionEvent e) {
            SearchInfo info = getSearchInfo();
            searchHandler.doSearch(info);
        }
    }
    
    private static class HomeAction extends AbstractAction {
        public HomeAction(String name, SearchCategory category) {
            this(name, category, false);
        }
        
        public HomeAction(String name, SearchCategory category, boolean selected) {
            super(name);
            putValue("limewire.searchCategory", category);
            putValue(Action.SELECTED_KEY, selected);
        }
        
        SearchCategory getSearchCategory() {
            return (SearchCategory)getValue("limewire.searchCategory");
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            
        }
    }
}
