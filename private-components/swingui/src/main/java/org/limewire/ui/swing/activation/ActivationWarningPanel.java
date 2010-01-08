package org.limewire.ui.swing.activation;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.ColoredBusyLabel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.ResizeUtils;

public class ActivationWarningPanel {

    public static enum Mode {
        EMPTY, SPINNER, WARNING
    }
    
    @Resource
    private Icon warningIcon;
    
    private final JPanel panel;
    private ColoredBusyLabel busyLabel;
    
    private Mode currentMode = Mode.EMPTY;
    
    public ActivationWarningPanel() {
        GuiUtils.assignResources(this);
        
        panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        ResizeUtils.forceSize(panel, new Dimension(20, 20));
    }
    
    public JComponent getComponent() {
        return panel;
    }
    
    public void setActivationMode(Mode mode) {
        if(currentMode == mode) {
            return;
        }
        if(currentMode == Mode.SPINNER) {
            busyLabel.setBusy(false);
            busyLabel = null;
        }
        currentMode = mode;
        
        panel.removeAll();

        switch(mode) {
        case WARNING:
            panel.add(new JLabel(warningIcon), BorderLayout.CENTER);
            break;
        case SPINNER:
            busyLabel = new ColoredBusyLabel(new Dimension(20,20));
            panel.add(busyLabel, BorderLayout.CENTER);
            panel.revalidate();
            busyLabel.setBusy(true);
            break;
        }
    }
}
