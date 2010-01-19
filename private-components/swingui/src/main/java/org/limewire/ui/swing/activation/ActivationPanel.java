package org.limewire.ui.swing.activation;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationState;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.activation.ActivationWarningPanel.Mode;
import org.limewire.ui.swing.components.ColoredBusyLabel;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

public class ActivationPanel {

    @Resource
    private int width;
    @Resource
    private int height;
    @Resource
    private Font font;
    @Resource
    private Color fontColor;
    @Resource
    private Color errorColor;
    @Resource
    private Icon unsupportedIcon;
    
    private final static String NO_LICENSE_PANEL = "NO_LICENSE_PANEL";
    private final static String EDIT_PANEL = "EDIT_PANEL";
    private final static String OK_PANEL = "OK_PANEL";
    
    private final ActivationManager activationManager;
    private ActivationListener listener;

    private JPanel activationPanel;
    private JDialog dialog;
    private LicenseKeyTextField licenseField;
    private LicenseKeyPanel licenseKeyPanel;
    private ActivationWarningPanel warningPanel;
    private JButton editButton;
    private JXLayer tableJXLayer;
    private ActivationTable table;
    ColoredBusyLabel tableOverlayBusyLabel;
    private JLabel licenseKeyErrorLabel;
    private JLabel licenseTableErrorLabel;
    private UnderneathModuleTableMessagePanel underneathModuleTableMessagePanel;
    private JPanel cardPanel;
    private CardLayout cardLayout;
    
    private EventList<ActivationItem> eventList;
    
    Map<String, ButtonPanel> cardMap = new HashMap<String, ButtonPanel>();
    
    private final StateManager stateManager;

    @Inject
    public ActivationPanel(ActivationManager activationManager) {
        this.activationManager = activationManager;
        listener = new ActivationListener();
        
        eventList = new BasicEventList<ActivationItem>();
        
        stateManager = new StateManager();
        
        initComponents();
    }
    
    private void initComponents() {
        GuiUtils.assignResources(this);   
        
        activationPanel = new JPanel(new MigLayout("gap 0, fillx, insets 10 20 10 20"));
        
        activationPanel.setPreferredSize(new Dimension(width, height));
        activationPanel.setBackground(GuiUtils.getMainFrame().getBackground());
        
        JLabel licenseKeyLabel = new JLabel(I18n.tr("License Key" + ":"));
        licenseKeyLabel.setFont(font);
        licenseKeyLabel.setForeground(fontColor);
        
        licenseField = new LicenseKeyTextField(25);
        licenseField.setFont(font);
        licenseField.setForeground(fontColor);
        licenseField.addActionListener(new ActivateAction("", ""));
        TextFieldClipboardControl.install(licenseField);

        licenseKeyPanel = new LicenseKeyPanel(licenseField);
        
        warningPanel = new ActivationWarningPanel();
        
        editButton = new JButton(new EditAction());
        
        licenseKeyErrorLabel = new JLabel("This text is to allow miglayout to position this component.");
        licenseKeyErrorLabel.setFont(font);
        licenseKeyErrorLabel.setForeground(errorColor);
        
        licenseTableErrorLabel = new JLabel("This text is to allow miglayout to position this component.");
        licenseTableErrorLabel.setFont(font);
        licenseTableErrorLabel.setForeground(errorColor);
        
        underneathModuleTableMessagePanel = new UnderneathModuleTableMessagePanel();
        underneathModuleTableMessagePanel.setVisible(false);
        
        table = new ActivationTable(eventList);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        tableJXLayer = new JXLayer<JComponent>(scrollPane);
        scrollPane.setMinimumSize(new Dimension(350, 4 * 25));
        scrollPane.setPreferredSize(new Dimension(350, 4 * 25));
        tableOverlayBusyLabel = new ColoredBusyLabel(new Dimension(20,20));
        JPanel busyLabelPanel = new JPanel(new MigLayout("align 50% 50%"));
        busyLabelPanel.add(Box.createVerticalStrut(10), "wrap");
        busyLabelPanel.add(tableOverlayBusyLabel);
        busyLabelPanel.setOpaque(false);
        tableJXLayer.getGlassPane().setLayout(new BorderLayout());
        tableJXLayer.getGlassPane().add(busyLabelPanel, BorderLayout.CENTER);
        tableJXLayer.getGlassPane().setVisible(false);

        activationPanel.add(licenseKeyErrorLabel, "skip 1, span, wrap");
        
        activationPanel.add(licenseKeyLabel, "gapright 10, growy 0");
        activationPanel.add(licenseKeyPanel, "pushy, growy 0");
        activationPanel.add(warningPanel.getComponent(), "gapleft 6, aligny 50%, growy 0");
        activationPanel.add(editButton, "gapleft 40, align 100% 50%, growy 0, wrap");
        
        activationPanel.add(licenseTableErrorLabel, "span, growx, gaptop 6, gapbottom 6, hidemode 0");
        
        activationPanel.add(tableJXLayer, "span, grow, gapbottom 10, gpy 200, wrap");
        
        activationPanel.add(underneathModuleTableMessagePanel, "hidemode 0, span, grow, wrap");
        
        activationPanel.add(Box.createVerticalStrut(10), "hidemode 0, span, growy, wrap");

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        
        activationPanel.add(cardPanel, "span, growx, wrap");
    }
    
    @Inject
    public void register() {
        activationManager.addListener(listener);
    }

    public void show() {
        // Setting the ActivationState and ErrorState initialize all of the
        // fields and views before being shown.
        licenseField.setText(activationManager.getLicenseKey());

        stateManager.setActivationState(activationManager.getActivationState(), activationManager.getActivationError());

        dialog = new LimeJDialog();
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setTitle(I18n.tr("LimeWire"));
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.add(activationPanel);
        dialog.pack();
        
        dialog.addComponentListener(new ComponentListener(){
            @Override
            public void componentHidden(ComponentEvent e) {
                dispose();
            }

            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentResized(ComponentEvent e) {}
            @Override
            public void componentShown(ComponentEvent e) {
//                if(okButton != null)
//                    okButton.requestFocusInWindow();
            }
        }); 
        
        dialog.setLocationRelativeTo(GuiUtils.getMainFrame());
        dialog.setVisible(true);
    }
    
    private void dispose() {
        if(listener != null) {
            activationManager.removeListener(listener);
        }
        if(dialog != null) {
            dialog.dispose();
            dialog = null;
        }
    }

    private void selectCard(String card) {
        if(!cardMap.containsKey(card)) {
            if(card.equals(NO_LICENSE_PANEL)) {
                ButtonPanel panel = new NoLicenseButtonPanel();
                cardMap.put(card, panel);
                cardPanel.add(panel, card);
            } else if(card.equals(OK_PANEL)) {
                ButtonPanel panel = new ActivatedButtonPanel();
                cardMap.put(card, panel);
                cardPanel.add(panel, card);
            } else if(card.equals(EDIT_PANEL)) {
                ButtonPanel panel = new EditButtonPanel();
                cardMap.put(card, panel);
                cardPanel.add(panel, card);
            }
        }

        cardLayout.show(cardPanel, card);
    }

    private class StateManager {
        // unfortunately if the user presses the refresh button and there is an error due to a server communication problem
        // we have to handle the error differently then we would if there was an error with a first time activation.
        // so, we track whether the refresh button was pressed with this flag.
        private boolean refreshing = false;
        private boolean editingLicense = true;
        private ActivationState state = ActivationState.NOT_AUTHORIZED;
        private ActivationError error = ActivationError.NO_ERROR;

        public void setRefreshing(boolean refreshing) {
            this.refreshing = refreshing;
        }

        public void setEditingLicense(boolean editingLicense) {
            editButton.setVisible(false);
            licenseKeyPanel.setEditable(true);
            licenseField.requestFocusInWindow();
            licenseField.selectAll();
            warningPanel.setActivationMode(Mode.EMPTY);
            selectCard(EDIT_PANEL);

            this.editingLicense = editingLicense;
        }

        public void setActivationState(ActivationState state, ActivationError error) {
            this.state = state;
           this.error = error;

           if (state == ActivationState.AUTHORIZED) {
               editingLicense = false;
               refreshing = false;
           }

           // let's clear the event list used to populate the module table and repopulate it if we're activated
           if (!refreshing) {
               eventList.clear();
           }

           if (state == ActivationState.AUTHORIZED) {
               eventList.addAll(activationManager.getActivationItems());
           } else if ((state == ActivationState.NOT_AUTHORIZED && !refreshing)) {
               eventList.add(new LostLicenseItem());
           }

           update();
        }
        
        public void update() {
            // here we go through row by row of the activation dialog controlling the visibility and state of individual gui components.

            // row 1: error message for license key

            // here we control the visibility and the text of the error message above the license key field.
            boolean isLicenseKeyErrorLabelVisible = false;
            if (error == ActivationError.INVALID_KEY) {
                licenseKeyErrorLabel.setText(I18n.tr("Sorry, the key you entered is invalid. Please try again."));
                isLicenseKeyErrorLabelVisible = true;
            } else if (error == ActivationError.BLOCKED_KEY) {
                licenseKeyErrorLabel.setText(I18n.tr("Sorry, the key you entered is blocked. It's already in use."));
                isLicenseKeyErrorLabelVisible = true;
            }
            licenseKeyErrorLabel.setVisible(isLicenseKeyErrorLabelVisible);
            
            // row 2: license field, icon panel, edit button
            
            // normally, we would make the license field editable if the state was uninitialized, not activated, or activating but not refreshing
            // and it would not be editable if they were already activated or if they were activating b/c they hit the refresh button.
            // but if the user hits the edit button, then this field becomes editable regardless of the activation state
            
            boolean isEditMode = editingLicense || state == ActivationState.NOT_AUTHORIZED
                                  || (state == ActivationState.AUTHORIZING && !refreshing);
            
            // let's set whether the license should be shown as a text field or as a none editable label.
            licenseKeyPanel.setEditable( isEditMode );

            // let's set whether there is an icon next to the license field and whether it shows an error icon or a spinner
            Mode warningPanelMode = Mode.EMPTY;
            if (state == ActivationState.AUTHORIZING) {
                if (!refreshing) {
                    warningPanelMode = Mode.SPINNER;
                }
            } else if (state == ActivationState.NOT_AUTHORIZED) {
                if (error == ActivationError.INVALID_KEY || error == ActivationError.BLOCKED_KEY) {
                    warningPanelMode = Mode.WARNING;
                }
            }
            warningPanel.setActivationMode(warningPanelMode);

            // let's set the visibility of the edit button next to the license field
            editButton.setVisible( !isEditMode );

            // row 3: error message for module table

            // here we control the visibility and the text of the error message above the module table
            boolean isLicenseTableErrorLabelVisible = false;
            if (error == ActivationError.EXPIRED_KEY) {
                licenseTableErrorLabel.setText(I18n.tr("Your license has expired. Click Renew to renew your license."));
                isLicenseTableErrorLabelVisible = true;
            } else if (refreshing && error == ActivationError.COMMUNICATION_ERROR) {
                licenseTableErrorLabel.setText(I18n.tr("There was an error refreshing. Please try again."));
                isLicenseTableErrorLabelVisible = true;
            }
            licenseTableErrorLabel.setVisible(isLicenseTableErrorLabelVisible);

            // row 4: the module table

            // here we set whether the busy spinner should show over the module table b/c we're refreshing the module list.
            if (state == ActivationState.AUTHORIZING && refreshing) {
                tableOverlayBusyLabel.setBusy(true);
                tableJXLayer.getGlassPane().setVisible(true);
            } else {
                tableOverlayBusyLabel.setBusy(false);
                tableJXLayer.getGlassPane().setVisible(false);
            }

            // row 5: the info message below the module table
            
            // here we control the visibility and the text of the error message that appears below the module table
            boolean isUnderneathModuleTableMessagePanelVisible = false;
// TODO implement the blocked modules method
/*            if (areThereBlockedModules()) {
                underneathModuleTableMessagePanel.showBlockedModulesMessage();
                isUnderneathModuleTableMessagePanelVisible = true;
            } else
*/ 
            if (areThereNonFunctionalModules()) {
                underneathModuleTableMessagePanel.showUnsupportedModulesMessage();
                isUnderneathModuleTableMessagePanelVisible = true;
            }
            underneathModuleTableMessagePanel.setVisible(isUnderneathModuleTableMessagePanelVisible);
            
            // row 6: the button panel
            
            // let's set which panel of buttons is showing underneath the module table
            String cardName = NO_LICENSE_PANEL;
            if (state == ActivationState.AUTHORIZED) {
                if (isEditMode) {
                    cardName = EDIT_PANEL;
                } else {
                    cardName = OK_PANEL;
                }
            } else if (refreshing) {
                cardName = OK_PANEL;
            }
            
            selectCard(cardName);

            activationPanel.repaint();
        }
        
        private boolean areThereExpiredModules() {
            for (ActivationItem item : activationManager.getActivationItems()) {
                if (item.getStatus() == ActivationItem.Status.EXPIRED) {
                    return true;
                }
            }
            return false;
        }

        private boolean areThereNonFunctionalModules() {
            for (ActivationItem item : activationManager.getActivationItems()) {
                if (item.getStatus() == ActivationItem.Status.UNAVAILABLE || item.getStatus() == ActivationItem.Status.UNUSEABLE_LW || item.getStatus() == ActivationItem.Status.UNUSEABLE_OS) {
                    return true;
                }
            }
            return false;
        }
    }
    
    private abstract class ButtonPanel extends JPanel {
        
        public ButtonPanel() {
            super(new MigLayout("fillx, gap 0, insets 0"));
            setOpaque(false);
        }
        
        public abstract void setActivationEnabled(boolean enabled);
    }
    
    private class NoLicenseButtonPanel extends ButtonPanel {
        private JButton activateButton;
        
        public NoLicenseButtonPanel() {
            JButton goProButton = new JButton(new UrlAction(I18n.tr("Go Pro"),"http://www.limewire.com/client_redirect/?page=gopro"));
            activateButton = new JButton(new ActivateAction(I18n.tr("Activate"), I18n.tr("Activate the License Key")));
            JButton laterButton = new JButton(new OKDialogAction(I18n.tr("Later"), I18n.tr("Activate License at a later time")));
            
            add(goProButton, "push");
            add(activateButton, "split, tag ok");
            add(laterButton, "tag cancel, wrap");
        }
        
        @Override
        public void setActivationEnabled(boolean enabled) {
            activateButton.setEnabled(enabled);
        }
    }

    private class EditButtonPanel extends ButtonPanel {
        private JButton updateButton;
        
        public EditButtonPanel() {
            updateButton = new JButton(new ActivateAction(I18n.tr("Update"), I18n.tr("Update the saved key")));
            JButton cancelButton = new JButton(new CancelAction());
            
            add(updateButton, "alignx 100%, gapright 10, tag ok, split");
            add(cancelButton, "wrap, tag cancel");
        }
        
        @Override
        public void setActivationEnabled(boolean enabled){
            updateButton.setEnabled(enabled);
        }
    }

    private class ActivatedButtonPanel extends ButtonPanel {

        public ActivatedButtonPanel() {
            JButton refreshButton = new JButton(new RefreshAction(I18n.tr("Refresh"), I18n.tr("Refresh the list of modules associated with the key")));
            JButton okButton = new JButton(new OKDialogAction());
            
            add(refreshButton, "push");
            add(okButton, "split, alignx 100%, tag ok, wrap");
        }
        
        @Override
        public void setActivationEnabled(boolean enabled){
            //NO STATE CHANGE
        }
    }
    
    private class UnderneathModuleTableMessagePanel extends JPanel {
        JEditorPane textLabel;
        
        public UnderneathModuleTableMessagePanel() {
            showUnsupportedModulesMessage();
        }

        public void showUnsupportedModulesMessage() {
            removeAll();
            setLayout(new MigLayout("insets 0 0 10 0, gap 0", "[]", "[]"));
            setOpaque(false);
            add(new JLabel(unsupportedIcon), "align 0% 0%, split");
            textLabel = new JEditorPane("text/html", "<html>" + I18n.tr("One or more of your licenses is currently not supported. For more help please contact ") + "<a href='http://www.limewire.com/support'>" + I18n.tr("Customer Support") + "</a></html>");
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
            add(textLabel,"align 0% 0%");
        }

        public void showBlockedModulesMessage() {
            removeAll();
            setLayout(new MigLayout("insets 0 0 10 0, gap 0", "[]", "[]"));
            setOpaque(false);
            add(new JLabel(unsupportedIcon), "align 0% 0%, split");
            textLabel = new JEditorPane("text/html", "<html>" + I18n.tr("Oh no! It appears that your license key has been subject to abuse. Please contact ") + "<a href='http://www.limewire.com/support'>" + I18n.tr("Customer Support") + "</a>" 
                                        + I18n.tr(" to resolve the situation.") + "</html>");
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
            add(textLabel,"align 0% 0%");
        }
    }
    
    private class EditAction extends AbstractAction {

        public EditAction() {
            putValue(Action.NAME, I18n.tr("Edit Key"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Edit the saved License Key"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            stateManager.setEditingLicense(true);
        }
    }
    
    private class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(Action.NAME, I18n.tr("Cancel"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Cancel editing"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            stateManager.setActivationState(ActivationState.AUTHORIZED, ActivationError.NO_ERROR);
        }
    }
    
    private class ActivateAction extends AbstractAction {
        public ActivateAction(String name, String description) {
            putValue(Action.NAME, name);
            putValue(Action.SHORT_DESCRIPTION, description);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            activationManager.activateKey(licenseField.getText().trim().replaceAll("-", ""));
        }
    }
    
    private class RefreshAction extends AbstractAction {
        public RefreshAction(String name, String description) {
            putValue(Action.NAME, name);
            putValue(Action.SHORT_DESCRIPTION, description);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            stateManager.setRefreshing(true);
            activationManager.activateKey(licenseField.getText().trim().replaceAll("-", ""));
        }
    }

    private class ActivationListener implements EventListener<ActivationEvent> {
        @Override
        public void handleEvent(final ActivationEvent event) {
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    stateManager.setActivationState(event.getData(), event.getError());
                }
            });
        }
    }
}
