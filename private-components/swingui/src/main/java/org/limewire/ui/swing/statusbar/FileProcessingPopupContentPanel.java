package org.limewire.ui.swing.statusbar;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.google.inject.Inject;

public class FileProcessingPopupContentPanel extends JPanel {

    @Inject
    public FileProcessingPopupContentPanel() {
        add(new JLabel("hello"));
    }
}
