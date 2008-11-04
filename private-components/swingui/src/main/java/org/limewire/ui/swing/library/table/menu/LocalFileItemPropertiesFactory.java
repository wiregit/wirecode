package org.limewire.ui.swing.library.table.menu;

import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.Icon;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.library.LibraryNavigator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;
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
    private final Navigator navigator;

    @Inject
    public LocalFileItemPropertiesFactory(ThumbnailManager thumbnailManager, CategoryIconManager categoryIconManager,
            IconManager iconManager, PropertiableHeadings propertiableHeadings, MagnetLinkFactory magnetLinkFactory, Navigator navigator) {
        this.thumbnailManager = thumbnailManager;
        this.categoryIconManager = categoryIconManager;
        this.iconManager = iconManager;
        this.propertiableHeadings = propertiableHeadings;
        this.magnetLinkFactory = magnetLinkFactory;
        this.navigator = navigator;
    }

    @Override
    public Properties<LocalFileItem> newProperties() {
        return new LocalFileItemProperties(thumbnailManager, categoryIconManager, iconManager, propertiableHeadings, magnetLinkFactory, navigator);
    }

    private static class LocalFileItemProperties extends AbstractFileItemDialog implements
            Properties<LocalFileItem> {
        private final ThumbnailManager thumbnailManager;
        private final CategoryIconManager categoryIconManager;
        private final IconManager iconManager;
        private final Navigator navigator;
        
        private @Resource Font smallFont;

        private LocalFileItemProperties(ThumbnailManager thumbnailManager, CategoryIconManager categoryIconManager, 
                IconManager iconManager, PropertiableHeadings propertiableHeadings, MagnetLinkFactory magnetLinkFactory,
                Navigator navigator) {
            super(propertiableHeadings, magnetLinkFactory);
            this.thumbnailManager = thumbnailManager;
            this.categoryIconManager = categoryIconManager;
            this.iconManager = iconManager;
            this.navigator = navigator;
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
        public void showProperties(final LocalFileItem propertiable) {
            icon.setIcon(getIcon(propertiable));
            populateCommonFields(propertiable);
            localFileLocation.setText(propertiable.getFileName());
            
            location.setLayout(new MigLayout("", "[]push[]15[]", "[]"));
            location.add(localFileLocation);
            location.add(locateOnDisk);
            location.add(locateInLibrary);
            
            locateOnDisk.setAction(new AbstractAction(I18n.tr("locate on disk")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NativeLaunchUtils.launchExplorer(propertiable.getFile());
                }
            });
            
            locateInLibrary.setAction(new AbstractAction(I18n.tr("locate in library")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                    navigator.getNavItem(
                            NavCategory.LIBRARY,
                            LibraryNavigator.NAME_PREFIX + propertiable.getCategory()).select(new NavSelectable<URN>() {
                                @Override
                                public URN getNavSelectionId() {
                                    return propertiable.getUrn();
                                }
                            });
                }
            });
            
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
