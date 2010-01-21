package org.limewire.ui.swing.activation;

import java.awt.CardLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class LicenseKeyPanel {
    
    final static String LICENSE_KEY_TEXT_FIELD = "text field";
    final static String LICENSE_KEY_LABEL = "label";
    
    private final JPanel licensePanel;
    private final LicenseKeyTextField licenseKeyTextField;
    private final JLabel licenseKeyLabel;
    private final CardLayout cardLayout;
    
    public LicenseKeyPanel(LicenseKeyTextField licenseKeyTextField) {   
        this.licenseKeyTextField = licenseKeyTextField;
        
        cardLayout = new CardLayout();
        licensePanel = new JPanel(cardLayout);
        licensePanel.setOpaque(false);

        licenseKeyLabel = new JLabel(licenseKeyTextField.getText());
        
        licensePanel.add(LICENSE_KEY_TEXT_FIELD, licenseKeyTextField);
        licensePanel.add(LICENSE_KEY_LABEL, licenseKeyLabel);

        cardLayout.show(licensePanel, LICENSE_KEY_TEXT_FIELD);
    }
    
    public JComponent getComponent() {
        return licensePanel;
    }
    
    public void setEditable(boolean editable) {
        if (editable) {
            cardLayout.show(licensePanel, LICENSE_KEY_TEXT_FIELD);
        } else {
            licenseKeyLabel.setText(licenseKeyTextField.getText());
            cardLayout.show(licensePanel, LICENSE_KEY_LABEL);
        }
    }
    
    public void setKey(String key) {
        licenseKeyTextField.setText(key);
        licenseKeyLabel.setText(key);
    }
}
