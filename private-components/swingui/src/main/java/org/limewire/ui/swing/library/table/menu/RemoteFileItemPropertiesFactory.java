package org.limewire.ui.swing.library.table.menu;

import java.awt.Font;

import javax.annotation.Resource;

import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PropertiableHeadings;

import com.google.inject.Inject;

public class RemoteFileItemPropertiesFactory implements PropertiesFactory<RemoteFileItem> {
    private final CategoryIconManager categoryIconManager;
    private final PropertiableHeadings propertiableHeadings;
    private final MagnetLinkFactory magnetLinkFactory;
    
    @Inject
    public RemoteFileItemPropertiesFactory(CategoryIconManager categoryIconManager,
            PropertiableHeadings propertiableHeadings, MagnetLinkFactory magnetLinkFactory) {
        this.categoryIconManager = categoryIconManager;
        this.propertiableHeadings = propertiableHeadings;
        this.magnetLinkFactory = magnetLinkFactory;
    }

    @Override
    public Properties<RemoteFileItem> newProperties() {
        return new RemoteFileItemProperties(categoryIconManager, propertiableHeadings, magnetLinkFactory);
    }

    private static class RemoteFileItemProperties extends AbstractFileItemDialog implements Properties<RemoteFileItem> {
        private final CategoryIconManager categoryIconManager;
        private @Resource Font smallFont;
        
        public RemoteFileItemProperties(CategoryIconManager categoryIconManager,
                PropertiableHeadings propertiableHeadings, MagnetLinkFactory magnetLinkFactory) {
            super(propertiableHeadings, magnetLinkFactory);
            this.categoryIconManager = categoryIconManager;
            GuiUtils.assignResources(this);
            
            title.setEditable(false);
            genre.setEditable(false);
            rating.setEditable(false);
            year.setEditable(false);
            description.setEditable(false);
        }
        
        @Override
        protected Font getSmallFont() {
            return smallFont;
        }

        @Override
        protected void commit() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void showProperties(RemoteFileItem propertiable) {
            icon.setIcon(categoryIconManager.getIcon(propertiable.getCategory()));
            populateCommonFields(propertiable);
            showDialog(propertiable.getFileName(), propertiable.getCategory());
        }
    }
}
