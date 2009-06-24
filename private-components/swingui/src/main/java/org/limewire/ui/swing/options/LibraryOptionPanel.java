package org.limewire.ui.swing.options;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.setting.Setting;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HorizonalCheckBoxListPanel;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.options.OptionPanelStateManager.SettingChangedListener;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.NotImplementedException;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/** Library Option View */
public class LibraryOptionPanel extends OptionPanel {

    private @Resource Icon file_sharedlist_p2p_large;
    private @Resource Icon sharing_my_files;
    private @Resource Icon sharing_arrow;
    
    private final UsePlayerPanel playerPanel;
    private final CategoriesPanel categoryPanel;
    private final OptionPanel iTunesPanel;
    private OptionPanel sharingPanel;
    
    private final Provider<UnsafeTypeOptionPanel> unsafeOptionPanelProvider;
    private final Provider<ITunesOptionPanel> iTunesOptionPanelProvider;
    private final Provider<UnsafeTypeOptionPanelStateManager> unsafeTypeOptionPanelStateManagerProvider;
    
    private final LibraryManager libraryManager;
    
    @Inject
    public LibraryOptionPanel(LibraryManager libraryManager,
            Provider<UnsafeTypeOptionPanel> unsafeTypeOptionPanelProvider,
            Provider<ITunesOptionPanel> iTunesOptionPanelProvider,
            Provider<UnsafeTypeOptionPanelStateManager> stateManager) {
        
        this.libraryManager = libraryManager;
        this.unsafeOptionPanelProvider = unsafeTypeOptionPanelProvider;
        this.iTunesOptionPanelProvider = iTunesOptionPanelProvider;
        this.unsafeTypeOptionPanelStateManagerProvider = stateManager;
        
        GuiUtils.assignResources(this);
        
        this.playerPanel = new UsePlayerPanel();
        this.categoryPanel = new CategoriesPanel();

        setLayout(new MigLayout("insets 15, fillx"));

        // TODO: three different ways to add a panel!?
        add(categoryPanel, "growx, wrap");
        add(getSharingPanel(), "growx, wrap");

        if(OSUtils.isMacOSX() || OSUtils.isWindows()) {
            iTunesPanel = new ITunesPanel();
            add(iTunesPanel, "growx, wrap");
        }
        else {
            iTunesPanel = null;
        }
        
        add(playerPanel, "wrap");
    }

    @Override
    boolean applyOptions() {
        return playerPanel.applyOptions() || categoryPanel.applyOptions() || getSharingPanel().applyOptions() 
            || iTunesPanel != null ? iTunesPanel.applyOptions() : false;
    }

    @Override
    boolean hasChanged() {
        return playerPanel.hasChanged() || categoryPanel.hasChanged() || getSharingPanel().hasChanged()
            || iTunesPanel != null ? iTunesPanel.hasChanged() : false;
    }

    @Override
    public void initOptions() {
        getSharingPanel().initOptions();
        categoryPanel.initOptions();
        playerPanel.initOptions();
        if (iTunesPanel != null) {
            iTunesPanel.initOptions();
        }
    }

    private class CategoriesPanel extends OptionPanel {

        private HorizonalCheckBoxListPanel<Category> checkBoxes;
        private JCheckBox warnMeOnAddToSharedFolderCheckBox;
        
        public CategoriesPanel() {
            super("Categories");
            
            checkBoxes = new HorizonalCheckBoxListPanel<Category>(Category.getCategoriesInOrder());
            warnMeOnAddToSharedFolderCheckBox
                = new JCheckBox(I18n.tr("Warn me when I add a folder to a list that is shared."));
            warnMeOnAddToSharedFolderCheckBox.setOpaque(false);
            
            add(new JLabel(
                    I18n.tr("When I add a folder to the Library or to a List, only look for the following types of files:")),
                    "wrap");
            add(checkBoxes, "wrap");
            add(warnMeOnAddToSharedFolderCheckBox, "wrap");
        }

        @Override
        boolean applyOptions() {
            SharingSettings.WARN_SHARING_FOLDER.set(warnMeOnAddToSharedFolderCheckBox.isSelected());
            libraryManager.getLibraryData().setCategoriesToIncludeWhenAddingFolders(checkBoxes.getSelected());
            return false;
        }

        @Override
        boolean hasChanged() {
            Collection<Category> selectedCategories = checkBoxes.getSelected();
            Collection<Category> managedCategories = libraryManager.getLibraryData().getManagedCategories();
            return selectedCategories.size() != managedCategories.size() || !selectedCategories.containsAll(managedCategories)
                || SharingSettings.WARN_SHARING_FOLDER.get() != warnMeOnAddToSharedFolderCheckBox.isSelected();
        }

        @Override
        public void initOptions() {
            warnMeOnAddToSharedFolderCheckBox.setSelected(SharingSettings.WARN_SHARING_FOLDER.get());
            checkBoxes.setSelected(libraryManager.getLibraryData().getManagedCategories());
        }

    }

    private class ITunesPanel extends OptionPanel {

        private ITunesOptionPanel iTunesOptionPanel; 
        
        public ITunesPanel() {
            super("iTunes");

            iTunesOptionPanel = iTunesOptionPanelProvider.get();
            
            JButton configureButton = new JButton(new DialogDisplayAction(LibraryOptionPanel.this,
                    iTunesOptionPanel, I18n.tr("iTunes Configuration"),
                    I18n.tr("Configure..."), I18n.tr("Configure iTunes")));
            
            add(new JLabel(I18n.tr("Configure how files in your LimeWire interact with iTunes")));
            add(configureButton, "gapleft push");
        }

        @Override
        boolean applyOptions() {
            return iTunesOptionPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return iTunesOptionPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            iTunesOptionPanel.initOptions();
        }
    }

    
    /** Do you want to use the LW player? */
    private class UsePlayerPanel extends OptionPanel {

        private JCheckBox useLimeWirePlayer;

        public UsePlayerPanel() {
            super("");
            setBorder(BorderFactory.createEmptyBorder());
            setLayout(new MigLayout("ins 0 0 0 0, gap 0"));

            useLimeWirePlayer = new JCheckBox(I18n
                    .tr("Use the LimeWire player when I play Audio files"));
            useLimeWirePlayer.setOpaque(false);

            add(useLimeWirePlayer);
        }

        @Override
        boolean applyOptions() {
            SwingUiSettings.PLAYER_ENABLED.setValue(useLimeWirePlayer.isSelected());
            return false;
        }

        @Override
        boolean hasChanged() {
            return useLimeWirePlayer.isSelected() != SwingUiSettings.PLAYER_ENABLED.getValue();
        }

        @Override
        public void initOptions() {
            useLimeWirePlayer.setSelected(SwingUiSettings.PLAYER_ENABLED.getValue());
        }
    }
    
    private OptionPanel getSharingPanel() {
        if(sharingPanel == null) {
            sharingPanel = new SharingPanel();
        }
        return sharingPanel;
    }
    
    private class SharingPanel extends OptionPanel {
        
        private final JButton configureButton; 
        private final JCheckBox shareP2PdownloadedFilesCheckBox;
        private final UnsafeTypeOptionPanel unsafeTypeOptionPanel;
        private JLabel unsafeMessageLabel;
        
        public SharingPanel() {
            super(I18n.tr("Sharing"));
            
            unsafeTypeOptionPanel = unsafeOptionPanelProvider.get();
            
            shareP2PdownloadedFilesCheckBox = new JCheckBox(I18n.tr("Add files I download from P2P users to my Public Shared List"));
            shareP2PdownloadedFilesCheckBox.setOpaque(false);
            
            configureButton = new JButton(new DialogDisplayAction(LibraryOptionPanel.this,
                    unsafeTypeOptionPanel, I18n.tr("Unsafe Categories"),
                    I18n.tr("Configure..."), I18n.tr("Configure unsafe categories")));
            final String learnMoreUrl = "http://www.limewire.com/client_redirect/?page=documentsSharing";
            HyperlinkButton learnMoreButton = new LearnMoreButton(learnMoreUrl);
            
            addModifyInfo();                        
            
            add(shareP2PdownloadedFilesCheckBox);
            add(new HyperlinkButton(new AbstractAction(I18n.tr("Learn more")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    throw new NotImplementedException("share p2p downloads learn more not implemented");
                }
            }), "gapleft 15, wrap");
            
            unsafeMessageLabel = new JLabel();
            add(unsafeMessageLabel);
            add(learnMoreButton, "gapleft 15");
            add(configureButton, "gapleft 15");
            
            unsafeTypeOptionPanelStateManagerProvider.get().addSettingChangedListener(new SettingChangedListener() {
                @Override
                public void settingChanged(Setting setting) {
                    updateUnsafeMessage();   
                }
            });
        }
        
        private void addModifyInfo() {
            JPanel modifyInfoPanel = new JPanel(new MigLayout("nogrid, insets 0, gap 0"));
            modifyInfoPanel.setOpaque(false);
            JLabel myFiles = new JLabel(I18n.tr("My Files"), sharing_my_files, JLabel.CENTER);
            myFiles.setVerticalTextPosition(JLabel.BOTTOM);
            myFiles.setHorizontalTextPosition(JLabel.CENTER);

            modifyInfoPanel.add(new JLabel(I18n.tr("To see or modify files in your Public Shared list, go to:")), 
                    "wrap");
            
            modifyInfoPanel.add(myFiles);
            modifyInfoPanel.add(new JLabel(sharing_arrow), "aligny top, gaptop 17");
            modifyInfoPanel
                    .add(new JLabel(I18n.tr("Public Shared"), file_sharedlist_p2p_large, JLabel.RIGHT), "aligny top, gaptop 15");

            add(modifyInfoPanel, "wrap");
        }
        
        private void updateUnsafeMessage() {
            if (((Boolean)unsafeTypeOptionPanelStateManagerProvider.get().getValue(LibrarySettings.ALLOW_PROGRAMS)).booleanValue()
                    || ((Boolean)unsafeTypeOptionPanelStateManagerProvider.get().getValue(LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING)).booleanValue()) {
                unsafeMessageLabel.setText("You have enabled some unsafe file sharing options.");
            }
            else {
                unsafeMessageLabel.setText("LimeWire is preventing you from unsafe searching and sharing.");
            }
        }
        
        @Override
        boolean applyOptions() {
            SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES
                .setValue(shareP2PdownloadedFilesCheckBox.isSelected());
            SharingSettings.ALLOW_PARTIAL_SHARING
                .setValue(shareP2PdownloadedFilesCheckBox.isSelected());
            return unsafeTypeOptionPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES
                    .getValue() != shareP2PdownloadedFilesCheckBox.isSelected()
                || SharingSettings.ALLOW_PARTIAL_SHARING
                    .getValue() != shareP2PdownloadedFilesCheckBox.isSelected()
                || unsafeTypeOptionPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            unsafeTypeOptionPanel.initOptions();
            updateUnsafeMessage();
            
            shareP2PdownloadedFilesCheckBox.setSelected(
                    SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue());
        }
    }

}
