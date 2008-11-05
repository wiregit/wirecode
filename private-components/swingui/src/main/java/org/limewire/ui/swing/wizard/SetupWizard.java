package org.limewire.ui.swing.wizard;


import java.awt.Frame;

import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.settings.InstallSettings;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;


public class SetupWizard {
    
    private Wizard wizard;
    
    @Inject
    public SetupWizard(LibraryManager libraryManager){        
        if (shouldShowWizard()) {
            createWizard(libraryManager.getLibraryData());
        }
    }
    
    public boolean shouldShowWizard(){
        return needsPage1() || needsPage2();
    }
    
    public void showDialogIfNeeded(Frame owner) {
        if (shouldShowWizard()) {
            wizard.showDialogIfNeeded(owner);
        }
    }
        
    private void createWizard(LibraryData libraryData){
        wizard = new Wizard();
        if(needsPage1()){
            wizard.addPage(new SetupPage1());
        }        

        if(needsPage2()){
            wizard.addPage(new SetupPage2(libraryData));
        }
    }
    
    private boolean needsPage1(){
        if (!InstallSettings.FILTER_OPTION.getValue()) {
            return true;
        }
        if (!InstallSettings.START_STARTUP.getValue()) {
            return GuiUtils.shouldShowStartOnStartupWindow();
        }
        return false;
    }
    
    private boolean needsPage2(){
        return !InstallSettings.SCAN_FILES.getValue();
    }

}
