package org.limewire.ui.swing.library.table.menu;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.MetaDataManager;
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
    private final MetaDataManager metaDataManager;

    @Inject
    public LocalFileItemPropertiesFactory(ThumbnailManager thumbnailManager,
            CategoryIconManager categoryIconManager, IconManager iconManager,
            PropertiableHeadings propertiableHeadings, MagnetLinkFactory magnetLinkFactory,
            Navigator navigator, MetaDataManager metaDataManager) {
        this.thumbnailManager = thumbnailManager;
        this.categoryIconManager = categoryIconManager;
        this.iconManager = iconManager;
        this.propertiableHeadings = propertiableHeadings;
        this.magnetLinkFactory = magnetLinkFactory;
        this.navigator = navigator;
        this.metaDataManager = metaDataManager;
    }

    @Override
    public Properties<LocalFileItem> newProperties() {
        return new LocalFileItemProperties(thumbnailManager, categoryIconManager, iconManager, 
                propertiableHeadings, magnetLinkFactory, navigator, metaDataManager);
    }

    private static class LocalFileItemProperties extends AbstractFileItemDialog implements
            Properties<LocalFileItem> {
        private final ThumbnailManager thumbnailManager;
        private final CategoryIconManager categoryIconManager;
        private final IconManager iconManager;
        private final Navigator navigator;
        private final MetaDataManager metaDataManager;
        private final Map<FilePropertyKey, Object> changedProps = new HashMap<FilePropertyKey, Object>();
        private LocalFileItem displayedItem;

        private @Resource Font smallFont;
        private @Resource Font mediumFont;
        private @Resource Font largeFont;

        private LocalFileItemProperties(ThumbnailManager thumbnailManager,
                CategoryIconManager categoryIconManager, IconManager iconManager,
                PropertiableHeadings propertiableHeadings, MagnetLinkFactory magnetLinkFactory,
                Navigator navigator, MetaDataManager metaDataManager) {
            super(propertiableHeadings, magnetLinkFactory);
            this.thumbnailManager = thumbnailManager;
            this.categoryIconManager = categoryIconManager;
            this.iconManager = iconManager;
            this.navigator = navigator;
            this.metaDataManager = metaDataManager;
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
        protected void commit() {
            if (changedProps.size() == 0) {
                return;
            }
            
            for(FilePropertyKey key : changedProps.keySet()) {
                displayedItem.setProperty(key, changedProps.get(key));
            }
            metaDataManager.save(displayedItem);
        }

        private void connectTextDocumentListener(final JTextComponent comp, final FilePropertyKey key) {
            comp.getDocument().addUndoableEditListener(new UndoableEditListener() {
                @Override
                public void undoableEditHappened(UndoableEditEvent e) {
                    changedProps.put(key, comp.getText());
                }
            });
        }

        private void connectComboSelectionListener(final JComboBox comp, final FilePropertyKey key) {
            comp.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    changedProps.put(key, comp.getSelectedItem().toString());
                }
            });
        }

        @Override
        public void showProperties(final LocalFileItem propertiable) {
            this.displayedItem = propertiable;
            
            icon.setIcon(getIcon(propertiable));
            populateCommonFields(propertiable);
            localFileLocation.setText(propertiable.getFile().getAbsolutePath());

            location.setLayout(new MigLayout("", "[]10[]15[]", "[]"));
            location.add(localFileLocation, "push");
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
                    navigator.getNavItem(NavCategory.LIBRARY,
                            LibraryNavigator.NAME_PREFIX + propertiable.getCategory()).select(
                            new NavSelectable<URN>() {
                                @Override
                                public URN getNavSelectionId() {
                                    return propertiable.getUrn();
                                }
                            });
                }
            });
            
            connectComboSelectionListener(genre, FilePropertyKey.GENRE);
            connectComboSelectionListener(rating, FilePropertyKey.RATING);
            connectComboSelectionListener(platform, FilePropertyKey.PLATFORM);
            connectTextDocumentListener(album, FilePropertyKey.TITLE);
            connectTextDocumentListener(author, FilePropertyKey.AUTHOR);
            connectTextDocumentListener(artist, FilePropertyKey.AUTHOR);
            connectTextDocumentListener(company, FilePropertyKey.COMPANY);
            connectTextDocumentListener(year, FilePropertyKey.YEAR);
            connectTextDocumentListener(title, propertiable.getCategory() == Category.AUDIO ? 
                    FilePropertyKey.TRACK_NAME : FilePropertyKey.TITLE);
            connectTextDocumentListener(track, FilePropertyKey.TRACK_NUMBER);
            connectTextDocumentListener(description, FilePropertyKey.COMMENTS);

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
            default: // OTHER, DOCUMENT
                return iconManager.getIconForFile(propertiable.getFile());
            }
        }
    }
}
