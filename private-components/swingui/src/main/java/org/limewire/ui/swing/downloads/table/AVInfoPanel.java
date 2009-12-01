package org.limewire.ui.swing.downloads.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Content panel for the Anti-Virus Info dialog in the downloads table.
 */
public class AVInfoPanel extends JPanel {
    private static final String AVG_URL = "http://www.avg.com/";

    @Resource private Font doNotShowFont;
    @Resource private Color doNotShowForeground;
    @Resource private Font headingFont;
    @Resource private Color headingForeground;
    @Resource private Font messageFont;
    @Resource private Color messageForeground;
    @Resource private Font threatHeadingFont;
    @Resource private Color threatHeadingForeground;
    @Resource private Icon threatIcon;
    @Resource private Icon vendorIcon;
    
    private JLabel headingLabel;
    private HTMLLabel messageLabel;
    
    private JButton okButton;
    private JCheckBox doNotShowCheckBox;
    private JLabel vendorLabel;
    
    /**
     * Constructs an AVInfoPanel.
     */
    @Inject
    public AVInfoPanel() {
        GuiUtils.assignResources(this);
        
        headingLabel = new JLabel();
        
        messageLabel = new HTMLLabel();
        messageLabel.setHtmlFont(messageFont);
        messageLabel.setHtmlForeground(messageForeground);
        messageLabel.setMargin(new Insets(0, 0, 0, 0));
        
        okButton = new JButton();
        okButton.setAction(new CloseAction());
        
        doNotShowCheckBox = new JCheckBox();
        doNotShowCheckBox.setFont(doNotShowFont);
        doNotShowCheckBox.setForeground(doNotShowForeground);
        doNotShowCheckBox.setText(I18n.tr("Do not show message again"));
        
        vendorLabel = new JLabel(vendorIcon);
    }
    
    /**
     * Displays the AV vendor message.
     */
    public void showVendorMessage() {
        setLayout(new MigLayout("insets 25 15 15 15, gap 0 0"));
        
        headingLabel.setFont(headingFont);
        headingLabel.setForeground(headingForeground);
        headingLabel.setText(I18n.tr("Anti-Virus Protection"));
        
        // Set message text.  We also set the preferred size to control the
        // message and dialog width.
        String message = I18n.tr("LimeWire PRO Anti-Virus protection is powered by AVG.  AVG scans, detects and deletes files that are suspected to contain viruses or spyware.  <a href=\"{0}\">Learn more</a>", AVG_URL);
        messageLabel.setText(message);
        messageLabel.setPreferredSize(new Dimension(300, messageLabel.getPreferredSize().height));
        
        // Hide checkbox option.
        doNotShowCheckBox.setVisible(false);
        
        // Add components to container.
        add(headingLabel, "span, align left, wrap");
        add(messageLabel, "span, align left, wrap 15");
        add(okButton, "alignx left, aligny bottom");
        add(vendorLabel, "alignx right, aligny bottom, push");
        
        // Display as modal dialog.
        showDialog(I18n.tr("About AVG Anti-Virus"));
    }
    
    /**
     * Displays the dangerous file message.  This applies to files marked as 
     * hazardous by the internal dangerous file checker.
     * 
     * @param item the item representing the downloaded file
     * @param autoNotify true if this is an automatic message request due to a state change
     */
    public void showDangerMessage(DownloadItem item, boolean autoNotify) {
        String heading = I18n.tr("Dangerous File");
        String message = I18n.tr("{0} is considered a dangerous file and has automatically been deleted for your protection.", item.getFileName());
        showWarningMessage(heading, message, autoNotify, false);
    }
    
    /**
     * Displays the threat detected message.  This applies to infected files
     * as determined by the virus scanner.
     * 
     * @param item the item representing the downloaded file
     * @param autoNotify true if this is an automatic message request due to a state change
     */
    public void showThreatMessage(DownloadItem item, boolean autoNotify) {
        String heading = I18n.tr("Threat Detected");
        String message = I18n.tr("{0} is suspected to contain a virus or spyware and has automatically been deleted for your protection.  LimeWire PRO Anti-Virus protection is powered by AVG.", item.getFileName());
        showWarningMessage(heading, message, autoNotify, true);
    }
    
    /**
     * Displays the failure message.  This applies to files that could not be 
     * scanned due to a problem with the virus scanner.
     * 
     * @param item the item representing the downloaded file
     * @param autoNotify true if this is an automatic message request due to a state change
     */
    public void showFailureMessage(DownloadItem item, boolean autoNotify) {
        String heading = I18n.tr("Scan Failed");
        String message = I18n.tr("{0} could not be inspected due to a problem with the virus scanner.  LimeWire PRO Anti-Virus protection is powered by AVG.", item.getFileName());
        showWarningMessage(heading, message, autoNotify, true);
    }
    
    /**
     * Displays a warning message with the specified heading and message text.
     */
    private void showWarningMessage(String heading, String message, 
            boolean autoNotify, boolean showVendor) {
        // Skip if message is automatic and setting is false.
        if (autoNotify && !SwingUiSettings.WARN_DOWNLOAD_THREAT_FOUND.getValue()) {
            return;
        }
        
        setLayout(new MigLayout("insets 25 15 15 15, gap 0 0, hidemode 3"));
        
        JLabel iconLabel = new JLabel(threatIcon);
        
        headingLabel.setFont(threatHeadingFont);
        headingLabel.setForeground(threatHeadingForeground);
        headingLabel.setText(heading);
        
        // Set message text.  We also set the preferred size to control the
        // message and dialog width.
        messageLabel.setText(message);
        messageLabel.setPreferredSize(new Dimension(330, messageLabel.getPreferredSize().height));
        
        // Set up checkbox option.
        doNotShowCheckBox.setSelected(true);
        doNotShowCheckBox.setVisible(autoNotify);
        
        // Add components to container.
        add(iconLabel, "spany, alignx left, aligny top, gaptop 6, gapright 15"); 
        add(headingLabel, "span, align left, wrap");
        add(messageLabel, "span, align left, wrap 15");
        add(okButton, "alignx left, aligny bottom");
        add(doNotShowCheckBox, "alignx left, aligny bottom, gapleft 5");
        if (showVendor) add(vendorLabel, "alignx right, aligny bottom, push");
        
        // Display as modal dialog.
        showDialog(I18n.tr("Warning"));
    }
    
    /**
     * Displays this panel in a modal dialog.
     */
    private void showDialog(String title) {
        // Create dialog.
        final JFrame owner = GuiUtils.getMainFrame();
        final JDialog dialog = new LimeJDialog(owner);
        
        // Set dialog properties.
        dialog.setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setTitle(title);
        
        // Add content to dialog.
        dialog.add(this, BorderLayout.CENTER);
        
        // Add listener to resize and position dialog after opening.  This is 
        // necessary because we don't know the actual height of the message
        // component (JEditorPane) until after the window is displayed.  This
        // will properly size the dialog for any message length.
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                dialog.pack();
                dialog.setLocationRelativeTo(owner);
            }
        });
        
        // Position and display dialog.
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
    
    /**
     * Closes the window containing this panel.
     */
    private void disposeWindow() {
        // Apply "do not show" option if visible.
        if (doNotShowCheckBox.isVisible()) {
            SwingUiSettings.WARN_DOWNLOAD_THREAT_FOUND.setValue(!doNotShowCheckBox.isSelected());
        }
        
        // Dispose of parent window.
        Container ancestor = getTopLevelAncestor();
        if (ancestor instanceof Window) {
            ((Window) ancestor).dispose();
        }
    }
    
    /**
     * Action to close window.
     */
    private class CloseAction extends AbstractAction {

        public CloseAction() {
            super(I18n.tr("OK"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            disposeWindow();
        }
    }
}
