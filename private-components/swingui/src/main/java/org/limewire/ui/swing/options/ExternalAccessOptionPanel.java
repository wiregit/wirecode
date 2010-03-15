package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.rest.RestUtils;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * External Access view for Advanced Options.
 */
class ExternalAccessOptionPanel extends OptionPanel {

    private MultiLineLabel localAccessLabel;
    private JCheckBox localAccessCheckBox;
    private JLabel accessSecretLabel;
    private JTextField accessSecretField;

    /**
     * Constructs an ExternalAccessOptionPanel.
     */
    @Inject
    public ExternalAccessOptionPanel() {
        setLayout(new MigLayout("hidemode 3, insets 15, fillx, wrap"));
        setOpaque(false);
        
        add(getAccessPanel(), "pushx, growx");
    }
    
    /**
     * Returns the container for access options.
     */
    private JPanel getAccessPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(""));
        panel.setLayout(new MigLayout("fillx"));
        panel.setOpaque(false);
        
        localAccessLabel = new MultiLineLabel();
        localAccessLabel.setMaxLineSpan(AdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH);
        localAccessLabel.setText(I18n.tr("LimeWire can allow other processes to make use of its services, like search and download, via a REST interface."));
        
        localAccessCheckBox = new JCheckBox(I18n.tr("Enable local access to REST service"));
        localAccessCheckBox.setOpaque(false);
        localAccessCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                localAccessStateChanged();
            }
        });
        
        accessSecretLabel = new JLabel(I18n.tr("Access Code:"));
        
        accessSecretField = new JTextField();
        accessSecretField.setColumns(32);
        accessSecretField.setEditable(false);
        
        panel.add(localAccessLabel, "growx, wrap");
        panel.add(localAccessCheckBox, "gapleft 25, wrap");
        panel.add(accessSecretLabel, "gapleft 25, split");
        panel.add(accessSecretField, "split, wrap");
        
        return panel;
    }
    
    @Override
    boolean applyOptions() {
        ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.setValue(localAccessCheckBox.isSelected());
        return false;
    }

    @Override
    boolean hasChanged() {
        return localAccessCheckBox.isSelected() != ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.getValue();
    }

    @Override
    public void initOptions() {
        localAccessCheckBox.setSelected(ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.getValue());
        accessSecretField.setText(RestUtils.getAccessSecret());
        localAccessStateChanged();
    }

    /**
     * Handles event when local access state is changed.
     */
    private void localAccessStateChanged() {
        accessSecretLabel.setVisible(localAccessCheckBox.isSelected());
        accessSecretField.setVisible(localAccessCheckBox.isSelected());
    }
}
