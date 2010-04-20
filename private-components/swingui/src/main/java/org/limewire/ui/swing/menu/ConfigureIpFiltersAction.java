package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

public class ConfigureIpFiltersAction extends AbstractAction {
    
    public ConfigureIpFiltersAction() {
        super(I18n.tr("Configure peers to connect to..."));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new ConfigureIpFiltersDialog().setVisible(true);
    }

}
