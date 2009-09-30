package org.limewire.ui.swing.options;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Application;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.setting.Setting;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.options.OptionPanelStateManager.SettingChangedListener;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/** Library Option View */
public class LibraryOptionPanel extends OptionPanel {
    
    @Resource private Icon p2pSharedListIcon;
    @Resource private Icon sharingMyFilesMacIcon;
    @Resource private Icon sharingMyFilesIcon;
    @Resource private Icon sharingArrowIcon;
    
    private final Application application;
    private final UsePlayerPanel playerPanel;
    private final CategoriesPanel categoryPanel;
    private final OptionPanel iTunesPanel;
    private OptionPanel sharingPanel;
    
    private final Provider<UnsafeTypeOptionPanel> unsafeOptionPanelProvider;
    private final Provider<ITunesOptionPanel> iTunesOptionPanelProvider;
    private final Provider<UnsafeTypeOptionPanelStateManager> unsafeTypeOptionPanelStateManagerProvider;
    
    @Inject
    public LibraryOptionPanel(
            Provider<UnsafeTypeOptionPanel> unsafeTypeOptionPanelProvider,
            Provider<ITunesOptionPanel> iTunesOptionPanelProvider,
            Provider<UnsafeTypeOptionPanelStateManager> stateManager,
            Application application) {
        this.application = application;
        this.unsafeOptionPanelProvider = unsafeTypeOptionPanelProvider;
        this.iTunesOptionPanelProvider = iTunesOptionPanelProvider;
        this.unsafeTypeOptionPanelStateManagerProvider = stateManager;
        
        GuiUtils.assignResources(this);
        
        this.playerPanel = new UsePlayerPanel();
        this.categoryPanel = new CategoriesPanel();

        setLayout(new MigLayout("insets 15, fillx, gap 4"));

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
        
        public CategoriesPanel() {
            super(I18n.tr("Adding Folders"));
            
            add(new JLabel("TODO: Flesh out this option"));
        }

        @Override
        boolean applyOptions() {
            return false;
        }

        @Override
        boolean hasChanged() {
            return false;
        }

        @Override
        public void initOptions() {
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
    private static class UsePlayerPanel extends OptionPanel {

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
            
            shareP2PdownloadedFilesCheckBox = new JCheckBox("<html>"+I18n.tr("Add files I download from P2P Users to my Public Shared List")+"</html>");
            shareP2PdownloadedFilesCheckBox.setOpaque(false);
            
            configureButton = new JButton(new DialogDisplayAction(LibraryOptionPanel.this,
                    unsafeTypeOptionPanel, I18n.tr("Unsafe File Sharing"),
                    I18n.tr("Configure..."), I18n.tr("Configure unsafe file sharing settings")));
            final String learnMoreUrl = "http://www.limewire.com/client_redirect/?page=documentsSharing";
            HyperlinkButton learnMoreButton = new LearnMoreButton(learnMoreUrl, application);
            
            addModifyInfo();                        
            
            add(shareP2PdownloadedFilesCheckBox);
            add(new LearnMoreButton("http://www.limewire.com/client_redirect/?page=autoSharingMoreInfo", application), "gapleft 15, wrap");
            
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
            
            Icon myFilesIcon = null;
            if (OSUtils.isMacOSX()) {
                myFilesIcon = sharingMyFilesMacIcon;
            } 
            else {
                myFilesIcon = sharingMyFilesIcon;
            }
            
            JPanel modifyInfoPanel = new JPanel(new MigLayout("nogrid, insets 0, gap 0"));
            modifyInfoPanel.setOpaque(false);
                       
            JLabel myFiles = new JLabel(I18n.tr("My Files"), myFilesIcon, JLabel.CENTER);
            myFiles.setVerticalTextPosition(JLabel.BOTTOM);
            myFiles.setHorizontalTextPosition(JLabel.CENTER);

            modifyInfoPanel.add(new JLabel(I18n.tr("To see or modify files in your Public Shared list, go to:")), 
                    "gapbottom 10, wrap");
            
            modifyInfoPanel.add(myFiles);
            modifyInfoPanel.add(new JLabel(sharingArrowIcon), "aligny top, gaptop 17");
            
            
            modifyInfoPanel
                    .add(new JLabel(I18n.tr("Public Shared"), p2pSharedListIcon, JLabel.RIGHT), "aligny top, gaptop 15");

            add(modifyInfoPanel, "wrap");
        }
        
        private void updateUnsafeMessage() {
            if (((Boolean)unsafeTypeOptionPanelStateManagerProvider.get().getValue(LibrarySettings.ALLOW_PROGRAMS)).booleanValue()
                    || ((Boolean)unsafeTypeOptionPanelStateManagerProvider.get().getValue(LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING)).booleanValue()) {
                unsafeMessageLabel.setText("<html>"+I18n.tr("You have enabled some unsafe file sharing options.")+"</html>");
            }
            else {
                unsafeMessageLabel.setText("<html>"+I18n.tr("LimeWire is preventing you from unsafe searching and sharing.")+"</html>");
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
