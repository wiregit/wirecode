package org.limewire.ui.swing.search.resultpanel;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.ui.swing.search.model.VisualSearchResult;

final class CopyMagnetLinkToClipboardAction extends AbstractAction {
    private final VisualSearchResult vsr;

    CopyMagnetLinkToClipboardAction(String name, VisualSearchResult vsr) {
        super(name);
        this.vsr = vsr;
    }

    public void actionPerformed(ActionEvent e) {
        StringSelection sel = new StringSelection(vsr.getMagnetLink());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
    }
}