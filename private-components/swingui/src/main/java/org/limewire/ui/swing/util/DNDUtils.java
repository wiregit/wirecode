package org.limewire.ui.swing.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.limewire.util.URIUtils;

/**
 * Static helper class with DND tasks that provides methods for handling URI and
 * file drops and also provides default transfer handlers.
 */
public class DNDUtils {
    public static final DataFlavor URIFlavor = createURIFlavor();

    /**
     * Returns array of uris extracted from transferable.
     * 
     * @param transferable
     * @return
     * @throws UnsupportedFlavorException
     * @throws IOException
     */
    public static URI[] getURIs(Transferable transferable) throws UnsupportedFlavorException,
            IOException {

        String lines = (String) transferable.getTransferData(URIFlavor);
        StringTokenizer st = new StringTokenizer(lines, System.getProperty("line.separator"));
        ArrayList<URI> uris = new ArrayList<URI>();
        while (st.hasMoreTokens()) {
            String line = st.nextToken().trim();
            if (line.length() == 0) {
                continue;
            }
            try {
                URI uri = URIUtils.toURI(line);
                uris.add(uri);
            } catch (URISyntaxException e) {
            }
        }
        return uris.toArray(new URI[uris.size()]);
    }

    /**
     * Returns true if the flavor is contained in the array.
     * 
     * @param array
     * @param flavor
     * @return
     */
    public static boolean contains(DataFlavor[] array, DataFlavor flavor) {
        for (int i = 0; i < array.length; i++) {
            if (flavor.equals(array[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks for {@link DataFlavor#javaFileListFlavor} and
     * {@link FileTransferable#URIFlavor} for unix systems.
     * 
     * @param flavors
     * @return
     */
    public static boolean containsFileFlavors(DataFlavor[] flavors) {
        return contains(flavors, DataFlavor.javaFileListFlavor) || contains(flavors, URIFlavor);
    }

    /**
     * Extracts the array of files from a transferable
     * 
     * @param transferable
     * @return an empty array if the transferable does not contain any data that
     *         can be interpreted as a list of files
     * @throws UnsupportedFlavorException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static File[] getFiles(Transferable transferable) throws UnsupportedFlavorException,
            IOException {
        if (contains(transferable.getTransferDataFlavors(), DataFlavor.javaFileListFlavor)) {
            return ((List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor))
                    .toArray(new File[0]);
        } else if (contains(transferable.getTransferDataFlavors(), URIFlavor)) {
            return getFiles(getURIs(transferable));
        }
        return new File[0];
    }

    /**
     * Returns array of files for uris that denote local paths.
     * 
     * @return empty array if no uri denotes a local file
     */
    public static File[] getFiles(URI[] uris) {
        ArrayList<File> files = new ArrayList<File>(uris.length);
        for (URI uri : uris) {
            String scheme = uri.getScheme();
            if (uri.isAbsolute() && scheme != null && scheme.equalsIgnoreCase("file")) {
                String path = uri.getPath();
                files.add(new File(path));
            }
        }
        return files.toArray(new File[files.size()]);
    }

    private static DataFlavor createURIFlavor() {
        try {
            return new DataFlavor("text/uri-list;class=java.lang.String");
        } catch (ClassNotFoundException cnfe) {
            return null;
        }
    }

}
