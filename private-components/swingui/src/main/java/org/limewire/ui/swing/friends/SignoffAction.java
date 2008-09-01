package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class SignoffAction extends AbstractAction {
    public SignoffAction() {
        super(tr("Sign off"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new SignoffEvent().publish();
    }
}