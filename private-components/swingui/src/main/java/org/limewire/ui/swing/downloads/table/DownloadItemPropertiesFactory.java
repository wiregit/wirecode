package org.limewire.ui.swing.downloads.table;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.properties.Dialog;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;

import com.google.inject.Singleton;

@Singleton
public class DownloadItemPropertiesFactory implements PropertiesFactory<DownloadItem> {

    @Override
    public Properties<DownloadItem> newProperties() {
        return new DownloadItemPropertiesImpl();
    }
    
    private static class DownloadItemPropertiesImpl extends Dialog implements Properties<DownloadItem>{

        @Override
        public void showProperties(DownloadItem propertiable) {
            //TODO - This is probably the wrong string to send to this dialog
            showDialog(propertiable.getTitle());
        }

        @Override
        protected void commit() {
            // TODO Auto-generated method stub
            
        }
    }

}
