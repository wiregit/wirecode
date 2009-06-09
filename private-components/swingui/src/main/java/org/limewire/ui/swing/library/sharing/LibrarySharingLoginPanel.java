package org.limewire.ui.swing.library.sharing;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.library.actions.ShowLoginAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Creates Login Panel for inner sharing Nav.
 */
public class LibrarySharingLoginPanel {
    
    private final HyperlinkButton loginButton;
    
    private final JPanel component;
    
    @Inject
    public LibrarySharingLoginPanel(ShowLoginAction loginAction) {
        component = new JPanel();
        
        component.setOpaque(false);
        
        loginButton = new HyperlinkButton(I18n.tr("Share with your friends"), loginAction);
        
        component.add(loginButton);
    }
    
    public JComponent getComponent() {
        return component;
    }
}
