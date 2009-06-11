package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.limewire.ui.swing.util.DNDUtils;


public class LocalFileTransferable implements Transferable {
    
    public static final DataFlavor LOCAL_FILE_DATA_FLAVOR = new DataFlavor(File[].class, "Local File array");
    
    private File[] files;
    
    public LocalFileTransferable(File[] files){
        this.files = files;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        System.out.println("flavor: " + flavor);
        if(flavor.equals(LOCAL_FILE_DATA_FLAVOR)){
            System.out.println("files: " + files);
            return files;
        } else if( flavor.equals(DNDUtils.URIFlavor)) {
            String seperator = System.getProperty("line.separator"); 
            StringBuffer lines = new StringBuffer();
            for(File file : files) {
                lines.append(file.toURI().toString());
                lines.append(seperator);
            }
            lines.append(seperator);
            System.out.println("lists: " + lines);
            return lines.toString();
        } else if(flavor.equals(DataFlavor.javaFileListFlavor)) {
            System.out.println("list: " + Arrays.asList(files));
            return Arrays.asList(files);
        }
        
        throw new UnsupportedFlavorException(flavor);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return DNDUtils.getFileFlavors();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DNDUtils.isFileFlavor(flavor);
    }

}
