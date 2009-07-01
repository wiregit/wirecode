package org.limewire.ui.swing.statusbar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.PopupHeaderBar;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PainterUtils;

import com.google.inject.Inject;

public class FileProcessingPopupContentPanel extends JPanel {

    @Resource private Color border = PainterUtils.TRASPARENT;
    
    @Inject
    public FileProcessingPopupContentPanel(final FileProcessingPanel parent) {
        super(new BorderLayout());
        
        GuiUtils.assignResources(this);
        
        add(new PopupHeaderBar(I18n.tr("Adding Files"), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                parent.repaint();
            }
        }), BorderLayout.NORTH);
        
        setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, border));
        
    }
}
