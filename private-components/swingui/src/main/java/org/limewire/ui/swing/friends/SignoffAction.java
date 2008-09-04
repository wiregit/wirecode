package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;

public class SignoffAction extends AbstractAction {
    public SignoffAction() {
        super(tr("Sign off"));
    }
    
    public SignoffAction(Icon icon) {
        super("", icon);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new SignoffEvent().publish();
    }
}