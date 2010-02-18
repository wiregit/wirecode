package org.limewire.ui.swing.activation;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.JTableHeader;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationSettingsController;
import org.limewire.activation.api.ActivationState;
import org.limewire.core.api.Application;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.activation.ActivationWarningPanel.Mode;
import org.limewire.ui.swing.components.ColoredBusyLabel;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.mainframe.AppFrame;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.table.CalendarRenderer;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.NotImplementedException;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

/**
 * Creates the ActivationPanel which gets displayed in a Modal Dialog
 * by going to File->License. 
 * 
 * This allows the user to enter/see their License Key and any Features
 * that may be associated with that License Key in a table. Options
 * change within the Panel depending on the state of the License Key.
 */
public class ActivationPanel {

    @Resource
    private int tableWidth;
    @Resource
    private Font font;
    @Resource
    private Color fontColor;
    @Resource
    private Color errorColor;
    @Resource
    private Icon unsupportedIcon;
    
    private final static String NO_LICENSE_BUTTON_PANEL = "NO_LICENSE_PANEL";
    private final static String EDIT_LICENSE_BUTTON_PANEL = "EDIT_PANEL";
    private final static String OK_LICENSE_BUTTON_PANEL = "OK_PANEL";

    private final ActivationManager activationManager;
    private final Application application;
    private final ActivationListener listener;
    private final EventList<ActivationItem> eventList;
    private final StateManager stateManager;
    private final Map<String, ButtonPanel> cardMap = new HashMap<String, ButtonPanel>();    
    
    private JPanel activationPanel;
    private JDialog dialog;
    private LicenseKeyTextField licenseField;
    private LicenseKeyPanel licenseKeyPanel;
    private ActivationWarningPanel warningPanel;
    private JButton editButton;
    private JXLayer tableJXLayer;
    private ActivationTable table;
    private JScrollPane scrollPane;
    private ColoredBusyLabel tableOverlayBusyLabel;
    private JLabel licenseErrorLabel;
    private UnderneathActivationTableMessagePanel underneathModuleTableMessagePanel;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    @Inject
    public ActivationPanel(ActivationManager activationManager, CalendarRenderer calendarRenderer,
                           Application application) {
        this.activationManager = activationManager;
        this.application = application;
        listener = new ActivationListener();
        eventList = new BasicEventList<ActivationItem>();
        stateManager = new StateManager();
        
        initComponents(calendarRenderer, application);
    }
    
    private void initComponents(CalendarRenderer calendarRenderer, Application application) {
        GuiUtils.assignResources(this);   
        
        activationPanel = new JPanel(new MigLayout("gap 0, fillx, insets 20 20 20 20"));

        activationPanel.setBackground(GuiUtils.getMainFrame().getBackground());
        
        JLabel licenseKeyLabel = new JLabel(I18n.tr("License Key:"));
        licenseKeyLabel.setFont(font);
        licenseKeyLabel.setForeground(fontColor);
        
        licenseField = new LicenseKeyTextField(25);
        licenseField.setFont(font);
        licenseField.setForeground(fontColor);
        licenseField.addActionListener(new ActivateAction());
        TextFieldClipboardControl.install(licenseField);

        licenseKeyPanel = new LicenseKeyPanel(licenseField);
        
        warningPanel = new ActivationWarningPanel();
        
        editButton = new JButton(new EditAction());
        
        licenseErrorLabel = new JLabel();
        licenseErrorLabel.setFont(font);
        licenseErrorLabel.setForeground(errorColor);
        
        underneathModuleTableMessagePanel = new UnderneathActivationTableMessagePanel();
        
        table = new ActivationTable(eventList, calendarRenderer, application);

        scrollPane = new JScrollPane(table);
        int height = 4 * table.getRowHeight() + table.getTableHeader().getPreferredSize().height + 2;
        
        tableOverlayBusyLabel = new ColoredBusyLabel(new Dimension(20,20));
        JPanel busyLabelPanel = new JPanel(new MigLayout("align 50% 50%"));
        busyLabelPanel.add(Box.createVerticalStrut(10), "wrap");
        busyLabelPanel.add(tableOverlayBusyLabel);
        busyLabelPanel.setOpaque(false);
        
        tableJXLayer = new JXLayer<JComponent>(scrollPane);
        tableJXLayer.getGlassPane().setLayout(new BorderLayout());
        tableJXLayer.getGlassPane().add(busyLabelPanel, BorderLayout.CENTER);
        tableJXLayer.getGlassPane().setVisible(false);
        
        activationPanel.add(licenseKeyLabel, "gapright 10, growy 0");
        activationPanel.add(licenseKeyPanel.getComponent(), "growx, width 230!, growy 0");
        activationPanel.add(warningPanel.getComponent(), "gapleft 6, aligny 50%, growy 0");
        activationPanel.add(editButton, "gapleft 16, align 100% 50%, growy 0, wrap");
        
        activationPanel.add(licenseErrorLabel, "span, growx, aligny 50%, height 27!, wrap");
        
        activationPanel.add(tableJXLayer, "span, grow, gapbottom 10, height " + height + "!, width " + tableWidth + ", gpy 200, wrap");

        activationPanel.add(underneathModuleTableMessagePanel, "hidemode 3, width " + tableWidth + "!, span, growx, wrap");

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        
        activationPanel.add(cardPanel, "span, gaptop 10, growx, wrap");
    }
        
    /**
     * Fills in the top right corner if a scrollbar appears with an empty table
     * header.
     */
    protected void configureEnclosingScrollPane(JScrollPane scrollPane) {
        JTableHeader th = new JTableHeader();
        th.setDefaultRenderer(new TableCellHeaderRenderer());
        // Put a dummy header in the upper-right corner.
        final Component renderer = th.getDefaultRenderer().getTableCellRendererComponent(null, "", false, false, -1, -1);
        JPanel cornerComponent = new JPanel(new BorderLayout());
        cornerComponent.add(renderer, BorderLayout.CENTER);
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerComponent);
    }    
    
    @Inject
    public void register() {
        activationManager.addListener(listener);
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                configureEnclosingScrollPane(scrollPane);                
            }
        });
    }

    public void show() {
        // Setting the ActivationState and ErrorState initialize all of the
        // fields and views before being shown.
        licenseField.setText(activationManager.getLicenseKey());

        // when we are initially opening the dialog don't show any error messages
        // pertaining to fleeting states like communication errors or invalid keys
        ActivationError error = ActivationError.NO_ERROR;
        if(activationManager.getActivationError() == ActivationError.BLOCKED_KEY) {
            error = activationManager.getActivationError();
        }
        stateManager.setActivationState(activationManager.getActivationState(), error);
        
        dialog = new LimeJDialog(GuiUtils.getMainFrame());
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setTitle(AppFrame.getInstance().getContext().getResourceMap().getString("Application.title"));
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
                dialog.pack();
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
            if(card.equals(NO_LICENSE_BUTTON_PANEL)) {
                NoLicenseButtonPanel noLicenseButtonPanel = new NoLicenseButtonPanel();
                cardMap.put(card, noLicenseButtonPanel);
                cardPanel.add(noLicenseButtonPanel, card);
            } else if(card.equals(OK_LICENSE_BUTTON_PANEL)) {
                ActivatedButtonPanel activatedButtonPanel = new ActivatedButtonPanel();
                cardMap.put(card, activatedButtonPanel);
                cardPanel.add(activatedButtonPanel, card);
            } else if(card.equals(EDIT_LICENSE_BUTTON_PANEL)) {
                EditButtonPanel editButtonPanel = new EditButtonPanel();
                cardMap.put(card, editButtonPanel);
                cardPanel.add(editButtonPanel, card);
            }
        }
        cardLayout.show(cardPanel, card);
    }

    /*
     * This class receives updates about the applications activation state and updates the user interface accordingly,
     * showing and hiding various components depending on what the activation state and error state is.
     */
    private class StateManager {
        private boolean editingLicense = true;
        private ActivationState state = ActivationState.NOT_AUTHORIZED;
        private ActivationError error = ActivationError.NO_ERROR;

        public void setEditingLicense(boolean editingLicense) {
            editButton.setVisible(false);
            licenseKeyPanel.setEditable(true);
            licenseField.requestFocusInWindow();
            licenseField.selectAll();
            warningPanel.setActivationMode(Mode.EMPTY);
            selectCard(EDIT_LICENSE_BUTTON_PANEL);

            this.editingLicense = editingLicense;
        }

        public void setActivationState(ActivationState state, ActivationError error) {
            this.state = state;
            this.error = error;

            if (state == ActivationState.AUTHORIZED) {
                editingLicense = false;
                licenseKeyPanel.setKey(activationManager.getLicenseKey());
            }

            // let's clear the event list used to populate the module table and repopulate it if we're activated
            eventList.clear();

            if (state == ActivationState.AUTHORIZED || state == ActivationState.REFRESHING) {
                eventList.addAll(activationManager.getActivationItems());
            } else if ((state == ActivationState.NOT_AUTHORIZED)) {
                if (error == ActivationError.BLOCKED_KEY) {
                    // do nothing
                } else {
                    eventList.add(new LostLicenseItem(application));
                }
            }

           update();
           
           if(dialog != null)
               dialog.pack();
        }
        
        public void update() {
            // here we go through row by row of the activation dialog controlling the visibility and state of individual gui components.
            
            // row 2: license field, icon panel, edit button
            
            // normally, we would make the license field editable if the state was uninitialized, not activated, or activating but not refreshing
            // and it would not be editable if they were already activated or if they were activating b/c they hit the refresh button.
            // but if the user hits the edit button, then this field becomes editable regardless of the activation state
            
            boolean isEditMode = editingLicense || state == ActivationState.NOT_AUTHORIZED
                                  || (state == ActivationState.AUTHORIZING);
            
            // let's set whether the license should be shown as a text field or as a none editable label.
            licenseKeyPanel.setEditable( isEditMode );

            // let's set whether there is an icon next to the license field and whether it shows an error icon or a spinner
            Mode warningPanelMode = Mode.EMPTY;
            if (state == ActivationState.AUTHORIZING) {
                warningPanelMode = Mode.SPINNER;
            } else if (state == ActivationState.NOT_AUTHORIZED) {
                if (error == ActivationError.INVALID_KEY || error == ActivationError.BLOCKED_KEY) {
                    warningPanelMode = Mode.WARNING;
                }
            }
            warningPanel.setActivationMode(warningPanelMode);

            // let's set the visibility of the edit button next to the license field
            editButton.setVisible( !isEditMode );

            // row 3: error message for module table
            licenseErrorLabel.setText(getLicenseErrorMessage(error));

            // row 4: the module table
            // here we set whether the busy spinner should show over the module table b/c we're refreshing the module list.
            if (state == ActivationState.REFRESHING) {
                editButton.setEnabled(false);
                tableOverlayBusyLabel.setBusy(true);
                tableJXLayer.getGlassPane().setVisible(true);
            } else {
                editButton.setEnabled(true);
                tableOverlayBusyLabel.setBusy(false);
                tableJXLayer.getGlassPane().setVisible(false);
            }

            // row 5: the info message below the module table
            if(error == ActivationError.BLOCKED_KEY) {
                underneathModuleTableMessagePanel.setState(MessageState.BLOCKED);
            } else if(areThereExpiredModules()) {
                underneathModuleTableMessagePanel.setState(MessageState.EXPIRED);
            } else if(areThereNonFunctionalModules()) {
                underneathModuleTableMessagePanel.setState(MessageState.UNSUPPORTED);
            } else {
                underneathModuleTableMessagePanel.setState(MessageState.NO_ERROR);
            }

            // row 6: the button panel
            // let's set which panel of buttons is showing underneath the module table
            String cardName = NO_LICENSE_BUTTON_PANEL;
            if (state == ActivationState.AUTHORIZED) {
                if (isEditMode) {
                    cardName = EDIT_LICENSE_BUTTON_PANEL;
                } else {
                    cardName = OK_LICENSE_BUTTON_PANEL;
                }
            } else if (state == ActivationState.REFRESHING) {
                cardName = OK_LICENSE_BUTTON_PANEL;
            }
            
            selectCard(cardName);
            
            // and let's update the button states
            if (state == ActivationState.REFRESHING) {
                ((ActivatedButtonPanel)cardMap.get(OK_LICENSE_BUTTON_PANEL)).setRefreshEnabled(false);
            } else if (state == ActivationState.AUTHORIZED) {
                ((ActivatedButtonPanel)cardMap.get(OK_LICENSE_BUTTON_PANEL)).setRefreshEnabled(true);
            }
            
            activationPanel.revalidate();
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
        
        private String getLicenseErrorMessage(ActivationError error) {
            switch(error) {
            case BLOCKED_KEY:
                return I18n.tr("Your License Key has been used on too many installations.");
            case COMMUNICATION_ERROR:
                return I18n.tr("Connection error. Please try again later.");
            case INVALID_KEY:
                return I18n.tr("Invalid License Key.");
            default:
                return "";
            }           
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
            JButton goProButton = new JButton(new UrlAction(I18n.tr("Go PRO"), application.addClientInfoToUrl(ActivationSettingsController.UPSELL_URL)));
            goProButton.setToolTipText(I18n.tr("Upgrade to PRO"));
            activateButton = new JButton(new ActivateAction(I18n.tr("Activate"), I18n.tr("Activate the License Key")));
            JButton laterButton = new JButton(new OKDialogAction(I18n.tr("Later"), I18n.tr("Activate License at a later time")));
            
            add(goProButton, "push");
            add(activateButton, "split, gapright 10, tag ok");
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
        JButton refreshButton;
        HyperlinkButton editAccountButton;
        
        public ActivatedButtonPanel() {
            refreshButton = new JButton(new RefreshAction(I18n.tr("Refresh"), I18n.tr("Refresh the list of features associated with the key")));
            JButton okButton = new JButton(new OKDialogAction());
            
            String accountSettingsUrl = application.addClientInfoToUrl(ActivationSettingsController.ACCOUNT_SETTINGS_URL);
            editAccountButton = new HyperlinkButton(new UrlAction(I18n.tr("Edit Account"), accountSettingsUrl));
            editAccountButton.setToolTipText(I18n.tr("Edit information about the account associated with this License Key"));
            
            add(refreshButton, "split");
            add(editAccountButton, "push");
            add(okButton, "split, alignx 100%, tag ok, wrap");
        }
        
        @Override
        public void setActivationEnabled(boolean enabled){
        }

        public void setRefreshEnabled(boolean enabled){
            refreshButton.setEnabled(enabled);
        }
    }

    
    private static enum MessageState {
        BLOCKED, EXPIRED, UNSUPPORTED, NO_ERROR;
    }
    
    private class UnderneathActivationTableMessagePanel extends JPanel {
        
        private final JLabel iconLabel;
        private final LabelWithLinkSupport textLabel;
        
        public UnderneathActivationTableMessagePanel() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            iconLabel = new JLabel(unsupportedIcon);
            
            textLabel = new LabelWithLinkSupport();
            
            add(iconLabel, BorderLayout.WEST);
            add(textLabel, BorderLayout.CENTER);
        }

        public void setState(MessageState state) {
            switch(state) {
            case BLOCKED:
                textLabel.setText("<html>" + "<font size=\"3\" face=\"" + font.getFontName() + "\">"
                                  + I18n.tr("Please contact {0}Customer Support{1} to resolve the situation.", "<a href='" + application.addClientInfoToUrl(ActivationSettingsController.CUSTOMER_SUPPORT_URL) + "'>", "</a>") 
                                  + "</font></html>");
                iconLabel.setVisible(false);
                textLabel.setVisible(true);
                return;
            case EXPIRED:
                textLabel.setText("<html>" + "<font size=\"3\" face=\"" + font.getFontName() + "\">" +
                        I18n.tr("One or more of your features has expired. Click \"Edit Account\" to renew.")
                        + "</font></html>");
                iconLabel.setVisible(true);
                textLabel.setVisible(true);
                return;
            case UNSUPPORTED:
                textLabel.setText("<html>" + "<font size=\"3\" face=\"" + font.getFontName() + "\">" +
                            I18n.tr("One or more of your features is currently not active. Click on {0} for more information.", "<img src='" + ActivationUtilities.getInfoIconURL() + "'>")
                            + "</font></html>");
                iconLabel.setVisible(true);
                textLabel.setVisible(true);
                return;
            case NO_ERROR:
                iconLabel.setVisible(false);
                textLabel.setVisible(false);
                return;
            }
            throw new NotImplementedException("State does not exist");
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
    
    /**
     * Attempts to activate a new License Key.
     */
    private class ActivateAction extends AbstractAction {
        
        public ActivateAction() {
            this("", "");
        }
        
        public ActivateAction(String name, String description) {
            putValue(Action.NAME, name);
            putValue(Action.SHORT_DESCRIPTION, description);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            BackgroundExecutorService.execute(new Runnable(){
                public void run() {
                    activationManager.activateKey(licenseField.getText().trim().replaceAll("-", ""));
                }
            });
        }
    }
    
    /**
     * Attempts to refresh the currently loaded License Key. The Activation Server
     * will be contacted and the list of active and deactive modules will be 
     * reloaded. 
     */
    private class RefreshAction extends AbstractAction {
        public RefreshAction(String name, String description) {
            putValue(Action.NAME, name);
            putValue(Action.SHORT_DESCRIPTION, description);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            BackgroundExecutorService.execute(new Runnable(){
                public void run() {
                    activationManager.refreshKey(licenseField.getText().trim().replaceAll("-", ""));                    
                }
            });
        }
    }

    /**
     * Listens for changes that occur to the state of the ActivationManager and 
     * update the UI accordingly.
     */
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
