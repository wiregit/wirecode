package org.limewire.ui.swing.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.ColoredBusyLabel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;

/**
 * A message panel that appears (sometimes) above the search results.
 * If the user tries to start a search before the application
 * has finished loading or before it has fully connected, the messages
 * "LimeWire will start your search right after it finishes loading." and
 * "You might not receive many results until LimeWire is fully connected."
 * will be shown here.  It also may show a hint to the user regarding how to 
 * switch to the classic search results view.
 */
class SearchResultsMessagePanel extends JXPanel {
    
    /**
     * This enumeration indicates which type of message should be shown.
     * NONE shows no message at all.
     * CONNECTING_TO_ULTRAPEERS shows the message "You might not receive many results until LimeWire is fully connected"
     * CLASSIC_SEARCH_RESULTS_HINT shows an arrow pointing to the button that switches to the classic table view of search results.
     */
    public enum MessageType {
        NONE,                       
        CONNECTING_TO_ULTRAPEERS
    }
    
    // We will only show the message panel if the user tries to search for something before LimeWire
    // has connected to all of its ultra peers. So, let's defer creating it until we're sure we need it.
    private MessagePanel messagePanel;
    
    private JPanel currentPanel = null;
    
    @Resource private int height;
    @Resource private Color background;
    @Resource private Font switchFont;
    @Resource private Color switchColor;
    
    public SearchResultsMessagePanel() {
        GuiUtils.assignResources(this);
        
        setLayout(new BorderLayout());
    }

    /**
     * This method updates the display of the message panel. The message panel can show
     * a connecting message, a hint for finding the classic search results
     * view, or nothing at all.
     * 
     * @param messageType - the type of message that should be shown
     */
    public void setMessageType(MessageType messageType) {
        switch (messageType) {
        case CONNECTING_TO_ULTRAPEERS:
            getMessagePanel().setMessage(I18n.tr("You might not receive many results until LimeWire is fully connected"));
            installPanel(getMessagePanel());
            break;
            
        case NONE:
            installPanel(null);
            break;
            
        default:
            throw new IllegalStateException("invalid type: " + messageType);                 
        }
    }
    
    /*
     * Remove the existing panel and use the given panel for the user interface
     */
    private void installPanel(JPanel panel) {
        if (currentPanel != panel) {
            removeAll();
            if (panel != null) {
                add(panel);
            }
            currentPanel = panel;
        }
        setVisible(currentPanel != null);
    }

    private MessagePanel getMessagePanel() {
        if (messagePanel == null) {
            messagePanel = new MessagePanel();
        }
        
        return messagePanel;
    }
    
    private class MessagePanel extends JPanel {
        private JLabel messageLabel;

        public MessagePanel() {
            super(new MigLayout("insets 0, gap 0, fill, aligny center"));
            ResizeUtils.forceHeight(this, height);
            setBackground(background);

            // the message should be right aligned. so, let's add a glue component to push the message
            // over as far right as possible.
            add(Box.createHorizontalGlue(), "gapbefore 4, alignx center, push");
                       
            // the message text should be set via the setMessage method
            messageLabel = new JLabel("");
            messageLabel.setFont(switchFont);
            messageLabel.setForeground(switchColor);
            add(messageLabel, "gapbefore 4, aligny center, alignx right");

            // a busy label showing that the application is still loading or connecting
            ColoredBusyLabel busyLabel = new ColoredBusyLabel(new Dimension(20, 20));
            busyLabel.setBusy(true);
            add(busyLabel, "gapbefore 25, gapright 25, alignx right");
        }
       
        void setMessage(String message) {
            messageLabel.setText(message);
        }
    } 
}
