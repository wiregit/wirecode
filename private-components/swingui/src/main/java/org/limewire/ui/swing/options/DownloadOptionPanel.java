package org.limewire.ui.swing.options;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.iTunesSettings;
import org.limewire.ui.swing.options.actions.BrowseDirectoryAction;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Downloads Option View
 */
@Singleton
public class DownloadOptionPanel extends OptionPanel {

    private SharingDownloadsPanel sharingDownloadsPanel;
    private SavingPanel savingPanel;
    private ITunesPanel iTunesPanel;
    
    @Inject
    public DownloadOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getSharingDownloadsPanel(), "pushx, growx");
        add(getSavingPanel(), "pushx, growx");
        if(OSUtils.isAnyMac() || OSUtils.isWindows())
            add(getITunesPanel(), "pushx, growx");
    }
    
    private OptionPanel getSharingDownloadsPanel() {
        if(sharingDownloadsPanel == null) {
            sharingDownloadsPanel = new SharingDownloadsPanel();
        }
        return sharingDownloadsPanel;
    }
    
    private OptionPanel getSavingPanel() {
        if(savingPanel == null) {
            savingPanel = new SavingPanel();
        }
        return savingPanel;
    }
    
    private OptionPanel getITunesPanel() {
        if(iTunesPanel == null) {
            iTunesPanel = new ITunesPanel();
        }
        return iTunesPanel;
    }

    @Override
    void applyOptions() {
        getSharingDownloadsPanel().applyOptions();
        getSavingPanel().applyOptions();
        getITunesPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getSharingDownloadsPanel().hasChanged() || getSavingPanel().hasChanged() || getITunesPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getSharingDownloadsPanel().initOptions();
        getSavingPanel().initOptions();
        getITunesPanel().initOptions();
    }
    
    /**
     * Setting for automatically sharing completed and incomplete downloads
     * with the Gnutella network.
     */
    private class SharingDownloadsPanel extends OptionPanel {

        private JCheckBox shareCompletedDownloadsCheckBox;
        private JCheckBox shareIncompleteDownloadsCheckBox;
        
        public SharingDownloadsPanel() {
            super(I18n.tr("Sharing downloads"));
            
            shareCompletedDownloadsCheckBox = new JCheckBox();
            shareCompletedDownloadsCheckBox.setContentAreaFilled(false);
            shareIncompleteDownloadsCheckBox = new JCheckBox();
            shareIncompleteDownloadsCheckBox.setContentAreaFilled(false);
            
            add(shareCompletedDownloadsCheckBox);
            add(new JLabel("Share files downloaded from the LimeWire Network with the LimeWire Network"), "wrap");
            
            add(shareIncompleteDownloadsCheckBox);
            add(new JLabel("Also share files being downloaded from the LimeWire Network with the LimeWire Network"), "wrap");
        }
        
        @Override
        void applyOptions() {
            SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.setValue(shareCompletedDownloadsCheckBox.isSelected());
            SharingSettings.ALLOW_PARTIAL_SHARING.setValue(shareIncompleteDownloadsCheckBox.isSelected());
        }

        @Override
        boolean hasChanged() {
            return SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue() != shareCompletedDownloadsCheckBox.isSelected()
                    || SharingSettings.ALLOW_PARTIAL_SHARING.getValue() != shareIncompleteDownloadsCheckBox.isSelected();
        }

        @Override
        public void initOptions() {
            shareCompletedDownloadsCheckBox.setSelected(SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue());
            shareIncompleteDownloadsCheckBox.setSelected(SharingSettings.ALLOW_PARTIAL_SHARING.getValue());
        }
    }
    
    private class SavingPanel extends OptionPanel {

        private String currentSaveDirectory;
        
        private JCheckBox clearDownloadsCheckBox;
        private JTextField downloadSaveTextField;
        private JButton browseSaveLocationButton;
        private JCheckBox autoRenameDuplicateFilesCheckBox;
        
        public SavingPanel() {
            super(I18n.tr("Saving"));
            
            clearDownloadsCheckBox = new JCheckBox();
            clearDownloadsCheckBox.setContentAreaFilled(false);
            downloadSaveTextField = new JTextField();
            downloadSaveTextField.setEditable(false);
            downloadSaveTextField.setBackground(Color.WHITE);
            browseSaveLocationButton = new JButton(new BrowseDirectoryAction(getParent(), downloadSaveTextField));
            autoRenameDuplicateFilesCheckBox = new JCheckBox();
            autoRenameDuplicateFilesCheckBox.setContentAreaFilled(false);
            
            add(clearDownloadsCheckBox, "split 2");
            add(new JLabel("Clear downloads from list when finished"), "wrap");
            
            add(new JLabel("Save downloads to:"), "split 3");
            add(downloadSaveTextField, "span, growx, push");
            add(browseSaveLocationButton, "wrap");
            
            add(autoRenameDuplicateFilesCheckBox, "gapleft 25, split 2");
            add(new JLabel("If the file already exists, download it with a different name"));
        }
        
        @Override
        void applyOptions() {
            SharingSettings.CLEAR_DOWNLOAD.setValue(clearDownloadsCheckBox.isSelected());
            final String save = downloadSaveTextField.getText();
            if(!save.equals(currentSaveDirectory)) {
                try {
                    File saveDir = new File(save);
                    if(!saveDir.isDirectory()) {
                        if (!saveDir.mkdirs())
                            throw new IOException();
                    }
                    SharingSettings.setSaveDirectory(saveDir);
                    currentSaveDirectory = save;
                } catch(IOException ioe) {
                    //TODO: error message
//                    GUIMediator.showError(I18n.tr("Invalid folder for saving files. Please use another folder or revert to the default."));
                    downloadSaveTextField.setText(currentSaveDirectory);
                } catch(NullPointerException npe) {
                    //TODO: error message
//                    GUIMediator.showError(I18n.tr("Invalid folder for saving files. Please use another folder or revert to the default."));
                    downloadSaveTextField.setText(currentSaveDirectory);
                }
            }
        }

        @Override
        boolean hasChanged() { //TODO: rename file checkbox init
            return SharingSettings.CLEAR_DOWNLOAD.getValue() != clearDownloadsCheckBox.isSelected()
                    || !currentSaveDirectory.equals(downloadSaveTextField.getText());
        }

        @Override
        public void initOptions() { //TODO: rename file setting
            clearDownloadsCheckBox.setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());

            //TODO: handle error dialog when download already exists
            
            try {
                File file = SharingSettings.getSaveDirectory();
                if (file == null) {
                    throw (new FileNotFoundException());
                }
                currentSaveDirectory = file.getCanonicalPath();
                downloadSaveTextField.setText(file.getCanonicalPath());
            } catch (FileNotFoundException fnfe) {
                // simply use the empty string if we could not get the save
                // directory.
                //TODO: change this to a real setting?? 
                currentSaveDirectory = "";
                downloadSaveTextField.setText("");
            } catch (IOException ioe) {
                currentSaveDirectory = "";
                downloadSaveTextField.setText("");
            }
        }
    }
    
    /**
     * Setting for automatically importing songs into your iTunes library. 
     * NOTE: this is only available on OSX and Windows
     */
    private class ITunesPanel extends OptionPanel {

        private JCheckBox addToITunesCheckBox;
        
        public ITunesPanel() {
            super(I18n.tr("iTunes"));
            
            addToITunesCheckBox = new JCheckBox();
            addToITunesCheckBox.setContentAreaFilled(false);
            
            add(addToITunesCheckBox);
            add(new JLabel("Add audio files I downloaded from LimeWire to my iTunes Library"), "wrap");
        }
        
        @Override
        void applyOptions() {
            iTunesSettings.ITUNES_SUPPORT_ENABLED.setValue(addToITunesCheckBox.isSelected());
        }

        @Override
        boolean hasChanged() {
            return iTunesSettings.ITUNES_SUPPORT_ENABLED.getValue() != addToITunesCheckBox.isSelected();
        }

        @Override
        public void initOptions() {
            addToITunesCheckBox.setSelected(iTunesSettings.ITUNES_SUPPORT_ENABLED.getValue());
        }        
    }

}
