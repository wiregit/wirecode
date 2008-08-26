package org.limewire.ui.swing.friends;

import java.awt.event.ActionEvent;

import javax.swing.text.JTextComponent;

public class DeleteAction extends AbstractSelectionRequiredTextAction {

    public DeleteAction(JTextComponent component) {
        super("Delete", component);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JTextComponent textComponent = getTextComponent(e);
        StringBuilder bldr = new StringBuilder(textComponent.getText());
        bldr.delete(textComponent.getSelectionStart(), textComponent.getSelectionEnd());
        textComponent.setText(bldr.toString());
    }
}
