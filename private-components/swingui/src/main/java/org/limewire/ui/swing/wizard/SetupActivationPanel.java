package org.limewire.ui.swing.wizard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationSettingsController;
import org.limewire.activation.api.ActivationState;
import org.limewire.core.api.Application;
import org.limewire.core.settings.ActivationSettings;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.activation.ActivationWarningPanel;
import org.limewire.ui.swing.activation.LabelWithLinkSupport;
import org.limewire.ui.swing.activation.LicenseKeyTextField;
import org.limewire.ui.swing.activation.ActivationWarningPanel.Mode;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class SetupActivationPanel extends JPanel {

    private final ActivationManager activationManager;
    
    private JTextField licenseField;
    private ActivationWarningPanel iconPanel;
    private JLabel errorMessageLabel;
    private ActivationListener activationListener;
    private LabelWithLinkSupport customerSupportLabel;
    private JXButton okButton;
    
    @Resource
    private Color errorColor;

    public SetupActivationPanel(WizardPage wizardPage, ActivationManager activationManager, Application application) {
        super(new MigLayout("fillx, insets 75 60 10 60, gap 0, gapy 0", "[][grow][]", "[][][][][][][][][][]"));
        
        GuiUtils.assignResources(this);

        this.activationManager = activationManager;

        add(Box.createVerticalStrut(18), "spanx 1, growx, cell 2 1"); //wrap

        add(wizardPage.createAndDecorateHeader(I18n.tr("Activate your LimeWire PRO.")), "cell 2 2");
        
        HyperlinkButton goProButton = wizardPage.createAndDecorateHyperlink(application.addClientInfoToUrl(ActivationSettingsController.UPSELL_URL),
                                                                 I18n.tr("Want to go PRO?"));
        add(goProButton, "spanx 1, growx, cell 2 2"); //wrap

        add(Box.createVerticalStrut(15), "spanx 1, growx, cell 2 3"); //wrap

        add(wizardPage.createAndDecorateMultiLine(I18n.tr("Enter your activation key below. This was sent to the email address you used when purchasing. You can always do this later on from the File menu.")), 
                       "spanx 1, growx, cell 2 4, gapafter 20px"); //wrap

        errorMessageLabel = wizardPage.createAndDecorateMultiLine("");
        errorMessageLabel.setForeground(errorColor);
        errorMessageLabel.setVisible(false);
        add(errorMessageLabel, "spanx 1, growx, cell 2 5"); //wrap
        add(Box.createRigidArea(new Dimension(1, 33)), "spanx 1, growx, cell 2 5"); //wrap

        iconPanel = new ActivationWarningPanel();
        add(iconPanel.getComponent(), "cell 1 6, aligny 50%");
        add(Box.createHorizontalStrut(3), "cell 1 6, aligny 50%");

        add(wizardPage.createAndDecorateHeader(I18n.tr("License Key:")), "aligny 50%");
        licenseField = wizardPage.createAndDecorateLicenseKeyField();
        if (!ActivationSettings.ACTIVATION_KEY.isDefault()) {
            licenseField.setText(ActivationSettings.ACTIVATION_KEY.getValueAsString());
        }
        licenseField.addActionListener(new EnterActionListener());
        add(licenseField, "cell 2 6, aligny 50%");

        okButton = wizardPage.createAndDecorateButton(I18n.tr("Activate") );
        okButton.addActionListener(new EnterActionListener());
        okButton.setEnabled(licenseField.getText().length() == 14);
        licenseField.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(LicenseKeyTextField.LICENSE_IS_CORRECT_LENGTH)) {
                    Boolean enabled = (Boolean) evt.getNewValue();
                    okButton.setEnabled(enabled.booleanValue());
                }
            }
        });
        add(okButton, "cell 2 6, aligny 50%"); //wrap

        add(Box.createRigidArea(new Dimension(19, 16)), "cell 3 6, aligny 50%");

        add(Box.createVerticalStrut(10), "spanx 1, growx, cell 2 7"); //wrap

        // TODO Change this link to point to the correct get key page.
        add(Box.createHorizontalStrut(88), "cell 2 8, aligny 50%");
        HyperlinkButton unknownKeyButton = wizardPage.createAndDecorateHyperlink(application.addClientInfoToUrl(ActivationSettingsController.ACCOUNT_SETTINGS_URL),
                                                                                 I18n.tr("I don't know my license key"));
        add(unknownKeyButton, "spanx 1, growx, cell 2 8"); //wrap
        
        add(Box.createVerticalStrut(40), "cell 2 9");

        customerSupportLabel = new LabelWithLinkSupport();
        Font font = wizardPage.createAndDecorateLabel("").getFont();
        customerSupportLabel.setText("<html>" + "<font size=\"3\" face=\"" + font.getFontName() + "\">" 
                            + I18n.tr("Please contact {0}customer support{1} for more information.", "<a href='" + application.addClientInfoToUrl(ActivationSettingsController.CUSTOMER_SUPPORT_URL) + "'>", "</a>") 
                            + "</font></html>");

        customerSupportLabel.setVisible(false);
        add(customerSupportLabel, "align 25% 50%, spanx 1, cell 2 10, hidemode 3"); //wrap

        this.activationListener = new ActivationListener();
        this.activationManager.addListener(activationListener);
    }
    
    public void reset() {
        ActivationError activationError = activationManager.getActivationError();
        if (activationError != ActivationError.BLOCKED_KEY) {
            customerSupportLabel.setVisible(false);
            errorMessageLabel.setVisible(false);
            iconPanel.setActivationMode(Mode.EMPTY);
            licenseField.setText("");
        }
    }
    
    private void setActivationState(ActivationState state, ActivationError error) {
        okButton.setEnabled(true);
        
        switch(state) {
        case NOT_AUTHORIZED:
            if (error != ActivationError.COMMUNICATION_ERROR) {
                iconPanel.setActivationMode(Mode.WARNING);
            } else {
                iconPanel.setActivationMode(Mode.EMPTY);
            }
            break;
        case AUTHORIZING:
            okButton.setEnabled(false);
            iconPanel.setActivationMode(Mode.SPINNER);
            break;
        case REFRESHING:
            iconPanel.setActivationMode(Mode.SPINNER);
            break;
        case AUTHORIZED:
            iconPanel.setActivationMode(Mode.EMPTY);
            break;
        default:
            throw new IllegalStateException("Unknown state: " + state);
        }

        customerSupportLabel.setVisible(false);

        switch(error) {
        case NO_ERROR:
            errorMessageLabel.setVisible(false);
            break;
        case NO_KEY:
            errorMessageLabel.setVisible(false);
            break;
        case INVALID_KEY:
            errorMessageLabel.setText(I18n.tr("Invalid license key."));
            errorMessageLabel.setVisible(true);
            break;
        case BLOCKED_KEY:
            errorMessageLabel.setText(I18n.tr("Your license key has been used on too many installations."));
            errorMessageLabel.setVisible(true);
            customerSupportLabel.setVisible(true);
            break;
        case COMMUNICATION_ERROR:
            errorMessageLabel.setText(I18n.tr("Communication error. Please try again later."));
            errorMessageLabel.setVisible(true);
            break;
        default:
            throw new IllegalStateException("Unknown error: " + error);
        }
    }

    class EnterActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            final String key = licenseField.getText();
            if (key != null) {
                BackgroundExecutorService.execute(new Runnable(){
                    public void run() {
                        // we check for empty strings as key entries, and manually set the error for it,
                        // b/c the activation manager usually doesn't create an error for this case.
                        // it just clears the key.
                        if (key.equals("")) {
                            setActivationState(ActivationState.NOT_AUTHORIZED, ActivationError.INVALID_KEY);
                            return;
                        }
                        
                        activationManager.activateKey(key.replaceAll("-", ""));
                    }
                });
            }
        }
    }

    private class ActivationListener implements EventListener<ActivationEvent> {
        @Override
        public void handleEvent(final ActivationEvent event) {
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    setActivationState(event.getData(), event.getError());
                }
            });
        }
    }
}
