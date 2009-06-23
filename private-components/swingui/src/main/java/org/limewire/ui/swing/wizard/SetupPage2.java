package org.limewire.ui.swing.wizard;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.settings.InstallSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

public class SetupPage2 extends WizardPage {
    private final JCheckBox shareDownloadedFilesCheckBox;

    private @Resource
    Icon file_sharedlist_p2p_large;

    private @Resource
    Icon sharing_my_files;

    private @Resource
    Icon sharing_arrow;

    public SetupPage2(SetupComponentDecorator decorator) {
        super(decorator);

        GuiUtils.assignResources(this);

        setOpaque(false);
        setLayout(new MigLayout("insets 0, gap 0, fill, align center"));

        shareDownloadedFilesCheckBox = createAndDecorateCheckBox(true);

        boolean newInstall = InstallSettings.PREVIOUS_RAN_VERSIONS.get().size() == 0;

        addAutoSharing(newInstall);
        addSeperator();
        addModifyInfo();
        if (!newInstall) {
            addSeperator();
            addOldVersionInfo();
        }

        initSettings();
    }

    private void addSeperator() {
        JSeparator separator = new JSeparator();
        separator.setForeground(Color.BLACK);
        add(separator, "growx, span, wrap");

    }

    private void initSettings() {
        shareDownloadedFilesCheckBox
                .setSelected(SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES
                        .getValue());
    }

    @Override
    public void applySettings() {
        // Auto-Sharing downloaded files Setting
        SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES
                .setValue(shareDownloadedFilesCheckBox.isSelected());
        SharingSettings.ALLOW_PARTIAL_SHARING.setValue(shareDownloadedFilesCheckBox.isSelected());
        InstallSettings.AUTO_SHARING_OPTION.setValue(true);
    }

    @Override
    public String getFooter() {
        return OSUtils.isMacOSX() ? I18n
                .tr("All settings can be changed later from LimeWire > Preferences") : I18n
                .tr("All settings can be changed later in Tools > Options");
    }

    @Override
    public String getLine1() {
        return I18n.tr("Learn about sharing.");
    }

    @Override
    public String getLine2() {
        return "";
    }

    /**
     * Adds header for Auto-Sharing, checkbox and associated text
     */
    private void addAutoSharing(boolean newInstall) {
        JPanel autoSharingPanel = new JPanel(new MigLayout("fill, insets 0, gap 0, nogrid"));

        autoSharingPanel.add(createAndDecorateHeader(I18n
                .tr("Files in your Public Shared list are shared with the world.")),
                "alignx center, wrap");
        if (newInstall) {
            autoSharingPanel.add(shareDownloadedFilesCheckBox, "alignx center");
            autoSharingPanel.add(createAndDecorateMultiLine(I18n
                    .tr("Add files I download from P2P Users to my Public Shared list."),
                    shareDownloadedFilesCheckBox));
            autoSharingPanel
                    .add(
                            createAndDecorateHyperlink("http://www.limewire.com/client_redirect/?page=autoSharingMoreInfo"),
                            "wrap");
        } else if (SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue()) {
            autoSharingPanel
                    .add(
                            createAndDecorateSubHeading(I18n
                                    .tr("LimeWire will add files you download from P2P Users into your Public Shared List.")),
                            "alignx center");
            autoSharingPanel
                    .add(
                            createAndDecorateHyperlink("http://www.limewire.com/client_redirect/?page=autoSharingMoreInfo"),
                            "wrap");
        }

        add(autoSharingPanel, "growx, span, sg sameRowSize, wrap");
    }

    private void addModifyInfo() {
        JPanel modifyInfoPanel = new JPanel(new MigLayout("fill, insets 0, gap 0, nogrid"));
        modifyInfoPanel.add(createAndDecorateHeader(I18n
                .tr("To see or modify files in your Public Shared list, go to")),
                "alignx center, wrap");

        JLabel myFiles = new JLabel(I18n.tr("My Files"), sharing_my_files, JLabel.CENTER);
        myFiles.setVerticalTextPosition(JLabel.BOTTOM);
        myFiles.setHorizontalTextPosition(JLabel.CENTER);

        modifyInfoPanel.add(myFiles, "alignx center");
        modifyInfoPanel.add(new JLabel(sharing_arrow), "aligny top, gaptop 17");
        modifyInfoPanel.add(
                new JLabel(I18n.tr("Public Shared"), file_sharedlist_p2p_large, JLabel.RIGHT),
                "aligny top, gaptop 15");

        add(modifyInfoPanel, "growx, span, sg sameRowSize, wrap");
    }

    private void addOldVersionInfo() {
        JPanel oldVersionInfoPanel = new JPanel(new MigLayout("fill, insets 0, gap 0"));
        oldVersionInfoPanel.add(createAndDecorateHeader(I18n
                .tr("Shared files from your old version will be in your Public Shared list.")),
                "alignx center, wrap");
        add(oldVersionInfoPanel, "growx, span, sg sameRowSize, wrap");
    }
}
