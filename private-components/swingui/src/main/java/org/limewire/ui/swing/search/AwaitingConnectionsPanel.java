package org.limewire.ui.swing.search;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.limewire.ui.swing.components.ColoredBusyLabel;
import org.limewire.ui.swing.util.I18n;

/**
 * This panel is shown if LimeWire has not fully connected to its peers AND has not
 * received any results for the user's query.
 * It shows a "LimeWire is connecting..." message with a busy icon.
 *
 */
class AwaitingConnectionsPanel extends JPanel
{
    AwaitingConnectionsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(Box.createVerticalGlue());
        
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        ColoredBusyLabel busyLabel = new ColoredBusyLabel(new Dimension(40, 40));
        busyLabel.setBusy(true);
        busyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        innerPanel.add(busyLabel);
        
        JLabel connectingLabel = new JLabel(I18n.tr("LimeWire is connecting..."));
        connectingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        innerPanel.add(connectingLabel);
        
        innerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        innerPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        
        add(innerPanel);

        add(Box.createVerticalGlue());
    }
}