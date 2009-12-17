package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.activation.ActivationPanel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class ActivationAction extends AbstractAction {

    @Inject
    public ActivationAction() {
        super(I18n.tr("&Activate..."));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        ActivationPanel activationPanel = new ActivationPanel();
        activationPanel.show();
    }
}
