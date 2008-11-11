package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.JMenu;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ToolsMenu extends JMenu {
    
    @Inject
    public ToolsMenu(final Provider<OptionsDialog> optionDialog) {
        super(I18n.tr("Tools"));
        
        add(new JMenu(I18n.tr("Library Manager")));
        add(new JMenu(I18n.tr("Downloads")));
        add(new JMenu(I18n.tr("Uploads")));
        addSeparator();
        add(new JMenu(I18n.tr("Advanced Search")));
        add(new JMenu(I18n.tr("What's New Search")));
        addSeparator();
        add(new JMenu(I18n.tr("Advanced Tools")));
        addSeparator();
        add(new AbstractAction(I18n.tr("Options")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                OptionsDialog options = optionDialog.get();
                if(!options.isVisible()) {
                    options.setLocationRelativeTo(GuiUtils.getMainFrame());
                    options.setVisible(true);
                }
            }
        });
    }
}
