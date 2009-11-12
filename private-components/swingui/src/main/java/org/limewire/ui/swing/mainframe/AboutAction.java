package org.limewire.ui.swing.mainframe;

import java.awt.event.ActionEvent;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class AboutAction extends AbstractAction {

    private final Application application;
    
    @Inject
    public AboutAction(Application application) {
        super(I18n.tr("&About LimeWire..."));
        
        this.application = application;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // if the main frame is not visible on OS X when about is opened, then a zombie window popups up...
        GuiUtils.getMainFrame().setVisible(true);
        new AboutWindow(null, application).showDialog();
    }
}
