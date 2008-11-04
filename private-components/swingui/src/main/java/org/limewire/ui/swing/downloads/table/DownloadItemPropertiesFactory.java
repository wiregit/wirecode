package org.limewire.ui.swing.downloads.table;

import java.awt.Font;

import javax.annotation.Resource;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.properties.Dialog;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Singleton;

@Singleton
public class DownloadItemPropertiesFactory implements PropertiesFactory<DownloadItem> {

    @Override
    public Properties<DownloadItem> newProperties() {
        return new DownloadItemProperties();
    }
    
    private static class DownloadItemProperties extends Dialog implements Properties<DownloadItem>{
        private @Resource Font smallFont;
        private @Resource Font mediumFont;
        private @Resource Font largeFont;
        
        public DownloadItemProperties() {
            GuiUtils.assignResources(this);
        }

        @Override
        protected Font getSmallFont() {
            return smallFont;
        }
        
        @Override
        protected Font getLargeFont() {
            return largeFont;
        }

        @Override
        protected Font getMediumFont() {
            return mediumFont;
        }

        @Override
        public void showProperties(DownloadItem propertiable) {
            //TODO - This is probably the wrong string to send to this dialog
            showDialog(propertiable.getTitle(), propertiable.getCategory());
        }

        @Override
        protected void commit() {
            // TODO Auto-generated method stub
            
        }
    }

}
