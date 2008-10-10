package org.limewire.ui.swing.options;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.util.I18n;

public class UnsafeTypeOptionPanel extends OptionPanel {

    private JCheckBox programCheckBox;
    private JCheckBox documentCheckBox;
    private JButton okButton;
    
    public UnsafeTypeOptionPanel(Action okButtonAction) {
        setLayout(new MigLayout("gapy 10"));
        
        programCheckBox = new JCheckBox();
        documentCheckBox = new JCheckBox();
        okButton = new JButton(okButtonAction);
        
        add(new JLabel(I18n.tr("Enabling these settings makes you more prone to viruses and accidently sharing private documents")), "span 2, wrap");
        
        add(programCheckBox, "split, gapleft 25");
        add(new JLabel(I18n.tr("Allow me to search for and share Programs with the LimeWire Network and my friends")),"wrap");
        add(documentCheckBox, "split, gapbottom 15, gapleft 25");
        add(new JLabel(I18n.tr("Allow me to search for and share Documents with the LimeWire Network")), "gapbottom 15, wrap");
        
        add(new JLabel(I18n.tr("By default, LimeWire allows you to share documents with your friends")), "push");
        add(okButton);
    }
    
    @Override
    void applyOptions() {
        SharingSettings.PROGRAM_SHARING_ENABLED.setValue(programCheckBox.isSelected());
        SharingSettings.DOCUMENT_SHARING_ENABLED.setValue(documentCheckBox.isSelected());
    }

    @Override
    boolean hasChanged() {
        return SharingSettings.PROGRAM_SHARING_ENABLED.getValue() != programCheckBox.isSelected() 
                || SharingSettings.DOCUMENT_SHARING_ENABLED.getValue() != documentCheckBox.isSelected();
    }

    @Override
    void initOptions() {
        programCheckBox.setSelected(SharingSettings.PROGRAM_SHARING_ENABLED.getValue());
        documentCheckBox.setSelected(SharingSettings.DOCUMENT_SHARING_ENABLED.getValue());
    }
}
