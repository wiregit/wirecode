package org.limewire.ui.swing.options;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.iTunesSettings;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;

/** Library Option View */
public class LibraryOptionPanel extends OptionPanel {

    private final UsePlayerPanel playerPanel;

    private final LibraryPanel libraryPanel;
    
    private final LibraryManager libraryManager;
    
    private final JCheckBox shareCompletedDownloadsCheckBox;
    private final JCheckBox addToITunesCheckBox;

    @Inject
    public LibraryOptionPanel(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
        this.playerPanel = new UsePlayerPanel();
        this.libraryPanel = new LibraryPanel();

        setLayout(new MigLayout("insets 15, fillx"));

        add(new JLabel("add some library options"), "wrap");
        add(libraryPanel, "wrap");
        add(playerPanel, "wrap");
        
        shareCompletedDownloadsCheckBox = new JCheckBox(I18n.tr("Share files downloaded from the P2P Network with the P2P Network"));
        shareCompletedDownloadsCheckBox.setContentAreaFilled(false);
        
        addToITunesCheckBox = new JCheckBox(I18n.tr("Add audio files I downloaded from LimeWire to iTunes"));
        addToITunesCheckBox.setContentAreaFilled(false);
        
        add(shareCompletedDownloadsCheckBox, "split 3, wrap");
        if(OSUtils.isMacOSX() || OSUtils.isWindows()) {
            add(addToITunesCheckBox, "split 3, wrap");
        }

    }

    @Override
    boolean applyOptions() {
        
        SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.setValue(shareCompletedDownloadsCheckBox.isSelected());
        SharingSettings.ALLOW_PARTIAL_SHARING.setValue(shareCompletedDownloadsCheckBox.isSelected());
        
        if(OSUtils.isMacOSX() || OSUtils.isWindows()) {
            iTunesSettings.ITUNES_SUPPORT_ENABLED.setValue(addToITunesCheckBox.isSelected());
        }
                
        return playerPanel.applyOptions() || libraryPanel.applyOptions();
    }

    @Override
    boolean hasChanged() {
        return playerPanel.hasChanged() || libraryPanel.hasChanged()
                    || SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue() != shareCompletedDownloadsCheckBox.isSelected()
                    || (OSUtils.isMacOSX() || OSUtils.isWindows()) ? iTunesSettings.ITUNES_SUPPORT_ENABLED.getValue() != addToITunesCheckBox.isSelected() : false;
    }

    @Override
    public void initOptions() {
        shareCompletedDownloadsCheckBox.setSelected(SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue());
        
        if(OSUtils.isMacOSX() || OSUtils.isWindows()) {
            addToITunesCheckBox.setSelected(iTunesSettings.ITUNES_SUPPORT_ENABLED.getValue());
        }

        libraryPanel.initOptions();
        playerPanel.initOptions();
    }

    /** Do you want to use the LW player? */
    private class LibraryPanel extends OptionPanel {

        private Map<Category, JCheckBox> categoryCheckboxes;

        public LibraryPanel() {
            super("");
            setBorder(BorderFactory.createEmptyBorder());
            setLayout(new MigLayout("ins 0 0 0 0, gap 0"));
            categoryCheckboxes = new HashMap<Category, JCheckBox>();
            
            for(Category category : Category.values()) {
                JCheckBox categoryCheckbox = new JCheckBox(category.getPluralName());
                categoryCheckboxes.put(category, categoryCheckbox);
                categoryCheckbox.setOpaque(false);
                add(categoryCheckbox);
            }
        }

        @Override
        boolean applyOptions() {
            libraryManager.getLibraryData().setCategoriesToIncludeWhenAddingFolders(getSelectedCategories());
            return false;
        }

        @Override
        boolean hasChanged() {
            Collection<Category> selectedCategories = getSelectedCategories();
            Collection<Category> managedCategories = libraryManager.getLibraryData().getManagedCategories();
            return selectedCategories.size() != managedCategories.size() || !selectedCategories.containsAll(managedCategories);
        }

        @Override
        public void initOptions() {
            Collection<Category> managedCategories = libraryManager.getLibraryData().getManagedCategories();
            for(Category category : categoryCheckboxes.keySet()) {
                JCheckBox categoryCheckbox = categoryCheckboxes.get(category);
                categoryCheckbox.setSelected(managedCategories.contains(category));
            }
        }
        
        private Collection<Category> getSelectedCategories() {
            Collection<Category> categories = new ArrayList<Category>();
            for(Category category : categoryCheckboxes.keySet()) {
                JCheckBox categoryCheckbox = categoryCheckboxes.get(category);
                if(categoryCheckbox.isSelected()) {
                    categories.add(category);
                }
            }
            return categories;
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
                    .tr("Use the LimeWire player when I play audio files"));
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

}
