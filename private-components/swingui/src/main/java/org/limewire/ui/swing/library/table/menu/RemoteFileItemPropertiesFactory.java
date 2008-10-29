package org.limewire.ui.swing.library.table.menu;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.properties.Dialog;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;

public class RemoteFileItemPropertiesFactory implements PropertiesFactory<RemoteFileItem> {

    @Override
    public Properties<RemoteFileItem> newProperties() {
        return new RemoteFileItemProperties();
    }

    private static class RemoteFileItemProperties extends Dialog implements Properties<RemoteFileItem> {

        @Override
        protected void commit() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void showProperties(RemoteFileItem propertiable) {
            showDialog(propertiable.getFileName());
        }
    }
}
