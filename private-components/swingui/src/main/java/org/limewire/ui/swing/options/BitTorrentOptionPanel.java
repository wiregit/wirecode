package org.limewire.ui.swing.options;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

    private final ButtonGroup buttonGroup;
    private final JRadioButton uploadForever;
    private final JRadioButton myControl;
    private final SpinnerNumberModel seedRatioModel;
    private final JSpinner seedRatio;
    private final SpinnerNumberModel seedTimeModel;
    private final JSpinner seedTime;
    private final Provider<TorrentManager> torrentManager;
    private final TorrentManagerSettings torrentSettings;
    private final JCheckBox prioritizeTorrentPopup;
    private final JPanel seedController;

    @Inject
    public BitTorrentOptionPanel(Provider<TorrentManager> torrentManager,
            @TorrentSettingsAnnotation TorrentManagerSettings torrentSettings) {
        this.torrentManager = torrentManager;
        this.torrentSettings = torrentSettings;
        setLayout(new MigLayout("nogrid, fill"));
        setOpaque(false);
        uploadForever = new JRadioButton("<html>" + I18n.tr("Upload torrents forever") + "</html>");
        uploadForever.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateState(uploadForever.isSelected());
            }
        });
        myControl = new JRadioButton(I18n.tr("Upload torrents until either of the following:"));
        myControl.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateState(uploadForever.isSelected());
            }
        });

        uploadForever.setOpaque(false);
        myControl.setOpaque(false);

        buttonGroup = new ButtonGroup();
        buttonGroup.add(uploadForever);
        buttonGroup.add(myControl);

        seedRatioModel = new SpinnerNumberModel(BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT
                .get().doubleValue(), BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getMinValue()
                .doubleValue(), BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getMaxValue()
                .doubleValue(), .05);

        seedRatio = new JSpinner(seedRatioModel);
        seedRatio.setPreferredSize(new Dimension(50, 20));
        seedRatio.setMaximumSize(new Dimension(60, 20));

        seedTimeModel = new SpinnerNumberModel(
                getDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.get()),
                getDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getMinValue()),
                getDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getMaxValue()), .05);

        seedTime = new JSpinner(seedTimeModel);
        seedTime.setPreferredSize(new Dimension(50, 20));
        seedTime.setMaximumSize(new Dimension(60, 20));

        seedController = new JPanel(new MigLayout("insets 0, gap 0, nogrid"));
        seedController.setOpaque(false);
        seedController.add(new JLabel(I18n.tr("Ratio:")));
        seedController.add(seedRatio, "wrap");
        seedController.add(new JLabel(I18n.tr("Maximum days:")));
        seedController.add(seedTime, "wrap");

        prioritizeTorrentPopup = new JCheckBox(I18n
                .tr("Let me choose files to download when starting a torrent"));
        prioritizeTorrentPopup.setOpaque(false);

        if (torrentManager.get().isValid()) {
            add(prioritizeTorrentPopup, "wrap");
            add(uploadForever, "wrap, gaptop 20");
            add(myControl, "wrap");
            add(seedController, "gapleft 20, wrap");
        } else {
            add(new MultiLineLabel(
                    I18n
                            .tr("There was an error loading bittorrent. You will not be able to use bittorrent capabilities until this is resolved."),
                    500), "wrap");
        }

        add(new JButton(new OKDialogAction()), "tag ok, alignx right, split 2");
        add(new JButton(new CancelDialogAction()), "tag cancel");
    }

    @Override
    boolean applyOptions() {
        BittorrentSettings.UPLOAD_TORRENTS_FOREVER.setValue(uploadForever.isSelected());
        if (!uploadForever.isSelected()) {
            BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.setValue(seedRatioModel.getNumber()
                    .floatValue());
            BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.setValue(((Double) Math
                    .ceil(((Double) seedTime.getValue()).doubleValue() * 60 * 60 * 24)).intValue());
        }

        BittorrentSettings.TORRENT_SHOW_POPUP_BEFORE_DOWNLOADING.setValue(prioritizeTorrentPopup
                .isSelected());

        if (torrentManager.get().isValid()) {
            torrentManager.get().setTorrentManagerSettings(torrentSettings);
        }
        return false;
    }

    @Override
    boolean hasChanged() {
        return BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue() != uploadForever.isSelected()
                || ((Float) seedRatio.getValue()).floatValue() != BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT
                        .getValue()
                || ((Double) Math.ceil(((Double) seedTime.getValue()).doubleValue() * 60 * 60 * 24))
                        .intValue() != BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getValue()
                || prioritizeTorrentPopup.isSelected() != BittorrentSettings.TORRENT_SHOW_POPUP_BEFORE_DOWNLOADING
                        .getValue();
    }

    @Override
    public void initOptions() {
        boolean auto = BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue();
        if (auto) {
            uploadForever.setSelected(true);
        } else {
            myControl.setSelected(true);
        }

        seedRatio.setValue(BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.get().doubleValue());
        seedTime.setValue(getDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.get()));
        prioritizeTorrentPopup.setSelected(BittorrentSettings.TORRENT_SHOW_POPUP_BEFORE_DOWNLOADING
                .getValue());

        updateState(auto);
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
        seedController.setVisible(!uploadForever);
    }

}
