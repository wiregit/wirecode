package org.limewire.ui.swing.library;

import java.awt.CardLayout;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.limewire.ui.swing.components.Disposable;

class LibraryMediator extends JPanel implements Disposable {

    private static final String LIBRRY_CARD = "LIBRARY_CARD";
    private static final String SHARING_CARD = "SHARING_CARD";
    
    private CardLayout cardLayout;

    private JComponent libraryComponent = null;
    private JComponent sharingComponent = null;
    
    public LibraryMediator() {
        cardLayout = new CardLayout();
        
        setLayout(cardLayout);
    }
    
    protected void setLibraryCard(JComponent panel) {
        if(libraryComponent != null) {
            remove(libraryComponent);
            ((Disposable)libraryComponent).dispose();
            libraryComponent = null;
        }
        libraryComponent = panel;
        add(panel, LIBRRY_CARD);
    }
    
    protected boolean isSharingCardSet() {
        return sharingComponent != null;
    }
    
    protected void setSharingCard(JComponent panel) {
        if(sharingComponent != null) {
            remove(sharingComponent);
            ((Disposable)sharingComponent).dispose();
            sharingComponent = null;
        }
        sharingComponent = panel;
        panel.validate();
        add(panel, SHARING_CARD);
    }
    
    public void showLibraryCard() {
        cardLayout.show(this, LIBRRY_CARD);
        validate();
        repaint();
    }
    
    protected void showSharingCard() {
        cardLayout.show(this, SHARING_CARD);
    }

    @Override
    public void dispose() {        
        Component[] components = getComponents();
        removeAll();
        for(Component component : components) {
            ((Disposable)component).dispose();
        }
    }
}
