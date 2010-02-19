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

public class SetupPage3 extends WizardPage {

    private final ActivationManager activationManager;
    private final Wizard wizard;
    private final Application application;
    private final boolean userHasPreexistingLicense;
    
    private ActivationListener activationListener;
    
    private String footerText;
    private String forwardText;
    private boolean hasBackButton;
    
    private SetupActivationPanel activationPanel;
    
    public SetupPage3(SetupComponentDecorator decorator, 
                      Application application, 
                      LibraryData libraryData,
                      ActivationManager activationManager,
                      Wizard wizard) {
        super(decorator, application);
        
        GuiUtils.assignResources(this);

        this.application = application;
        this.activationManager = activationManager;
        this.wizard = wizard;
        userHasPreexistingLicense = !ActivationSettings.ACTIVATION_KEY.isDefault();
        
        setOpaque(false);
        setLayout(new BorderLayout());

        if (activationManager.getActivationState() != ActivationState.AUTHORIZED) {
            this.activationListener = new ActivationListener();
            this.activationManager.addListener(activationListener);
            showLicenseEntryPage();
        } else {
            showModuleInfoPage();
        }
    }
    
    @Override
    public void finalize() {
        if(activationListener != null) {
            activationManager.removeListener(activationListener);
            activationListener = null;
        }
    }

    @Override
    public void setVisible(boolean visible) {
        // if they leave this setup page and come back to it, let's clear its state.
        if (visible) {
            if (activationPanel != null) {
                activationPanel.reset();
            }
        }
        
        super.setVisible(visible);
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
        footerText = I18n.tr("You can activate LimeWire PRO later from File > License"); 
        forwardText = I18n.tr("No thanks");
        hasBackButton = true;
        activationPanel = new SetupActivationPanel(this, activationManager, application); 
        add(activationPanel);
    }

    private void showModuleInfoPage() {
        removeAll();
        add(new SetupActivationThankYouPanel(SetupPage3.this, activationManager.getActivationItems(), userHasPreexistingLicense, application));
        footerText = I18n.tr("You can edit your licenses from File > License");
        forwardText = I18n.tr("Done");
        hasBackButton = false;
        wizard.updateControls();
    }

    private class ActivationListener implements EventListener<ActivationEvent> {
        @Override
        public void handleEvent(final ActivationEvent event) {
            if (event.getData() == ActivationState.AUTHORIZED) {
                activationManager.removeListener(activationListener);
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        showModuleInfoPage();
                    }
                });
            }
        }
    }
}
