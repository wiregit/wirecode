package org.limewire.activation.impl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.activation.api.ActSettings;
import org.limewire.activation.api.ActivationManager;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

public class BlockedNotificationDialog extends LimeJDialog {

    @Resource
    private int dialogWidth;
    @Resource
    private int dialogHeight;
    @Resource
    private Color headingColor;

    private final ActivationManager activationManager;
    private final ActSettings activationSettings;
    
    public BlockedNotificationDialog(ActivationManager activationManager, ActSettings activationSettings) {
        super(GuiUtils.getMainFrame());

        GuiUtils.assignResources(this);   

        this.activationManager = activationManager;
        this.activationSettings = activationSettings;
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            setModal(true);
            setResizable(false);
            setTitle(I18n.tr("LimeWire License"));
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            add(createContentPanel());
            setSize(dialogWidth, dialogHeight);
            pack();
            
            addComponentListener(new ComponentListener(){
                @Override
                public void componentHidden(ComponentEvent e) {
                    dispose();
                }
                @Override
                public void componentMoved(ComponentEvent e) {}
                @Override
                public void componentResized(ComponentEvent e) {}
                @Override
                public void componentShown(ComponentEvent e) {}
            }); 

            setLocationRelativeTo(GuiUtils.getMainFrame());
        }
        super.setVisible(visible);
    }

    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new MigLayout("fill, gap 0, insets 10 10 10 10", "[]", "[][][][]"));

        if (GuiUtils.getMainFrame() != null) {
            contentPanel.setBackground(GuiUtils.getMainFrame().getBackground());
        }

        contentPanel.setPreferredSize(new Dimension(dialogWidth, dialogHeight));
        
        JLabel headingLabel = new JLabel(I18n.tr("Sorry, your license key has been blocked."));
        headingLabel.setForeground(headingColor);
        headingLabel.setFont(headingLabel.getFont().deriveFont(Font.BOLD, headingLabel.getFont().getSize()+1));
        contentPanel.add(headingLabel, "alignx 0%, gapleft 20, gapright 20, gaptop 15, gapbottom 20, wrap");
        
        String messageA = I18n.tr("Your license key has been used on too many installations. ");
        String messageB = I18n.tr("Please contact ");
        String messageC = I18n.tr("customer support");
        String messageD = I18n.tr(" to resolve the situation or go to File > License to enter a new key.");
        
        JEditorPane textLabel = new JEditorPane();
        textLabel.setContentType("text/html");
        textLabel.setEditable(false);
        textLabel.setOpaque(false);
        textLabel.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    NativeLaunchUtils.openURL(e.getURL().toExternalForm());
                }
            }
        });
        textLabel.setText("<html>" + messageA + messageB
                          + "<a href='" + activationSettings.getCustomerSupportHost() + "'>" + messageC + "</a>" 
                          + messageD + "</html>");
        contentPanel.add(textLabel, "alignx 0%, gapleft 20, gapright 20, gapbottom 15, wrap");

        contentPanel.add(Box.createVerticalGlue(), "growy, growprioy 200, wrap");

        final JCheckBox dontShowAgainCheckbox = new JCheckBox(I18n.tr("Don't show me this again"));
        dontShowAgainCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                activationSettings.setActivationKey("");
                activationSettings.setMCode("");
                // we call the activation manager and ask it to validate an obviously bad key to erase the blocked error message
                activationManager.activateKey("");
            }
        });
        contentPanel.add(dontShowAgainCheckbox, "align 0% 50%, split");

        contentPanel.add(Box.createHorizontalGlue(), "align 0% 50%, growx, split");

        JButton closeButton = new JButton(I18n.tr("Close"));
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BlockedNotificationDialog.this.setVisible(false);
                BlockedNotificationDialog.this.dispose();
            }
            
        });
        contentPanel.add(closeButton, "align 100% 50%, wrap");

        return contentPanel;
    }
    

}
