package org.limewire.ui.swing.options;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.util.I18n;

public class UnsafeTypeOptionPanel extends OptionPanel {

    private JCheckBox programCheckBox;
    private JCheckBox documentCheckBox;
    private JButton okButton;
    
    public UnsafeTypeOptionPanel(Action okButtonAction) {
        setLayout(new MigLayout("gapy 10"));
        
        programCheckBox = new JCheckBox(I18n.tr("Allow me to search for and share Programs with the P2P Network and my friends"));
        documentCheckBox = new JCheckBox(I18n.tr("Allow me to search for and share Documents with the P2P Network"));
        okButton = new JButton(okButtonAction);
        
        add(new JLabel(I18n.tr("Enabling these settings makes you more prone to viruses and accidently sharing private documents:")), "span 2, wrap");
        
        add(programCheckBox, "split, gapleft 25, wrap");
        add(documentCheckBox, "split, gapbottom 15, gapleft 25, wrap");
        
        add(new JLabel(I18n.tr("By default, LimeWire allows you to share documents with your friends")), "push");
        add(okButton);
    }
    
    @Override
    void applyOptions() {
        LibrarySettings.ALLOW_PROGRAMS.setValue(programCheckBox.isSelected());
        LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.setValue(documentCheckBox.isSelected());
    }

    @Override
    boolean hasChanged() {
        return LibrarySettings.ALLOW_PROGRAMS.getValue() != programCheckBox.isSelected() 
                || LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue() != documentCheckBox.isSelected();
    }

    @Override
    public void initOptions() {
        programCheckBox.setSelected(LibrarySettings.ALLOW_PROGRAMS.getValue());
        documentCheckBox.setSelected(LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue());
    }
}
