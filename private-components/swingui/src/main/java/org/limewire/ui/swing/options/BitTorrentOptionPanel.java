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
import org.limewire.bittorrent.TorrentSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * BitTorrent Option View
 */
public class BitTorrentOptionPanel extends OptionPanel {

    private ButtonGroup buttonGroup;

    private JRadioButton limewireControl;

    private JRadioButton myControl;

    private JLabel uploadBandWidthLabel;

    private BandWidthSlider uploadBandWidth;

    private JLabel downloadBandWidthLabel;

    private BandWidthSlider downloadBandWidth;

    private final Provider<TorrentManager> torrentManager;

    private final TorrentSettings torrentSettings;

    @Inject
    public BitTorrentOptionPanel(Provider<TorrentManager> torrentManager,
            @TorrentSettingsAnnotation TorrentSettings torrentSettings) {
        this.torrentManager = torrentManager;
        this.torrentSettings = torrentSettings;
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));

        add(getBitTorrentPanel(), "pushx, growx");
    }

    private JPanel getBitTorrentPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout("gapy 10"));

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

        buttonGroup = new ButtonGroup();
        buttonGroup.add(limewireControl);
        buttonGroup.add(myControl);

        p.add(limewireControl, "wrap");
        p.add(myControl, "wrap");

        downloadBandWidthLabel = new JLabel(I18n.tr("Download bandwidth"));
        uploadBandWidthLabel = new JLabel(I18n.tr("Upload bandwidth"));

        uploadBandWidth = new BandWidthSlider();
        downloadBandWidth = new BandWidthSlider();

        p.add(downloadBandWidthLabel, "split");
        p.add(downloadBandWidth, "alignx right, wrap");
        p.add(uploadBandWidthLabel, "split");
        p.add(uploadBandWidth, "alignx right, wrap");

        return p;
    }

    @Override
    boolean applyOptions() {
        SwingUiSettings.AUTOMATIC_SETTINGS.setValue(limewireControl.isSelected());
        if (limewireControl.isSelected()) {
            BittorrentSettings.LIBTORRENT_UPLOAD_SPEED.setValue(BandWidthSlider.MAX_SLIDER);
            BittorrentSettings.LIBTORRENT_DOWNLOAD_SPEED.setValue(BandWidthSlider.MAX_SLIDER);
        } else {
            BittorrentSettings.LIBTORRENT_UPLOAD_SPEED.setValue(uploadBandWidth.getValue());
            BittorrentSettings.LIBTORRENT_DOWNLOAD_SPEED.setValue(downloadBandWidth.getValue());
        }
        // TODO this a little weird since we are jsut using the fact that the
        // inject settings will be updated automatically by updating the
        // BittorentSettings values.
        torrentManager.get().updateSettings(torrentSettings);
        return false;
    }

    @Override
    boolean hasChanged() {
        return SwingUiSettings.AUTOMATIC_SETTINGS.getValue() != limewireControl.isSelected()
                || uploadBandWidth.getValue() != BittorrentSettings.LIBTORRENT_UPLOAD_SPEED
                        .getValue()
                || downloadBandWidth.getValue() != BittorrentSettings.LIBTORRENT_DOWNLOAD_SPEED
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

        uploadBandWidth.setValue(BittorrentSettings.LIBTORRENT_UPLOAD_SPEED.getValue());
        downloadBandWidth.setValue(BittorrentSettings.LIBTORRENT_DOWNLOAD_SPEED.getValue());

        updateState(auto);
    }

    private void updateState(boolean limeWireControl) {
        uploadBandWidthLabel.setVisible(!limeWireControl);
        uploadBandWidth.setVisible(!limeWireControl);
        uploadBandWidth.setEnabled(!limeWireControl);
        downloadBandWidthLabel.setVisible(!limeWireControl);
        downloadBandWidth.setVisible(!limeWireControl);
        downloadBandWidth.setEnabled(!limeWireControl);
    }

}
