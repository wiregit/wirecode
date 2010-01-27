package org.limewire.ui.swing.wizard;

import java.awt.BorderLayout;

import javax.swing.SwingUtilities;

import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationState;
import org.limewire.core.api.Application;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.settings.ActivationSettings;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

public class SetupPage3 extends WizardPage {

    private final ActivationManager activationManager;

    private ActivationListener activationListener;

    private Wizard wizard;
    
    private String footerText;
    private String forwardText;
    private boolean hasBackButton;
    
    private final boolean userHasPreexistingLicense;
    
    public SetupPage3(SetupComponentDecorator decorator, 
                      Application application, 
                      LibraryData libraryData,
                      ActivationManager activationManager,
                      Wizard wizard) {
        
        super(decorator, application);
        
        GuiUtils.assignResources(this);

        this.activationManager = activationManager;

        this.wizard = wizard;
        
        setOpaque(false);
        setLayout(new BorderLayout());

        userHasPreexistingLicense = !ActivationSettings.ACTIVATION_KEY.isDefault();
        
        this.activationListener = new ActivationListener();
        this.activationManager.addListener(activationListener);

        if (activationManager.getActivationState() != ActivationState.AUTHORIZED) {
            showLicenseEntryPage();
        } else {
            showModuleInfoPage();
        }
    }
    
    @Override
    public void finalize() {
        activationManager.removeListener(activationListener);
        activationListener = null;
    }

    @Override
    protected String getForwardButtonText() {
        return forwardText;
    }

    @Override
    protected boolean hasBackButton() {
        return hasBackButton;
    }

    @Override
    public void applySettings() {
        // Auto-Sharing downloaded files Setting
    }

    @Override
    public String getFooter() {
        return footerText;
    }

    @Override
    public String getLine1() {
        return I18n.tr("LimeWire PRO");
    }

    @Override
    public String getLine2() {
        return "";
    }
    
    private void showLicenseEntryPage() {
        footerText = OSUtils.isMacOSX() ? I18n.tr("You can activate LimeWire Pro later from File > License...") 
                : I18n.tr("You can activate LimeWire Pro later from File > License...");
        forwardText = I18n.tr("Skip This Step");
        hasBackButton = true;
        add(new SetupActivationPanel(this, activationManager));
    }

    private void showModuleInfoPage() {
        removeAll();
        add(new SetupActivationThankYouPanel(SetupPage3.this, activationManager.getActivationItems(), userHasPreexistingLicense));
        footerText = I18n.tr("You can edit your licenses from File > License...");
        forwardText = I18n.tr("Done");
        hasBackButton = false;
        wizard.updateControls();
    }
    
    private class ActivationListener implements EventListener<ActivationEvent> {
        @Override
        public void handleEvent(final ActivationEvent event) {
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    if (event.getData() == ActivationState.AUTHORIZED) {
                        showModuleInfoPage();
                    }
                }
            });
        }
    }


}
