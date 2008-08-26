package org.limewire.ui.swing.friends;

import javax.swing.text.JTextComponent;

abstract class AbstractSelectionRequiredTextAction extends AbstractTextAction {
    private final JTextComponent component;
    
    public AbstractSelectionRequiredTextAction(String name, JTextComponent component, String... actions) {
        super(name, actions);
        this.component = component;
    }
    
    @Override
    public boolean isEnabled() {
        return component.getSelectedText() != null;
    }
}
