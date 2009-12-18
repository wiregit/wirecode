package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.activation.ActivationPanel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ActivationAction extends AbstractAction {

    private Provider<ActivationPanel> activationPanelProvider;
    
    @Inject
    public ActivationAction(Provider<ActivationPanel> activationPanelProvider) {
        super(I18n.tr("&Activate..."));
        
        this.activationPanelProvider = activationPanelProvider;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        ActivationPanel activationPanel = activationPanelProvider.get();
        activationPanel.show();
    }
}
