package org.limewire.ui.swing.options;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.DaapSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.options.actions.BrowseDirectoryAction;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Files Option View
 */
@Singleton
public class FilesOptionPanel extends OptionPanel {
    
    private final ManageSaveFoldersOptionPanelFactory manageFoldersOptionPanelFactory;
    private final ManageFileExtensionsOptionPanel manageFileExtensionsOptionPanel;
    
    private ManageExtensionsPanel manageExtensionsPanel;
    private SaveFoldersPanel saveFoldersPanel;
    private LimeWireStorePanel limeWireStorePanel;
    private ITunesPanel iTunesPanel;
    
    // Somebody needs this for testing the options panel without limewire?
    public static FilesOptionPanel createTestingFilesOptionPanel() {
        return null;//new FilesOptionPanel();
    }
        
    /*
    private FilesOptionPanel() {
        this(new ManageSaveFoldersOptionPanelFactory() {

            @Override
            public ManageSaveFoldersOptionPanel create(Action okAction,
                    CancelDialogAction cancelAction) {
                
                return new ManageSaveFoldersOptionPanel(
                        CategoryIconManager.createTestingCategoryIconManager(),
                        okAction, cancelAction);
                
            }
            
        });
    }
    */
    @Inject
    FilesOptionPanel(ManageSaveFoldersOptionPanelFactory manageFoldersOptionPanelFactory, 
            ManageFileExtensionsOptionPanel manageFileExtensionsOptionPanel) { 
        
        this.manageFoldersOptionPanelFactory = manageFoldersOptionPanelFactory;
        this.manageFileExtensionsOptionPanel = manageFileExtensionsOptionPanel;
        
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
    public void initOptions() {
        getManageExtensionsPanel().initOptions();
        getSaveOptionPanel().initOptions();
        getLimeWireStorePanel().initOptions();
        getITunesPanel().initOptions();
    }
    
    private class ManageExtensionsPanel extends OptionPanel {

        private ManageFileExtensionsOptionPanel extensionsPanel;
        private JButton manageButton;
        
        public ManageExtensionsPanel() {
            super(I18n.tr("Manage Extensions"));
            
            extensionsPanel = manageFileExtensionsOptionPanel;
            
            manageButton = new JButton(new DialogDisplayAction(FilesOptionPanel.this,
                    extensionsPanel, I18n.tr("Manage file extensions"),
                    I18n.tr("Manage"),I18n.tr("Manage file extensions to load")));
            
            add(new JLabel("Choose the specific file extensions LimeWire scans into your Library"), "push");
            add(manageButton);
        }
        
        @Override
        void applyOptions() {
            extensionsPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return extensionsPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            extensionsPanel.initOptions();
        }
    }
    
    private class SaveFoldersPanel extends OptionPanel {

        private ManageSaveFoldersOptionPanel saveFolderPanel;
        private JButton configureButton;
        
        public SaveFoldersPanel() {
            super(I18n.tr("Save Folders"));
            
            saveFolderPanel = manageFoldersOptionPanelFactory.create(new OKDialogAction(), new CancelDialogAction());
            saveFolderPanel.setSize(new Dimension(400,500));
            
            configureButton = new JButton(new DialogDisplayAction(FilesOptionPanel.this, saveFolderPanel, 
                    I18n.tr("Manage Save Folders"),I18n.tr("Configure"),I18n.tr("Configure how file types are saved")));
            
            add(new JLabel("Choose where specific file types get saved"), "push");
            add(configureButton);
        }
        
        @Override
        void applyOptions() {
            saveFolderPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return saveFolderPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            saveFolderPanel.initOptions();
        }
    }
    
    private class LimeWireStorePanel extends OptionPanel {

        private String currentSaveDirectory;
        private LWSFileNamingOptionPanel storeOptionPanel;
        
        private JTextField storePathTextField;
        private JButton browseStorePathButton;
        private JButton configureNamingButton;
        
        public LimeWireStorePanel() {
            super(I18n.tr("LimeWire Store"));
            
            storeOptionPanel = new LWSFileNamingOptionPanel(new OKDialogAction(), new CancelDialogAction());
            storeOptionPanel.setPreferredSize(new Dimension(350, 140));
            
            storePathTextField = new JTextField(40);
            browseStorePathButton = new JButton(new BrowseDirectoryAction(getParent(), storePathTextField));
            configureNamingButton = new JButton(new DialogDisplayAction(FilesOptionPanel.this,
                    storeOptionPanel,I18n.tr("LimeWire Store File Organization"),
                    I18n.tr("Configure file naming"),I18n.tr("Configure how files are automatically named")));
            
            add(new JLabel("Save store downloads to:"), "split");
            add(storePathTextField);
            add(browseStorePathButton, "wrap");
            
            add(configureNamingButton);
        }
        
        @Override
        void applyOptions() {
            final String save = storePathTextField.getText();
            if(!save.equals(currentSaveDirectory)) {
                try {
                    File saveDir = new File(save);
                    if(!saveDir.isDirectory()) {
                        if (!saveDir.mkdirs())
                            throw new IOException();
                    }
                    SharingSettings.setSaveLWSDirectory(saveDir);
                    currentSaveDirectory = save;
                } catch(IOException ioe) {
                    //TODO: error message
//                    GUIMediator.showError(I18n.tr("Invalid folder for saving files. Please use another folder or revert to the default."));
                    storePathTextField.setText(currentSaveDirectory);
                } catch(NullPointerException npe) {
                    //TODO: error message
//                    GUIMediator.showError(I18n.tr("Invalid folder for saving files. Please use another folder or revert to the default."));
                    storePathTextField.setText(currentSaveDirectory);
                }
            }
            
            storeOptionPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return !currentSaveDirectory.equals(storePathTextField.getText()) || 
                    storeOptionPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            storeOptionPanel.initOptions();
            try {
                File file = SharingSettings.getSaveLWSDirectory();
                if (file == null) {
                    throw (new FileNotFoundException());
                }
                currentSaveDirectory = file.getCanonicalPath();
                storePathTextField.setText(file.getCanonicalPath());
            } catch (FileNotFoundException fnfe) {
                // simply use the empty string if we could not get the save
                // directory.
                //TODO: change this to a real setting?? 
                currentSaveDirectory = "";
                storePathTextField.setText("");
            } catch (IOException ioe) {
                currentSaveDirectory = "";
                storePathTextField.setText("");
            }
        }
    }
    
    private class ITunesPanel extends OptionPanel {

        private JCheckBox shareWithITunesCheckBox;
        private JCheckBox requirePassWordCheckBox;
        private JPasswordField passwordField;
        
        public ITunesPanel() {
            super(I18n.tr("iTunes"));
            
            shareWithITunesCheckBox = new JCheckBox();
            shareWithITunesCheckBox.setContentAreaFilled(false);
            requirePassWordCheckBox = new JCheckBox();
            requirePassWordCheckBox.setContentAreaFilled(false);
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
        public void initOptions() {
            //TODO: share with init
            passwordField.setText(DaapSettings.DAAP_PASSWORD.getValue());
            requirePassWordCheckBox.setSelected(DaapSettings.DAAP_REQUIRES_PASSWORD.getValue());
        }
    }
}
