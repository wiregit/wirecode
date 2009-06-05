package org.limewire.ui.swing.options;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/** Library Option View */
public class LibraryOptionPanel extends OptionPanel {

    private final UsePlayerPanel playerPanel;
    
    @Inject
    public LibraryOptionPanel() {
        this.playerPanel = new UsePlayerPanel();
        
        setLayout(new MigLayout("insets 15, fillx, wrap", "", ""));
        
        add(new JLabel("add some library options"), "pushx, growx");
        add(playerPanel, "pushx, growx");
    }

    @Override
    boolean applyOptions() {
          return     playerPanel.applyOptions();
    }

    @Override
    boolean hasChanged() {
          return     playerPanel.hasChanged();
    }

    @Override
    public void initOptions() {
        playerPanel.initOptions();
    }    
    
    
    /** Do you want to use the LW player? */
    private class UsePlayerPanel extends OptionPanel {

        private JCheckBox useLimeWirePlayer;

        public UsePlayerPanel() {
            super("");
            setBorder(BorderFactory.createEmptyBorder());
            setLayout(new MigLayout("ins 0 0 0 0, gap 0! 0!, fill"));

            useLimeWirePlayer = new JCheckBox(I18n.tr("Use the LimeWire player when I play audio files"));
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
