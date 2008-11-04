package org.limewire.ui.swing.library.table.menu;

import java.awt.Font;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.PropertiableHeadings;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LocalFileItemPropertiesFactory implements PropertiesFactory<LocalFileItem> {
    private final ThumbnailManager thumbnailManager;
    private final CategoryIconManager categoryIconManager;
    private final IconManager iconManager;
    private final PropertiableHeadings propertiableHeadings;
    private final MagnetLinkFactory magnetLinkFactory;

    @Inject
    public LocalFileItemPropertiesFactory(ThumbnailManager thumbnailManager, CategoryIconManager categoryIconManager,
            IconManager iconManager, PropertiableHeadings propertiableHeadings, MagnetLinkFactory magnetLinkFactory) {
        this.thumbnailManager = thumbnailManager;
        this.categoryIconManager = categoryIconManager;
        this.iconManager = iconManager;
        this.propertiableHeadings = propertiableHeadings;
        this.magnetLinkFactory = magnetLinkFactory;
    }

    @Override
    public Properties<LocalFileItem> newProperties() {
        return new LocalFileItemProperties(thumbnailManager, categoryIconManager, iconManager, propertiableHeadings, magnetLinkFactory);
    }

    private static class LocalFileItemProperties extends AbstractFileItemDialog implements
            Properties<LocalFileItem> {
        private final ThumbnailManager thumbnailManager;
        private final CategoryIconManager categoryIconManager;
        private final IconManager iconManager;
        private @Resource Font smallFont;

        private LocalFileItemProperties(ThumbnailManager thumbnailManager, CategoryIconManager categoryIconManager, 
                IconManager iconManager, PropertiableHeadings propertiableHeadings, MagnetLinkFactory magnetLinkFactory) {
            super(propertiableHeadings, magnetLinkFactory);
            this.thumbnailManager = thumbnailManager;
            this.categoryIconManager = categoryIconManager;
            this.iconManager = iconManager;
            GuiUtils.assignResources(this);
        }
        
        @Override
        protected Font getSmallFont() {
            return smallFont;
        }

        @Override
        protected void commit() {

        }

        @Override
        public void showProperties(LocalFileItem propertiable) {
            icon.setIcon(getIcon(propertiable));
            populateCommonFields(propertiable);
            showDialog(propertiable.getFileName(), propertiable.getCategory());
        }

        private Icon getIcon(LocalFileItem propertiable) {
            switch (propertiable.getCategory()) {
            case IMAGE:
                return thumbnailManager.getThumbnailForFile(propertiable.getFile());
            case AUDIO:
            case VIDEO:
            case PROGRAM:
                return categoryIconManager.getIcon(propertiable.getCategory());
            default: //OTHER, DOCUMENT
                return iconManager.getIconForFile(propertiable.getFile());
            }
        }
    }
}
