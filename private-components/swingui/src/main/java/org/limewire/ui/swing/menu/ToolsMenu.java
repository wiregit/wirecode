package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.JMenu;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Singleton;
import com.limegroup.gnutella.gui.GUIUtils;

@Singleton
public class ToolsMenu extends JMenu {

    private OptionsDialog optionDialog;
    
    public ToolsMenu() {
        super(I18n.tr("Tools"));
        
        add(new AbstractAction(I18n.tr("Options")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(optionDialog == null) {
                    optionDialog = new OptionsDialog();
                }
                if(!optionDialog.isVisible()) {
                    GUIUtils.centerOnScreen(optionDialog);
                    optionDialog.setVisible(true);
                }
            }
        });
    }
}
