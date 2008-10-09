package org.limewire.ui.swing.options;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.util.I18n;

/**
 * Connections Option View
 */
public class ConnectionsOptionPanel extends OptionPanel {

    private ConnectionSpeedPanel connectionSpeedPanel;
    private DownloadsPanel downloadsPanel;
    private UploadsPanel uploadPanel;
    
    public ConnectionsOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getConnectionSpeedPanel(), "pushx, growx");
        add(getDownloadsPanel(), "pushx, growx");
        add(getUploadPanel(), "pushx, growx");
    }
    
    private OptionPanel getConnectionSpeedPanel() {
        if(connectionSpeedPanel == null) {
            connectionSpeedPanel = new ConnectionSpeedPanel();
        }
        return connectionSpeedPanel;
    }
    
    private OptionPanel getDownloadsPanel() {
        if(downloadsPanel == null) {
            downloadsPanel = new DownloadsPanel();
        }
        return downloadsPanel;
    }
    
    private OptionPanel getUploadPanel() {
        if(uploadPanel == null) {
            uploadPanel = new UploadsPanel();
        }
        return uploadPanel;
    }

    @Override
    void applyOptions() {
        getConnectionSpeedPanel().applyOptions();
        getDownloadsPanel().applyOptions();
        getUploadPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getConnectionSpeedPanel().hasChanged() ||
                getDownloadsPanel().hasChanged() ||
                getUploadPanel().hasChanged();
    }

    @Override
    void initOptions() {
        getConnectionSpeedPanel().initOptions();
        getDownloadsPanel().initOptions();
        getUploadPanel().initOptions();
    }
    
    private class ConnectionSpeedPanel extends OptionPanel {

        private ButtonGroup buttonGroup;
        
        private JRadioButton broadBandButton;
        private JRadioButton dialupButton;
        
        public ConnectionSpeedPanel() {
            super(I18n.tr("Connection Speed"));
            
            buttonGroup = new ButtonGroup();
            
            broadBandButton = new JRadioButton();
            dialupButton = new JRadioButton();
            
            buttonGroup.add(broadBandButton);
            buttonGroup.add(dialupButton);
            
            add(new JLabel("Set your connection speed"), "push");
            add(broadBandButton);
            add(new JLabel("Broadband"),"wrap");

            add(dialupButton, "skip 1");
            add(new JLabel("Dial-up"));
        }
        
        @Override
        void applyOptions() {
        }

        @Override
        boolean hasChanged() {
            return false;
        }

        @Override
        void initOptions() {
            ConnectionSettings.CONNECTION_SPEED.getValue();
        }

    }
    
    private class DownloadsPanel extends OptionPanel {

        private JSpinner maxDownloadSpinner;
        private JCheckBox limitBandWidthCheckBox;
        private JSlider bandWidthSlider;
        
        public DownloadsPanel() {
            super(I18n.tr("Downloads"));
            
            maxDownloadSpinner = new JSpinner();
            limitBandWidthCheckBox = new JCheckBox();
            bandWidthSlider = new JSlider();
            
            add(new JLabel("Don't allow more than"), "split");
            add(maxDownloadSpinner, "split");
            add(new JLabel("downloads at once"), "wrap");
            
            add(limitBandWidthCheckBox, "split");
            add(new JLabel("Limit your download bandwidth"));
            add(bandWidthSlider);
        }
        
        @Override
        void applyOptions() {
        }

        @Override
        boolean hasChanged() {
            return false;
        }

        @Override
        void initOptions() {
        }
    }
    
    private class UploadsPanel extends OptionPanel {

        private JSpinner maxUploadSpinner;
        private JCheckBox uploadBandwidthCheckBox;
        private JCheckBox clearUploadCheckBox;
        
        public UploadsPanel() {
            super(I18n.tr("Uploads"));
            
            maxUploadSpinner = new JSpinner();
            uploadBandwidthCheckBox = new JCheckBox();
            clearUploadCheckBox = new JCheckBox();
            
            add(new JLabel("Don't allow more than"), "split");
            add(maxUploadSpinner, "split");
            add(new JLabel("uploads at once"), "wrap");
            
            add(uploadBandwidthCheckBox, "split");
            add(new JLabel("Limit your upload bandwidth"), "wrap");
            
            add(clearUploadCheckBox, "split");
            add(new JLabel("Clear uploads from list when finished"));
        }
        
        @Override
        void applyOptions() {
            if(SharingSettings.CLEAR_UPLOAD.getValue() != clearUploadCheckBox.isSelected()) {
                SharingSettings.CLEAR_UPLOAD.setValue(clearUploadCheckBox.isSelected());
            }
        }

        @Override
        boolean hasChanged() {
            return SharingSettings.CLEAR_UPLOAD.getValue() != clearUploadCheckBox.isSelected();
        }

        @Override
        void initOptions() {
            clearUploadCheckBox.setSelected(SharingSettings.CLEAR_UPLOAD.getValue());
        }
    }
}
