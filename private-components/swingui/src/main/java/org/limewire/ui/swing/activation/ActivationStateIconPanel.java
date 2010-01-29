package org.limewire.ui.swing.activation;

import java.awt.CardLayout;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.ColoredBusyLabel;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.util.GuiUtils;

public class ActivationStateIconPanel extends JPanel {
    final static String NO_ICON = "no icon";
    final static String BUSY_ICON = "busy icon";
    final static String ERROR_ICON = "error icon";
    
    @Resource private Icon errorIcon;

    private ColoredBusyLabel busyLabel;
    
    public ActivationStateIconPanel() {
        super(new CardLayout());

        GuiUtils.assignResources(this);

        add(NO_ICON, Box.createGlue());

        busyLabel = new ColoredBusyLabel(new Dimension(20,20));
        add(BUSY_ICON, busyLabel);

        add(ERROR_ICON, new IconButton(errorIcon));

        setMinimumSize(new Dimension(20, 20));
        setMaximumSize(new Dimension(20, 20));
        setPreferredSize(new Dimension(20, 20));
    }
    
    public void showNoIcon() {
        CardLayout cardLayout = (CardLayout) (getLayout());
        cardLayout.show(this, NO_ICON);
        busyLabel.setBusy(false);
    }
    
    public void showBusyIcon() {
        CardLayout cardLayout = (CardLayout) (getLayout());
        busyLabel.setBusy(true);
        cardLayout.show(this, BUSY_ICON);
    }

    public void showErrorIcon() {
        CardLayout cardLayout = (CardLayout) (getLayout());
        cardLayout.show(this, ERROR_ICON);
        busyLabel.setBusy(false);
    }

}
