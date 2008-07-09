package org.limewire.ui.swing.home;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.Line;

public class HomeSearchPanel extends JPanel {
    
    private final JTextField textField;
    private final ButtonGroup buttonGroup;
    private final HomeButtonBed all;
    private final HomeButtonBed audio;
    private final HomeButtonBed video;
    private final HomeButtonBed images;
    private final HomeButtonBed documents;
    private final JButton search;
    private final JButton searchFriends;
    private final SearchHandler searchHandler;
    
    public HomeSearchPanel(SearchHandler searchHandler) {
        this.searchHandler = searchHandler;
        this.buttonGroup = new ButtonGroup();
        this.textField = new JTextField();
        this.all = new HomeButtonBed("All", buttonGroup);
        this.audio = new HomeButtonBed("Audio", buttonGroup);
        this.video = new HomeButtonBed("Video", buttonGroup);
        this.images = new HomeButtonBed("Images", buttonGroup);
        this.documents = new HomeButtonBed("Documents", buttonGroup);
        this.search = new JButton("Search");
        this.searchFriends = new JButton("Search Friends");
        
        all.setSelected(true);
        
        SearchAction action = new SearchAction();
        search.addActionListener(action);
        searchFriends.addActionListener(action);
        textField.addActionListener(action);
        
        setOpaque(false);
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
        gbc.insets = new Insets(0, 1, 5, 1);
        add(all, gbc);
        add(new Line(Color.BLACK), gbc);
        add(audio, gbc);
        add(new Line(Color.BLACK), gbc);
        add(video, gbc);
        add(new Line(Color.BLACK), gbc);
        add(images, gbc);
        add(new Line(Color.BLACK), gbc);
        add(documents, gbc);

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
        gbc.insets = new Insets(0, 5, 5, 0);
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
    
    @Override
    public boolean requestFocusInWindow() {
        return textField.requestFocusInWindow();
    }
    
    SearchInfo getSearchInfo() {
        return new DefaultSearchInfo(textField.getText());
    }
    
    private class SearchAction extends AbstractAction { 
        
        @Override
        public void actionPerformed(ActionEvent e) {
            SearchInfo info = getSearchInfo();
            searchHandler.doSearch(info);
        }
    }
    
    private static class HomeButtonBed extends JPanel {
        private final HomeButton button;
        
        public HomeButtonBed(String name, ButtonGroup group) {
            this.button = new HomeButton(name, group);
            
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            add(button, gbc);
        }
        
        void setSelected(boolean selected) {
            button.setSelected(selected);
        }
    }
    
    private static class HomeButton extends JToggleButton {
        public HomeButton(String name, ButtonGroup group) {
            super(name);
            group.add(this);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setMargin(new Insets(0, 5, 0, 5));
            addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if(e.getStateChange() == ItemEvent.SELECTED) {
                        HomeButton.this.getParent().setBackground(Color.LIGHT_GRAY);
                    } else {
                        HomeButton.this.getParent().setBackground(null);
                    }
                    
                }
            });
            
            addMouseListener(new MouseAdapter() {
               @Override
                public void mouseEntered(MouseEvent e) {
                   if(!isSelected()) {
                       HomeButton.this.getParent().setBackground(Color.CYAN);
                   }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if(!isSelected()) {
                        HomeButton.this.getParent().setBackground(null);
                    }
                }
            });
        }
    }

}
