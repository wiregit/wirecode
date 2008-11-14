package org.limewire.ui.swing.library;

import java.awt.CardLayout;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.limewire.ui.swing.nav.NavComponent;
import org.limewire.ui.swing.nav.NavSelectable;

public class BaseLibraryMediator extends JPanel implements Disposable, NavComponent {

    private static final String mainCard = "MAIN_CARD";
    private static final String auxCard = "AUX_CARD";
    
    private LibrarySelectable librarySelectable;
    
    private CardLayout cardLayout;
    
    private JComponent auxComponent = null;
    
    public BaseLibraryMediator() {
        cardLayout = new CardLayout();
        
        setLayout(cardLayout);
    }
    
    public void setMainCard(JComponent panel) {
        add(panel, mainCard);
    }
    
    public void setAuxCard(JComponent panel) {
        if(auxComponent != null) {
            ((Disposable)auxComponent).dispose();
            remove(auxComponent);
            auxComponent = null;
        }
        auxComponent = panel;
        panel.validate();
        add(panel, auxCard);
    }
    
    public void showMainCard() {
        if(auxComponent != null) {
            ((Disposable)auxComponent).dispose();
            remove(auxComponent);
            auxComponent = null;
        }
        cardLayout.show(this, mainCard);
    }
    
    public void showAuxCard() {
        cardLayout.show(this, auxCard);
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
