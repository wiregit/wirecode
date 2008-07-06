package org.limewire.ui.swing.nav;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class NavTree extends JPanel {
    
    private final LimeWireNavList limewire;
    private final LibraryNavList library;

    public NavTree() {
        setOpaque(false);
        
        limewire = new LimeWireNavList();
        library = new LibraryNavList();
        
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
    
    private class Listener implements ListSelectionListener {
        
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if(e.getValueIsAdjusting()) // Ignore.
                return; 
            
            JList list = (JList)e.getSource();
            if(list.getSelectedIndex() != -1) { // Something is selected!
                if(limewire.isListSourceFrom(list)) {
                    library.clearSelection();
                } else { // if(library.isListSourceFrom(list)) {
                    limewire.clearSelection();
                }
                Navigator.getInstance().showMainPanel(list.getSelectedValue().toString());
            }
        }
    }
    
}
