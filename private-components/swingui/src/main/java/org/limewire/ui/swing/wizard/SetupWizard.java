package org.limewire.ui.swing.wizard;

import java.awt.Frame;

import org.limewire.core.api.Application;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.settings.InstallSettings;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class SetupWizard {

    private Wizard wizard;
    private final Application application;
    private final LibraryData libraryData;

    @Inject
    public SetupWizard(Provider<SetupComponentDecoratorFactory> decoratorFactory,
            Provider<LibraryManager> libraryManagerProvider, Application application) {
        
        this.application = application;
        this.libraryData = libraryManagerProvider.get().getLibraryData();        
        
        createWizard(decoratorFactory.get(), libraryData);
    }

    public static boolean shouldShowWizard() {
        return shouldShowPage1();
    }

    public void showDialog(Frame owner) {           
        wizard.showDialog(owner);
    }

    private void createWizard(SetupComponentDecoratorFactory decoratorFactory,
            LibraryData libraryData) {

        SetupComponentDecorator decorator = decoratorFactory.create();

        wizard = new Wizard(decorator);

        if (shouldShowPage1()) {
            wizard.addPage(new SetupPage1(decorator, application));
        }
        
        wizard.addPage(new SetupPage2(decorator, application, libraryData));
    }

    private static boolean shouldShowPage1() {
        if (!InstallSettings.AUTO_SHARING_OPTION.getValue()) {
            return true;
        }
        if (!InstallSettings.ANONYMOUS_DATA_COLLECTION.getValue()) {
            return true;
        }
        if (!InstallSettings.FILTER_OPTION.getValue()) {
            return true;
        }
        if (!InstallSettings.START_STARTUP.getValue()) {
            return GuiUtils.shouldShowStartOnStartupWindow();
        }

        return false;
    }

}
