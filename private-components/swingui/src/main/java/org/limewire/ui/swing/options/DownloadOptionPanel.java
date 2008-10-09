package org.limewire.ui.swing.options;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.iTunesSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

/**
 * Downloads Option View
 */
public class DownloadOptionPanel extends OptionPanel {

    private SharingDownloadsPanel sharingDownloadsPanel;
    private SavingPanel savingPanel;
    private ITunesPanel iTunesPanel;
    
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
    void initOptions() {
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
            shareIncompleteDownloadsCheckBox = new JCheckBox();
            
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
        void initOptions() {
            shareCompletedDownloadsCheckBox.setSelected(SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue());
            shareIncompleteDownloadsCheckBox.setSelected(SharingSettings.ALLOW_PARTIAL_SHARING.getValue());
        }
    }
    
    private class SavingPanel extends OptionPanel {

        private JCheckBox clearDownloadsCheckBox;
        private JTextField downloadSaveTextField;
        private JButton browseSaveLocationButton;
        private JCheckBox autoRenameDuplicateFilesCheckBox;
        
        public SavingPanel() {
            super(I18n.tr("Saving"));
            
            clearDownloadsCheckBox = new JCheckBox();
            downloadSaveTextField = new JTextField();
            browseSaveLocationButton = new JButton(I18n.tr("Browse"));
            autoRenameDuplicateFilesCheckBox = new JCheckBox();
            
            add(clearDownloadsCheckBox, "split");
            add(new JLabel("Clear downloads from list when finished"), "wrap");
            
            add(new JLabel("Save downloads to:"), "split");
            add(downloadSaveTextField);
            add(browseSaveLocationButton, "wrap");
            
            add(autoRenameDuplicateFilesCheckBox, "gapleft 25, split");
            add(new JLabel("If the file already exists, download it with a different name"));
        }
        
        @Override
        void applyOptions() {
            SharingSettings.CLEAR_DOWNLOAD.setValue(clearDownloadsCheckBox.isSelected());
        }

        @Override
        boolean hasChanged() {
            return SharingSettings.CLEAR_DOWNLOAD.getValue() != clearDownloadsCheckBox.isSelected();
        }

        @Override
        void initOptions() {
            clearDownloadsCheckBox.setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());
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
        void initOptions() {
            addToITunesCheckBox.setSelected(iTunesSettings.ITUNES_SUPPORT_ENABLED.getValue());
        }        
    }

}
