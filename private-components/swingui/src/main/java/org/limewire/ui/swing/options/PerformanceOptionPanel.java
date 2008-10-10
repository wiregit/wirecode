package org.limewire.ui.swing.options;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.DHTSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Performance Option View
 */
@Singleton
public class PerformanceOptionPanel extends OptionPanel {

    private final String firstMultiLineLabel = I18n.tr("If your computer has a fast internet connection, LimeWire may act as an \"Ultrapeer\" or connect to the Mojito DHT. You may disable these if you notice performacnce issues.");
    private final String secondMultiLineLabel = I18n.tr("LimeWire uses a secure communications mode called TLS, which may use more CPU resources.");
    private final String thirdMultiLineLabel = I18n.tr("Out-of-band Searching helps deliver faster search results to you, but some internet connections may not work well with this feature.");
    
    private JCheckBox disableUltraPeerCheckBox;
    private JCheckBox disableMojitoCheckBox;
    private JCheckBox disableTLS;
    private JCheckBox disableOutOfBandSearchCheckBox;
    
    @Inject
    public PerformanceOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getPerformancePanel(), "pushx, growx");
    }
    
    private JPanel getPerformancePanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout());
        
        disableUltraPeerCheckBox = new JCheckBox();
        disableMojitoCheckBox = new JCheckBox();
        disableTLS = new JCheckBox();
        disableOutOfBandSearchCheckBox = new JCheckBox();

        p.add(new MultiLineLabel(firstMultiLineLabel, ReallyAdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH), "wrap");
        
        p.add(disableUltraPeerCheckBox, "gapleft 25, split");
        p.add(new JLabel(I18n.tr("Disable Ultrapeer capabilities")), "wrap");
        
        p.add(disableMojitoCheckBox, "gapleft 25, split");
        p.add(new JLabel(I18n.tr("Disable connecting to the Mojito DHT")), "wrap");
        
        p.add(new MultiLineLabel(secondMultiLineLabel, ReallyAdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH), "gaptop 18, wrap");
        
        p.add(disableTLS, "gapleft 25, split");
        p.add(new JLabel(I18n.tr("Disable TLS capabilities")), "wrap");
        
        p.add(new MultiLineLabel(thirdMultiLineLabel, ReallyAdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH), "gaptop 18, wrap");
        
        p.add(disableOutOfBandSearchCheckBox, "gapleft 25, split");
        p.add(new JLabel(I18n.tr("Disable Out-of-band searching")), "wrap");
        
        return p;
    }
    
    @Override
    void applyOptions() {
        // TODO finish them
        SearchSettings.OOB_ENABLED.setValue(disableOutOfBandSearchCheckBox.isSelected());
    }

    @Override
    boolean hasChanged() {
        return UltrapeerSettings.DISABLE_ULTRAPEER_MODE.getValue() != disableUltraPeerCheckBox.isSelected() 
        || DHTSettings.DISABLE_DHT_USER.getValue() != disableMojitoCheckBox.isSelected()
        //TODO: NetworkManager API
//        || (!GuiCoreMediator.getNetworkManager().isIncomingTLSEnabled() &&
//            !GuiCoreMediator.getNetworkManager().isOutgoingTLSEnabled())
//               != TLS_CHECK_BOX.isSelected();
        
        || SearchSettings.OOB_ENABLED.getValue() != disableOutOfBandSearchCheckBox.isSelected();
    }

    @Override
    void initOptions() {
        disableUltraPeerCheckBox.setSelected(UltrapeerSettings.DISABLE_ULTRAPEER_MODE.getValue());
        disableMojitoCheckBox.setSelected(DHTSettings.DISABLE_DHT_USER.getValue());
        //TODO: Network manager API
//        TLS_CHECK_BOX.setSelected(!GuiCoreMediator.getNetworkManager().isIncomingTLSEnabled() ||
//                !GuiCoreMediator.getNetworkManager().isOutgoingTLSEnabled());
        
        disableOutOfBandSearchCheckBox.setSelected(SearchSettings.OOB_ENABLED.getValue());
    }

}