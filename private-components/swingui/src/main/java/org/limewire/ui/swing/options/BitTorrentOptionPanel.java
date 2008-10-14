package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.BittorrentSettings;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * BitTorrent Option View
 */
@Singleton
public class BitTorrentOptionPanel extends OptionPanel {
    
    private static final int MIN = 1;
    private static final int MAX = 10;
    
    private ButtonGroup buttonGroup;
    
    private JRadioButton limewireControl;
    private JRadioButton myControl;
    
    private JSpinner maxUploadSpinner;
    private JSpinner minUploadSpinner;
    private JCheckBox safeChunkCheckBox;
    private JCheckBox experimentCheckBox;
    
    @Inject
    public BitTorrentOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getBitTorrentPanel(), "pushx, growx");
    }

    private JPanel getBitTorrentPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout("gapy 10"));
        
        limewireControl = new JRadioButton();
        limewireControl.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateState(!limewireControl.isSelected());
            }
        });
        myControl = new JRadioButton();
        
        buttonGroup = new ButtonGroup();
        buttonGroup.add(limewireControl);
        buttonGroup.add(myControl);
        
        maxUploadSpinner = new JSpinner(new SpinnerNumberModel(MIN, MIN, MAX, 1));
        minUploadSpinner = new JSpinner(new SpinnerNumberModel(MIN, MIN, MAX, 1));
        safeChunkCheckBox = new JCheckBox();
        experimentCheckBox = new JCheckBox();

        p.add(limewireControl);        
        p.add(new JLabel(I18n.tr("Let LimeWire manage my BitTorrent settings (Recommended)")), "wrap");
        
        p.add(myControl);        
        p.add(new JLabel(I18n.tr("Let me manage my BitTorrent settings")), "wrap");

        p.add(new JLabel(I18n.tr("Max uploads per torrent")), "skip 1, gapleft 20, split");
        p.add(maxUploadSpinner, "wrap");
        p.add(new JLabel(I18n.tr("Min uploads per torrent")), "skip 1, gapleft 20, split");
        p.add(minUploadSpinner, "wrap");
        p.add(safeChunkCheckBox, "skip 1, gapleft 20, split");
        p.add(new JLabel(I18n.tr("Safe chunk verification")), "wrap");
        p.add(experimentCheckBox, "skip 1, gapleft 20, split");
        p.add(new JLabel(I18n.tr("Experiemental disk access")));
        
        return p;
    }
    
    @Override
    void applyOptions() {
        BittorrentSettings.AUTOMATIC_SETTINGS.setValue(limewireControl.isSelected());
        BittorrentSettings.TORRENT_MAX_UPLOADS.setValue((Integer)maxUploadSpinner.getModel().getValue());
        BittorrentSettings.TORRENT_MIN_UPLOADS.setValue((Integer)minUploadSpinner.getModel().getValue());
        BittorrentSettings.TORRENT_FLUSH_VERIRY.setValue(safeChunkCheckBox.isSelected());
        BittorrentSettings.TORRENT_USE_MMAP.setValue(experimentCheckBox.isSelected());
    }
    
    @Override
    boolean hasChanged() {
        return BittorrentSettings.AUTOMATIC_SETTINGS.getValue() != limewireControl.isSelected() 
                || (Integer)maxUploadSpinner.getModel().getValue() != BittorrentSettings.TORRENT_MAX_UPLOADS.getValue()
                || (Integer)minUploadSpinner.getModel().getValue()!= BittorrentSettings.TORRENT_MIN_UPLOADS.getValue()
                || BittorrentSettings.TORRENT_FLUSH_VERIRY.getValue() != safeChunkCheckBox.isSelected()
                || BittorrentSettings.TORRENT_USE_MMAP.getValue() != experimentCheckBox.isSelected();
    }
    
    @Override
    public void initOptions() {
        boolean auto = BittorrentSettings.AUTOMATIC_SETTINGS.getValue();
        if(auto)
            limewireControl.setSelected(true);
        else
            myControl.setSelected(true);
        
        maxUploadSpinner.getModel().setValue(BittorrentSettings.TORRENT_MAX_UPLOADS.getValue());
        minUploadSpinner.getModel().setValue(BittorrentSettings.TORRENT_MIN_UPLOADS.getValue());
        safeChunkCheckBox.setSelected(BittorrentSettings.TORRENT_FLUSH_VERIRY.getValue());
        experimentCheckBox.setSelected(BittorrentSettings.TORRENT_USE_MMAP.getValue());
        
        updateState(!auto);
    }
    
    private void updateState(boolean value) {
        maxUploadSpinner.setEnabled(value);
        minUploadSpinner.setEnabled(value);
        safeChunkCheckBox.setEnabled(value);
        experimentCheckBox.setEnabled(value);
    }
}
