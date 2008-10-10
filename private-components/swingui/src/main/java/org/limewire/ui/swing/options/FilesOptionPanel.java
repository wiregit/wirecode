package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.DaapSettings;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Files Option View
 */
@Singleton
public class FilesOptionPanel extends OptionPanel {

    private ManageExtensionsPanel manageExtensionsPanel;
    private SaveFoldersPanel saveFoldersPanel;
    private LimeWireStorePanel limeWireStorePanel;
    private ITunesPanel iTunesPanel;
    
    @Inject
    public FilesOptionPanel() {         
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
    
        setOpaque(false);
        
        add(getManageExtensionsPanel(), "pushx, growx");
        add(getSaveOptionPanel(), "pushx, growx");
        add(getLimeWireStorePanel(), "pushx, growx");
        add(getITunesPanel(), "pushx, growx");
    }
    
    private OptionPanel getManageExtensionsPanel() {
        if(manageExtensionsPanel == null) {
            manageExtensionsPanel = new ManageExtensionsPanel();
        }
        return manageExtensionsPanel;
    }
    
    private OptionPanel getSaveOptionPanel() {
        if(saveFoldersPanel == null) {
            saveFoldersPanel = new SaveFoldersPanel();
        }
        return saveFoldersPanel;
    }
    
    private OptionPanel getLimeWireStorePanel() {
        if(limeWireStorePanel == null) {
            limeWireStorePanel = new LimeWireStorePanel();
        }
        return limeWireStorePanel;
    } 
    
    private OptionPanel getITunesPanel() {
        if(iTunesPanel == null) {
            iTunesPanel = new ITunesPanel();
        }
        return iTunesPanel;
    }

    @Override
    void applyOptions() {
        getManageExtensionsPanel().applyOptions();
        getSaveOptionPanel().applyOptions();
        getLimeWireStorePanel().applyOptions();
        getITunesPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getManageExtensionsPanel().hasChanged() || 
                getSaveOptionPanel().hasChanged() ||
                getLimeWireStorePanel().hasChanged() ||
                getITunesPanel().hasChanged();
    }

    @Override
    void initOptions() {
        getManageExtensionsPanel().initOptions();
        getSaveOptionPanel().initOptions();
        getLimeWireStorePanel().initOptions();
        getITunesPanel().initOptions();
    }
    
    private class ManageExtensionsPanel extends OptionPanel {

        private JButton manageButton;
        
        public ManageExtensionsPanel() {
            super(I18n.tr("Manage Extensions"));
            
            manageButton = new JButton(I18n.tr("Manage"));
            
            add(new JLabel("Choose the specific file extensions LimeWire scans into your Library"), "push");
            add(manageButton);
        }
        
        @Override
        void applyOptions() {
        }

        @Override
        boolean hasChanged() {
            return false;
        }

        @Override
        void initOptions() {

        }
    }
    
    private class SaveFoldersPanel extends OptionPanel {

        private JButton configureButton;
        
        public SaveFoldersPanel() {
            super(I18n.tr("Save Folders"));
            
            configureButton = new JButton(I18n.tr("Configure"));
            
            add(new JLabel("Choose where specific file types get saved"), "push");
            add(configureButton);
        }
        
        @Override
        void applyOptions() {
        }

        @Override
        boolean hasChanged() {
            return false;
        }

        @Override
        void initOptions() {

        }
    }
    
    private class LimeWireStorePanel extends OptionPanel {

        private JTextField storePathTextField;
        private JButton browseStorePathButton;
        private JButton configureNamingButton;
        
        public LimeWireStorePanel() {
            super(I18n.tr("LimeWire Store"));
            
            storePathTextField = new JTextField(40);
            browseStorePathButton = new JButton(I18n.tr("Browse"));
            configureNamingButton = new JButton(I18n.tr("Configure file naming"));
            
            add(new JLabel("Save store downloads to:"), "split");
            add(storePathTextField);
            add(browseStorePathButton, "wrap");
            
            add(configureNamingButton);
        }
        
        @Override
        void applyOptions() {
        }

        @Override
        boolean hasChanged() {
            return false;
        }

        @Override
        void initOptions() {

        }
    }
    
    private class ITunesPanel extends OptionPanel {

        private JCheckBox shareWithITunesCheckBox;
        private JCheckBox requirePassWordCheckBox;
        private JPasswordField passwordField;
        
        public ITunesPanel() {
            super(I18n.tr("iTunes"));
            
            shareWithITunesCheckBox = new JCheckBox();
            requirePassWordCheckBox = new JCheckBox();
            requirePassWordCheckBox.addItemListener(new ItemListener(){
                @Override
                public void itemStateChanged(ItemEvent e) {
                    passwordField.setEnabled(requirePassWordCheckBox.isSelected());
                }
            });
            passwordField = new JPasswordField(30);
            passwordField.setEnabled(false);
            
            add(shareWithITunesCheckBox, "split");
            add(new JLabel("Share the audio section of my LimeWire on my local network using iTunes"),"wrap");
            
            add(requirePassWordCheckBox, "gapleft 25, split");
            add(new JLabel("Require password"));
            add(passwordField);
        }
        
        @Override
        void applyOptions() {
            DaapSettings.DAAP_REQUIRES_PASSWORD.setValue(requirePassWordCheckBox.isSelected());
            
            //TODO: do some string checking for password length, etc.. see the old DaapSetting usage
        }

        @Override
        boolean hasChanged() {
            return DaapSettings.DAAP_REQUIRES_PASSWORD.getValue() != requirePassWordCheckBox.isSelected() ||
                    DaapSettings.DAAP_PASSWORD.getValue() != requirePassWordCheckBox.getText();
        }

        @Override
        void initOptions() {
            //TODO: share with init
            passwordField.setText(DaapSettings.DAAP_PASSWORD.getValue());
            requirePassWordCheckBox.setSelected(DaapSettings.DAAP_REQUIRES_PASSWORD.getValue());
        }
    }
}
