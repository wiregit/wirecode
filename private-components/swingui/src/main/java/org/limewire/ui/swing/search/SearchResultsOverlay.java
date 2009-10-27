package org.limewire.ui.swing.search;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.jxlayer.JXLayer;
import org.limewire.ui.swing.components.ColoredBusyLabel;
import org.limewire.ui.swing.util.I18n;

/**
 * This class accepts a component which displays search results and sponsored results
 * as an argument. If LimeWire has not fully connected to its peers AND has not
 * received any results for the user's query, then it doesn't show the search results component,
 * but instead shows a "LimeWire is connecting..." message with a busy icon.
 * Otherwise, it shows the given component.
 *
 */
class SearchResultsOverlay extends JPanel
{
    /**
     * The type of overlay which should be placed over the search results.
     * NONE indicates that no overlay should be shown.
     * AWAITING_CONNECTIONS indicates that an overlay with a busy icon and a "LimeWire is connecting..."
     * message should be shown.
     */
    public enum OverlayType {
        NONE, 
        AWAITING_CONNECTIONS
    }
    
    private JComponent awaitingConnectionsHintPanel;
    private final JXLayer jxlayer;
    
    /**
     * @param searchResultsComponent -- the component containing the search results (probably a scrollpane)
     */
    public SearchResultsOverlay(JComponent searchResultsComponent) {
        super(new BorderLayout());
        
        jxlayer = new JXLayer<JComponent>(searchResultsComponent);
        jxlayer.getGlassPane().setLayout(new BorderLayout());
        
        add(jxlayer, BorderLayout.CENTER);
    }

    /**
     * This method controls whether an overlay should be shown and if so, which type of overlay.
     * 
     * @param overlayType -- the overlay type. See the type definition for more info.
     */
    void setOverlayType(OverlayType overlayType) {
        switch (overlayType) {
        case AWAITING_CONNECTIONS:
            installOverlay(getAwaitingConnectionsPanel());
            break;
            
        case NONE:
            uninstallOverlay();
            break;
            
        default:
            throw new IllegalStateException("invalid type: " + overlayType); 
        }
    }
          
    private void installOverlay(JComponent component) {
        jxlayer.getGlassPane().setVisible(false);
        jxlayer.getGlassPane().removeAll();
        jxlayer.getGlassPane().add(component);
        jxlayer.getGlassPane().setVisible(true);
    }
    
    private void uninstallOverlay() {
        jxlayer.getGlassPane().setVisible(false);
        jxlayer.getGlassPane().removeAll();
    }
    
    private JComponent getAwaitingConnectionsPanel() {
        if (awaitingConnectionsHintPanel == null) {
            awaitingConnectionsHintPanel = createAwaitingConnectionsPanel();
        }

        return awaitingConnectionsHintPanel;
    }

    private JPanel createAwaitingConnectionsPanel() {
        JPanel aPanel = new JPanel();
        aPanel.setLayout(new BoxLayout(aPanel, BoxLayout.Y_AXIS));

        aPanel.add(Box.createVerticalGlue());
        
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
        
        aPanel.add(innerPanel);

        aPanel.add(Box.createVerticalGlue());

        return aPanel;
    }
}