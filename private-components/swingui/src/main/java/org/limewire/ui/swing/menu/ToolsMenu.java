package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.JMenu;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ToolsMenu extends JMenu {
    
    @Inject
    public ToolsMenu(final OptionsDialog optionDialog) {
        super(I18n.tr("Tools"));
        
        add(new AbstractAction(I18n.tr("Options")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!optionDialog.isVisible()) {
                    optionDialog.setLocationRelativeTo(GuiUtils.getMainFrame());
                    optionDialog.setVisible(true);
                }
            }
        });
    }
}
