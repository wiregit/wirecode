package org.limewire.ui.swing.nav;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class NavTree extends JPanel {
    
    private final LimeWireNavList limewire;
    private final LibraryNavList library;
    
    private final List<NavList> navigableLists;

    public NavTree(Navigator navigator) {
        this.navigableLists = new ArrayList<NavList>();
        this.limewire = new LimeWireNavList(navigator);
        this.library = new LibraryNavList(navigator);
        
        navigableLists.add(limewire);
        navigableLists.add(library);
        
        setOpaque(false);
        
        Listener listener = new Listener();
        limewire.addListSelectionListener(listener);
        library.addListSelectionListener(listener);
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 20, 0);
        add(limewire, gbc);
        add(library, gbc);
        
        gbc.weighty = 1;
        add(Box.createGlue(), gbc);
    }
    
    public void goHome() {
        limewire.goHome();
    }
    
    private class Listener implements ListSelectionListener {
        
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if(e.getValueIsAdjusting()) // Ignore.
                return; 
            
            JList list = (JList)e.getSource();
            if(list.getSelectedIndex() != -1) { // Something is selected!
                for(NavList navList : navigableLists) {
                    if(!navList.isListSourceFrom(list)) {
                        navList.clearSelection();
                    } else {
                        navList.navigateToSelection();
                    }
                }
            }
        }
    }
    
}
