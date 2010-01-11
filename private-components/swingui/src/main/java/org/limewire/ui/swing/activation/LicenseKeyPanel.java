package org.limewire.ui.swing.activation;

import java.awt.CardLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class LicenseKeyPanel extends JPanel {
    
    final static String LICENSE_KEY_TEXT_FIELD = "text field";
    final static String LICENSE_KEY_LABEL = "label";
    
    private final LicenseKeyTextField licenseKeyTextField;
    private final JLabel licenseKeyLabel;
    
    public LicenseKeyPanel(LicenseKeyTextField licenseKeyTextField) {
        super(new CardLayout());
        
        setOpaque(false);
        
        this.licenseKeyTextField = licenseKeyTextField;
        
        add(LICENSE_KEY_TEXT_FIELD, licenseKeyTextField);

        licenseKeyLabel = new JLabel(licenseKeyTextField.getText());
        add(LICENSE_KEY_LABEL, licenseKeyLabel);

        CardLayout cardLayout = (CardLayout) (getLayout());
        cardLayout.show(this, LICENSE_KEY_TEXT_FIELD);
    }
    
    public void setEditable(boolean editable) {
        if (editable) {
            CardLayout cardLayout = (CardLayout) (getLayout());
            cardLayout.show(this, LICENSE_KEY_TEXT_FIELD);
        } else {
            licenseKeyLabel.setText(licenseKeyTextField.getText());
            CardLayout cardLayout = (CardLayout) (getLayout());
            cardLayout.show(this, LICENSE_KEY_LABEL);
        }
    }
}
