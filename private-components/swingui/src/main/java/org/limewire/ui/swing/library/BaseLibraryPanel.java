package org.limewire.ui.swing.library;

import java.awt.CardLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class BaseLibraryPanel extends JPanel {

    private static final String mainCard = "MAIN_CARD";
    private static final String auxCard = "AUX_CARD";
    
    private CardLayout cardLayout;
    
    private JComponent auxComponent = null;
    
    public BaseLibraryPanel() {
        cardLayout = new CardLayout();
        
        setLayout(cardLayout);
    }
    
    public void setMainCard(JComponent panel) {
        add(panel, mainCard);
    }
    
    public void setAuxCard(JComponent panel) {
        if(auxComponent != null) {
            ((Disposable)auxComponent).dispose();
            auxComponent = null;
        }
        auxComponent = panel;
        panel.validate();
        add(panel, auxCard);
    }
    
    public void showMainCard() {
        if(auxComponent != null) {
            ((Disposable)auxComponent).dispose();
            auxComponent = null;
        }
        cardLayout.show(this, mainCard);
    }
    
    public void showAuxCard() {
        cardLayout.show(this, auxCard);
    }
}
