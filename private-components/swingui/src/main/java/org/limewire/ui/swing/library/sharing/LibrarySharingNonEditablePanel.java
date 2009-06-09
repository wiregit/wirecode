package org.limewire.ui.swing.library.sharing;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.util.I18n;

import net.miginfocom.swing.MigLayout;

import com.google.inject.Inject;

public class LibrarySharingNonEditablePanel {

    private final JPanel component;
    private final HyperlinkButton editButton;
    
    @Inject
    public LibrarySharingNonEditablePanel() {
        component = new JPanel(new MigLayout("insets 0, gap 0, fillx"));
        
        component.setOpaque(false);
        
        component.add(new JLabel(I18n.tr("Shared list with...")), "aligny top, gaptop 5, gapleft 5, wrap");
        
        editButton = new HyperlinkButton(I18n.tr("Edit Sharing"));
        
        component.add(editButton, "aligny top, gaptop 5, gapleft 5, wrap");
    }
    
    public JComponent getComponent() {
        return component;
    }
}
