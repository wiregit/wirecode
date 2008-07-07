package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.limewire.ui.swing.nav.NavSelectionListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorImpl;
import org.limewire.ui.swing.util.Line;

public class LimeWireSwingUI extends JPanel {
    
    private final TopPanel topPanel;
    private final LeftPanel leftPanel;
    private final MainPanel mainPanel;
    private final StatusPanel statusPanel;
    private final NavigatorImpl navigator;
    
    public LimeWireSwingUI() {
        this.mainPanel = new MainPanel();
        this.leftPanel = new LeftPanel();
        this.navigator = new NavigatorImpl(mainPanel, leftPanel);
        this.topPanel = new TopPanel();
        this.statusPanel = new StatusPanel();
        
        leftPanel.addNavSelectionListener(new NavSelectionListener() {
            @Override
            public void navItemSelected(Navigator.NavItem target, String name) {
                navigator.showNavigablePanel(target, name);
            }
        });
        navigator.addDefaultNavigableItems();
        
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;        
        add(new Line(Color.BLACK), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(topPanel, gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(new Line(Color.BLACK), gbc);
        
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.gridwidth = 1;
        add(leftPanel, gbc);
        
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        add(new Line(Color.BLACK), gbc);
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(mainPanel, gbc);
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(new Line(Color.BLACK), gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(statusPanel, gbc);
    }
    
    public void goHome() {        
        leftPanel.goHome();
    }
    
    

}
