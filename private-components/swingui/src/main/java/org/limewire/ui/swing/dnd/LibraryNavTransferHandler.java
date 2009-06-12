package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.DataFlavor;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

public class LibraryNavTransferHandler extends TransferHandler {
    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        return true;
    }
}
