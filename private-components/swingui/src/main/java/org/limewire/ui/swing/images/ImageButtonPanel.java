package org.limewire.ui.swing.images;

import javax.swing.Action;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.sharing.components.UnshareButton;

/**
 * Panel to display buttons in a thumbnail view
 */
public class ImageButtonPanel extends JPanel {
    
    /**
     * For painting a cell renderer, its non editable so no need to add actions to it
     */
    public ImageButtonPanel() {              
        setOpaque(false);

        UnshareButton b = new UnshareButton();
        
        setLayout(new MigLayout("fill, insets 0 6 0 6"));
        
        add(b, "alignx right");
    }
    
    /**
     * For painting a cell editor, actions are needed since the button is clickable
     */
    public ImageButtonPanel(Action unshareAction) {
        setOpaque(false);

        UnshareButton b = new UnshareButton(unshareAction);
        
        setLayout(new MigLayout("fill, insets 0 6 0 6"));
        
        add(b, "alignx right");
    }
}
