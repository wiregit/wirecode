package org.limewire.ui.swing.browser;

import org.mozilla.interfaces.nsIDOMWindow;
import org.mozilla.interfaces.nsIFilePicker;
import org.mozilla.interfaces.nsILocalFile;
import org.mozilla.interfaces.nsISimpleEnumerator;
import org.mozilla.interfaces.nsIURI;

public class MozillaFilePicker extends NsISelfReferencingFactory implements nsIFilePicker {

    public MozillaFilePicker() {
        super(nsIFilePicker.NS_IFILEPICKER_IID, "@mozilla.org/filepicker;1");
    }

    @Override
    public void appendFilter(String title, String filter) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendFilters(int filterMask) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getDefaultExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDefaultString() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public nsILocalFile getDisplayDirectory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public nsILocalFile getFile() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public nsIURI getFileURL() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public nsISimpleEnumerator getFiles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getFilterIndex() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void init(nsIDOMWindow parent, String title, short mode) {
        // TODO Auto-generated method stub
        System.out.println(parent.getName());
        System.out.println(parent.getTop().getName());
        System.out.println(parent.getTop().getDocument().getLocalName());
        System.out.println(parent.getTop().getDocument().getNamespaceURI());
       

    }

    @Override
    public void setDefaultExtension(String defaultExtension) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setDefaultString(String defaultString) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setDisplayDirectory(nsILocalFile displayDirectory) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setFilterIndex(int filterIndex) {
        // TODO Auto-generated method stub

    }

    @Override
    public short show() {
        // TODO Auto-generated method stub
        return 0;
    }

}
