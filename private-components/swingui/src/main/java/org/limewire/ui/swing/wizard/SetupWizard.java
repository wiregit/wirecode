package org.limewire.ui.swing.wizard;


import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.settings.InstallSettings;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;


public class SetupWizard extends Wizard {
    
    @Inject
    public SetupWizard(LibraryManager libraryManager){
        createPages(libraryManager.getLibraryData());
    }

    
    protected void createPages(LibraryData libraryData){
        if(needsPage1()){
            addPage(new SetupPage1());
        }        

        if(needsPage2()){
            addPage(new SetupPage2(libraryData));
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
