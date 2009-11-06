package org.limewire.ui.swing.options;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;

import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * BitTorrent Option View.
 */
public class BitTorrentOptionPanel extends OptionPanel {

    private final Provider<TorrentManager> torrentManager;
    private final TorrentManagerSettings torrentSettings;
    
    private final JRadioButton uploadTorrentsForeverButton;
    private final JRadioButton uploadTorrentsControlButton;
    private final SpinnerNumberModel seedRatioModel;
    private final JLabel seedRatioLabel;
    private final JSpinner seedRatioSpinner;
    private final SpinnerNumberModel seedTimeModel;
    private final JLabel seedTimeLabel;
    private final JSpinner seedTimeSpinner;
    private final JCheckBox chooseTorrentsCheckBox;

    @Inject
    public BitTorrentOptionPanel(Provider<TorrentManager> torrentManager,
            @TorrentSettingsAnnotation TorrentManagerSettings torrentSettings) {
        this.torrentManager = torrentManager;
        this.torrentSettings = torrentSettings;
        
        setLayout(new MigLayout("fill"));
        setOpaque(false);
        uploadTorrentsForeverButton = new JRadioButton(I18n.tr("Upload torrents forever"));
        uploadTorrentsForeverButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateState(uploadTorrentsForeverButton.isSelected());
            }
        });
        uploadTorrentsControlButton = new JRadioButton(I18n.tr("Upload torrents until either of the following:"));
        uploadTorrentsControlButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateState(uploadTorrentsForeverButton.isSelected());
            }
        });

        uploadTorrentsForeverButton.setOpaque(false);
        uploadTorrentsControlButton.setOpaque(false);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(uploadTorrentsForeverButton);
        buttonGroup.add(uploadTorrentsControlButton);

        seedRatioModel = new SpinnerNumberModel(BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT
                .get().doubleValue(), BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getMinValue()
                .doubleValue(), BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getMaxValue()
                .doubleValue(), .05);

        seedRatioSpinner = new JSpinner(seedRatioModel);
        seedRatioSpinner.setPreferredSize(new Dimension(50, 20));
        seedRatioSpinner.setMaximumSize(new Dimension(60, 20));

        seedTimeModel = new SpinnerNumberModel(
                getDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.get()),
                getDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getMinValue()),
                getDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getMaxValue()), .05);

        seedTimeSpinner = new JSpinner(seedTimeModel);
        seedTimeSpinner.setPreferredSize(new Dimension(50, 20));
        seedTimeSpinner.setMaximumSize(new Dimension(60, 20));

        seedRatioLabel = new JLabel(I18n.tr("Ratio:"));
        seedTimeLabel = new JLabel(I18n.tr("Maximum days:"));
        chooseTorrentsCheckBox = new JCheckBox(I18n.tr("Let me choose files to download when starting a torrent"));
        chooseTorrentsCheckBox.setOpaque(false);

        if (torrentManager.get().isValid()) {
            add(uploadTorrentsForeverButton, "span 3, wrap");
            add(uploadTorrentsControlButton, "span 3, wrap");
            add(seedRatioLabel, "gapleft 20");
            add(seedRatioSpinner, "span, wrap");
            add(seedTimeLabel, "gapleft 20");
            add(seedTimeSpinner, "span, wrap");
            add(chooseTorrentsCheckBox, "span, gaptop 10, gapbottom 5, wrap");
        } else {
            add(new MultiLineLabel(I18n.tr("There was an error loading bittorrent. You will not be able to use bittorrent capabilities until this is resolved."),
                    500), "wrap");
        }

        add(new JButton(new OKDialogAction()), "span, tag ok, alignx right, split 2");
        add(new JButton(new CancelDialogAction()), "tag cancel");
    }

    @Override
    boolean applyOptions() {
        BittorrentSettings.UPLOAD_TORRENTS_FOREVER.setValue(uploadTorrentsForeverButton.isSelected());
        if (!uploadTorrentsForeverButton.isSelected()) {
            BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.setValue(seedRatioModel.getNumber().floatValue());
            BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.setValue(((Double) Math.ceil(((Double) seedTimeSpinner.getValue()).doubleValue() * 60 * 60 * 24)).intValue());
        }

        BittorrentSettings.TORRENT_SHOW_POPUP_BEFORE_DOWNLOADING.setValue(chooseTorrentsCheckBox
                .isSelected());

        if (torrentManager.get().isValid()) {
            torrentManager.get().setTorrentManagerSettings(torrentSettings);
        }
        return false;
    }

    @Override
    boolean hasChanged() {
        return BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue() != uploadTorrentsForeverButton.isSelected()
                || ((Float) seedRatioSpinner.getValue()).floatValue() != BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getValue()
                || ((Double) Math.ceil(((Double) seedTimeSpinner.getValue()).doubleValue() * 60 * 60 * 24))
                        .intValue() != BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getValue()
                || chooseTorrentsCheckBox.isSelected() != BittorrentSettings.TORRENT_SHOW_POPUP_BEFORE_DOWNLOADING.getValue();
    }

    @Override
    public void initOptions() {
        boolean auto = BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue();
        if (auto) {
            uploadTorrentsForeverButton.setSelected(true);
        } else {
            uploadTorrentsControlButton.setSelected(true);
        }

        seedRatioSpinner.setValue(BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.get().doubleValue());
        seedTimeSpinner.setValue(getDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.get()));
        chooseTorrentsCheckBox.setSelected(BittorrentSettings.TORRENT_SHOW_POPUP_BEFORE_DOWNLOADING.getValue());
    }

    private double getDays(Integer integer) {
        return integer.doubleValue() / (60 * 60 * 24);
    }

    /**
     * Updates the state of the components based on whether the user has opted
     * to control the bittorrent settings manually, or let limewire control
     * them.
     */
    private void updateState(boolean uploadForever) {
        seedRatioLabel.setEnabled(!uploadForever);
        seedRatioSpinner.setEnabled(!uploadForever);
        seedTimeLabel.setEnabled(!uploadForever);
        seedTimeSpinner.setEnabled(!uploadForever);
    }

}
