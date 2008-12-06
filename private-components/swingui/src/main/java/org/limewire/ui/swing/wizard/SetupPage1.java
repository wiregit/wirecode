package org.limewire.ui.swing.wizard;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.ContentSettings;
import org.limewire.core.settings.InstallSettings;
import org.limewire.core.settings.StartupSettings;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.shell.LimeAssociationOption;
import org.limewire.ui.swing.shell.LimeAssociations;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.MacOSXUtils;
import org.limewire.ui.swing.util.WindowsUtils;
import org.limewire.util.OSUtils;



public class SetupPage1 extends WizardPage {

    private String line1 = I18n.tr("A few things before we begin...");
    private String line2 = I18n.tr("Please take a minute to configure these options before moving on.");
    
    private String filterTitle = I18n.tr("Content Filters");
    
    private String learnMore = I18n.tr("Learn more"); 
    
    private String filterCheckText = I18n.tr("Don't let me downloador upload files copyright owners request not to be shared.");
    private JCheckBox filterCheck;

    private String associationsAndStartupTitle = I18n.tr("File Associations and Startup");
    private String associationsText = I18n.tr("Associate .magnet and .torrent files with LimeWire");    
    private String startupText = I18n.tr("Launch LimeWire at Startup");
    
    private JCheckBox associationCheck;
    private JCheckBox startupCheck;    
    
    public SetupPage1(SetupComponentDecorator decorator){

        setOpaque(false);
        
        setLayout(new MigLayout());
        
        HyperLinkButton learnMoreButton = new HyperLinkButton(learnMore);
        filterCheck = new JCheckBox(filterCheckText);
        decorator.decorateLargeCheckBox(filterCheck);
        
        associationCheck = new JCheckBox(associationsText);
        decorator.decorateLargeCheckBox(associationCheck);
        associationCheck.setSelected(true);

        startupCheck = new JCheckBox(startupText);  
        decorator.decorateLargeCheckBox(startupCheck);
        
        if (shouldShowStartOnStartupWindow()) {
            startupCheck.setSelected(true);
        }

        int checkBoxIndent = 50;
        add(new JLabel(filterTitle), "gaptop 20, gap left 30, wrap");
        add(filterCheck, "gaptop 20, gap left " + checkBoxIndent+ ", wrap");
        add(learnMoreButton, "gap left 40, wrap");
        
        add(new JLabel(associationsAndStartupTitle), "gaptop 20, gap left 30, wrap");
        add(associationCheck, "gaptop 10, gap left " + checkBoxIndent+ ", wrap");
        if (shouldShowStartOnStartupWindow()) {
            add(startupCheck, "gaptop 10, gap left " + checkBoxIndent + ", wrap");
        }
        
    }

    @Override
    public String getLine1() {
        return line1;
    }
    
    @Override
    public String getLine2() {
        return line2;
    }
    
    @Override
    public void applySettings() {
        // filter settings
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(filterCheck.isSelected());
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(filterCheck.isSelected());
        InstallSettings.FILTER_OPTION.setValue(true);

        ApplicationSettings.HANDLE_MAGNETS.setValue(associationCheck.isSelected());
        LimeAssociationOption magnetAssociationOption = LimeAssociations.getMagnetAssociation();
        if (magnetAssociationOption != null) {
            magnetAssociationOption.setEnabled(associationCheck.isSelected());
        }

        ApplicationSettings.HANDLE_TORRENTS.setValue(associationCheck.isSelected());
        LimeAssociationOption torrentAssociationOption = LimeAssociations.getTorrentAssociation();
        if (torrentAssociationOption != null) {
            torrentAssociationOption.setEnabled(associationCheck.isSelected());
        }

        InstallSettings.ASSOCIATION_OPTION.setValue(2);

        // launch at startup
        if (shouldShowStartOnStartupWindow()) {
            if (OSUtils.isMacOSX())
                MacOSXUtils.setLoginStatus(startupCheck.isSelected());
            else if (WindowsUtils.isLoginStatusAvailable())
                WindowsUtils.setLoginStatus(startupCheck.isSelected());

            StartupSettings.RUN_ON_STARTUP.setValue(startupCheck.isSelected());
        }
        InstallSettings.START_STARTUP.setValue(true);

    }
    
    /**
     * Determines if the Start On Startup option is available.
     */
    private boolean shouldShowStartOnStartupWindow() {
        return OSUtils.isMacOSX() || OSUtils.isGoodWindows();
    }
 
}
