package org.limewire.ui.swing.library.table.menu;

import javax.swing.Icon;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.properties.Dialog;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LocalFileItemPropertiesFactory implements PropertiesFactory<LocalFileItem> {
    private final ThumbnailManager thumbnailManager;
    private final CategoryIconManager categoryIconManager;
    private final IconManager iconManager;

    @Inject
    public LocalFileItemPropertiesFactory(ThumbnailManager thumbnailManager, CategoryIconManager categoryIconManager,
            IconManager iconManager) {
        this.thumbnailManager = thumbnailManager;
        this.categoryIconManager = categoryIconManager;
        this.iconManager = iconManager;
    }

    @Override
    public Properties<LocalFileItem> newProperties() {
        return new LocalFileItemProperties(thumbnailManager, categoryIconManager, iconManager);
    }

    private static class LocalFileItemProperties extends Dialog implements
            Properties<LocalFileItem> {
        private final ThumbnailManager thumbnailManager;
        private final CategoryIconManager categoryIconManager;
        private final IconManager iconManager;

        private LocalFileItemProperties(ThumbnailManager thumbnailManager, CategoryIconManager categoryIconManager, IconManager iconManager) {
            this.thumbnailManager = thumbnailManager;
            this.categoryIconManager = categoryIconManager;
            this.iconManager = iconManager;
        }

        @Override
        protected void commit() {

        }

        @Override
        public void showProperties(LocalFileItem propertiable) {

            icon.setIcon(getIcon(propertiable));
            

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
