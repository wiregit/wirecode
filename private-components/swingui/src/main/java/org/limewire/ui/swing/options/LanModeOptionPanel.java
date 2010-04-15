package org.limewire.ui.swing.options;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * LAN Mode view for Advanced Options.
 */
class LanModeOptionPanel extends OptionPanel {

    private MultiLineLabel lanModeLabel;
    private JCheckBox lanModeCheckBox;

    /**
     * Constructs an ExternalAccessOptionPanel.
     */
    @Inject
    public LanModeOptionPanel() {
        setLayout(new MigLayout("hidemode 3, insets 15, fillx, wrap"));
        setOpaque(false);
        add(getLanModePanel(), "pushx, growx");
    }

    /**
     * Returns the container for LAN mode options.
     */
    private JPanel getLanModePanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(""));
        panel.setLayout(new MigLayout("fillx"));
        panel.setOpaque(false);

        lanModeLabel = new MultiLineLabel();
        lanModeLabel.setMaxLineSpan(AdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH);
        lanModeLabel.setText(I18n.tr("In LAN mode, LimeWire will only connect to computers on your Local Area Network. You will not be able to access files from users outside your LAN, and they will not be able to access your files."));

        lanModeCheckBox = new JCheckBox(I18n.tr("Run in LAN mode"));
        lanModeCheckBox.setOpaque(false);

        panel.add(lanModeLabel, "growx, wrap");
        panel.add(lanModeCheckBox, "gapleft 25, wrap");

        return panel;
    }

    @Override
    ApplyOptionResult applyOptions() {
        ConnectionSettings.LAN_MODE.setValue(lanModeCheckBox.isSelected());
        return new ApplyOptionResult(false, true);
    }

    @Override
    boolean hasChanged() {
        return lanModeCheckBox.isSelected() != ConnectionSettings.LAN_MODE.getValue();
    }

    @Override
    public void initOptions() {
        lanModeCheckBox.setSelected(ConnectionSettings.LAN_MODE.getValue());
    }
}
