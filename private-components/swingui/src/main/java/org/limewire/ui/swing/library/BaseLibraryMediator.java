package org.limewire.ui.swing.library;

import java.awt.CardLayout;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.limewire.ui.swing.nav.NavComponent;
import org.limewire.ui.swing.nav.NavSelectable;

public class BaseLibraryMediator extends JPanel implements Disposable, NavComponent {

    private static final String LIBRRY_CARD = "LIBRARY_CARD";
    private static final String SHARING_CARD = "SHARING_CARD";
    
    private LibrarySelectable librarySelectable;
    
    private CardLayout cardLayout;

    private JComponent libraryComponent = null;
    protected JComponent sharingComponent = null;
    
    public BaseLibraryMediator() {
        cardLayout = new CardLayout();
        
        setLayout(cardLayout);
    }
    
    public void setLibraryCard(JComponent panel) {
        if(libraryComponent != null) {
            ((Disposable)libraryComponent).dispose();
            remove(libraryComponent);
            libraryComponent = null;
        }
        libraryComponent = panel;
        add(panel, LIBRRY_CARD);
    }
    
    public void setSharingCard(JComponent panel) {
        if(sharingComponent != null) {
            ((Disposable)sharingComponent).dispose();
            remove(sharingComponent);
            sharingComponent = null;
        }
        sharingComponent = panel;
        panel.validate();
        add(panel, SHARING_CARD);
    }
    
    public void showLibraryCard() {
        cardLayout.show(this, LIBRRY_CARD);
    }
    
    public void showSharingCard() {
        cardLayout.show(this, SHARING_CARD);
    }

    @Override
    public void dispose() {
        removeAll();
        for(Component component : getComponents()) {
            ((Disposable)component).dispose();
        }
    }

    @Override
    public void select(NavSelectable selectable) {
        librarySelectable.selectAndScroll(selectable.getNavSelectionId());
    }
}
