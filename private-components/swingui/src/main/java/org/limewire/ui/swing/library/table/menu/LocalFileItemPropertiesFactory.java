package org.limewire.ui.swing.library.table.menu;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.properties.Dialog;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;

import com.google.inject.Singleton;

@Singleton
public class LocalFileItemPropertiesFactory implements PropertiesFactory<LocalFileItem>{

    @Override
    public Properties<LocalFileItem> newProperties() {
        return new LocalFileItemProperties();
    }

    private static class LocalFileItemProperties extends Dialog implements Properties<LocalFileItem> {

        @Override
        protected void commit() {
            
        }

        @Override
        public void showProperties(LocalFileItem propertiable) {
            showDialog(propertiable.getFileName());
        }
    }
}
