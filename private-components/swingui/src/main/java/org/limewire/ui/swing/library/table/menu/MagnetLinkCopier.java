package org.limewire.ui.swing.library.table.menu;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.MagnetLinkFactory;

public class MagnetLinkCopier {
    
    private FileItem fileItem;
    private MagnetLinkFactory magnetFactory;

    public MagnetLinkCopier(FileItem fileItem, MagnetLinkFactory magnetFactory){
        this.fileItem = fileItem;
        this.magnetFactory = magnetFactory;
    }
    
    public void copyLinkToClipBoard(){
        StringSelection sel = new StringSelection(magnetFactory.createMagnetLink(fileItem));
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
    }

}
