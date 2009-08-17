package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * BitTorrent Option View.
 */
public class BitTorrentOptionPanel extends OptionPanel {

    private ButtonGroup buttonGroup;

    private JRadioButton limewireControl;

    private JRadioButton myControl;

    private JLabel seedRatioLabel;

    private SeedRatioSlider seedRatio;

    private final Provider<TorrentManager> torrentManager;

    private final TorrentManagerSettings torrentSettings;

    @Inject
    public BitTorrentOptionPanel(Provider<TorrentManager> torrentManager,
            @TorrentSettingsAnnotation TorrentManagerSettings torrentSettings) {
        this.torrentManager = torrentManager;
        this.torrentSettings = torrentSettings;

        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        setOpaque(false);

        add(getBitTorrentPanel(), "pushx, growx");
    }

    private JPanel getBitTorrentPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout("gapy 10"));
        p.setOpaque(false);

        limewireControl = new JRadioButton(I18n
                .tr("Let LimeWire manage my BitTorrent settings (Recommended)"));
        limewireControl.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateState(limewireControl.isSelected());
            }
        });
        myControl = new JRadioButton(I18n.tr("Let me manage my BitTorrent settings"));
        myControl.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateState(limewireControl.isSelected());
            }
        });

        limewireControl.setOpaque(false);
        myControl.setOpaque(false);

        buttonGroup = new ButtonGroup();
        buttonGroup.add(limewireControl);
        buttonGroup.add(myControl);

        seedRatioLabel = new JLabel(I18n.tr("Seed Ratio"));

        seedRatio = new SeedRatioSlider();

        if (torrentManager.get().isValid()) {
            p.add(limewireControl, "wrap");
            p.add(myControl, "wrap");

            p.add(seedRatioLabel, "split");
            p.add(seedRatio, "alignx right, wrap");

        } else {
            // TODO updating text after we get the new error message from mike
            // s.
            p
                    .add(new MultiLineLabel(
                            I18n
                                    .tr("There was an error loading bittorrent. You will not be use bittorrent capabilities until this is resolved."),
                            500));
        }
        return p;
    }

    @Override
    boolean applyOptions() {
        SwingUiSettings.AUTOMATIC_SETTINGS.setValue(limewireControl.isSelected());
        if (limewireControl.isSelected()) {
            BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.setValue(SeedRatioSlider.DEAFULT_SLIDER);
        } else {
            BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.setValue(seedRatio.getValue()/(float)10);
        }

        if (torrentManager.get().isValid()) {
            torrentManager.get().setTorrentManagerSettings(torrentSettings);
        }
        return false;
    }

    @Override
    boolean hasChanged() {
        return SwingUiSettings.AUTOMATIC_SETTINGS.getValue() != limewireControl.isSelected()
                || seedRatio.getValue() != BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT
                        .getValue();
    }

    @Override
    public void initOptions() {
        boolean auto = SwingUiSettings.AUTOMATIC_SETTINGS.getValue();
        if (auto) {
            limewireControl.setSelected(true);
        } else {
            myControl.setSelected(true);
        }

        seedRatio.setValue((int) (BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getValue() * 10));

        updateState(auto);
    }

    /**
     * Updates the state of the components based on whether the user has opted
     * to control the bittorrent settings manually, or let limewire control
     * them.
     * 
     * @param limewireControlled if true then the user is not managing the
     *        settings, and the bandwidth controls should not be shown. If false
     *        the User has opted to manually set the bandwidth settings. The
     *        upload and download bandwidth controls should be enabled and set
     *        visible.
     */
    private void updateState(boolean limewireControlled) {
        seedRatioLabel.setVisible(!limewireControlled);
        seedRatio.setVisible(!limewireControlled);
        seedRatio.setEnabled(!limewireControlled);
    }

}
