package org.limewire.ui.swing.options;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/** Library Option View */
public class LibraryOptionPanel extends OptionPanel {

    private final UsePlayerPanel playerPanel;

    private final LibraryPanel libraryPanel;
    
    private final LibraryManager libraryManager;

    @Inject
    public LibraryOptionPanel(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
        this.playerPanel = new UsePlayerPanel();
        this.libraryPanel = new LibraryPanel();

        setLayout(new MigLayout("insets 15, fillx, wrap", "", ""));

        add(new JLabel("add some library options"), "pushx, growx");
        add(libraryPanel, "pushx, growx");
        add(playerPanel, "pushx, growx");
    }

    @Override
    boolean applyOptions() {
        return playerPanel.applyOptions() || libraryPanel.applyOptions();
    }

    @Override
    boolean hasChanged() {
        return playerPanel.hasChanged() || libraryPanel.hasChanged();
    }

    @Override
    public void initOptions() {
        libraryPanel.initOptions();
        playerPanel.initOptions();
    }

    /** Do you want to use the LW player? */
    private class LibraryPanel extends OptionPanel {

        private JCheckBox audioCheckbox;

        private JCheckBox videoCheckbox;

        private JCheckBox imagesCheckbox;

        private JCheckBox programsCheckbox;

        private JCheckBox documentsCheckbox;

        private JCheckBox otherCheckbox;

        public LibraryPanel() {
            super("");
            setBorder(BorderFactory.createEmptyBorder());
            setLayout(new MigLayout("ins 0 0 0 0, gap 0! 0!, fill"));

            audioCheckbox = new JCheckBox(I18n.tr("Audio"));
            audioCheckbox.setOpaque(false);

            videoCheckbox = new JCheckBox(I18n.tr("Video"));
            videoCheckbox.setOpaque(false);

            imagesCheckbox = new JCheckBox(I18n.tr("Images"));
            imagesCheckbox.setOpaque(false);

            programsCheckbox = new JCheckBox(I18n.tr("Programs"));
            programsCheckbox.setOpaque(false);

            documentsCheckbox = new JCheckBox(I18n.tr("Documents"));
            documentsCheckbox.setOpaque(false);

            otherCheckbox = new JCheckBox(I18n.tr("Other"));
            otherCheckbox.setOpaque(false);

            add(audioCheckbox);
            add(videoCheckbox);
            add(imagesCheckbox);
            add(programsCheckbox);
            add(documentsCheckbox);
            add(otherCheckbox);
        }

        @Override
        boolean applyOptions() {
            if(hasChanged()) {
                LibrarySettings.MANAGE_AUDIO.set(audioCheckbox.isSelected());
                LibrarySettings.MANAGE_VIDEO.set(videoCheckbox.isSelected());
                LibrarySettings.MANAGE_IMAGES.set(imagesCheckbox.isSelected());
                LibrarySettings.MANAGE_PROGRAMS.set(programsCheckbox.isSelected());
                LibrarySettings.MANAGE_DOCUMENTS.set(documentsCheckbox.isSelected());
                LibrarySettings.MANAGE_OTHER.set(otherCheckbox.isSelected());
                
                //libraryData.setCategoriesToIncludeWhenAddingFolders(managedCategories)
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            return LibrarySettings.MANAGE_AUDIO.getValue() != audioCheckbox.isSelected()
                    || LibrarySettings.MANAGE_VIDEO.getValue() != videoCheckbox.isSelected()
                    || LibrarySettings.MANAGE_IMAGES.getValue() != imagesCheckbox.isSelected()
                    || LibrarySettings.MANAGE_DOCUMENTS.getValue() != documentsCheckbox
                            .isSelected()
                    || LibrarySettings.MANAGE_PROGRAMS.getValue() != programsCheckbox.isSelected()
                    || LibrarySettings.MANAGE_OTHER.getValue() != otherCheckbox.isSelected();
        }

        @Override
        public void initOptions() {
            audioCheckbox.setSelected(LibrarySettings.MANAGE_AUDIO.getValue());
            videoCheckbox.setSelected(LibrarySettings.MANAGE_VIDEO.getValue());
            imagesCheckbox.setSelected(LibrarySettings.MANAGE_IMAGES.getValue());
            programsCheckbox.setSelected(LibrarySettings.MANAGE_PROGRAMS.getValue());
            documentsCheckbox.setSelected(LibrarySettings.MANAGE_DOCUMENTS.getValue());
            otherCheckbox.setSelected(LibrarySettings.MANAGE_OTHER.getValue());
        }
    }

    /** Do you want to use the LW player? */
    private class UsePlayerPanel extends OptionPanel {

        private JCheckBox useLimeWirePlayer;

        public UsePlayerPanel() {
            super("");
            setBorder(BorderFactory.createEmptyBorder());
            setLayout(new MigLayout("ins 0 0 0 0, gap 0! 0!, fill"));

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
