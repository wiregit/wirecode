package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;


import javax.swing.DefaultComboBoxModel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.properties.Dialog;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.CategoryIconManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SearchResultPropertiesFactory implements PropertiesFactory<VisualSearchResult> {
    private final CategoryIconManager iconManager;
    
    @Inject
    public SearchResultPropertiesFactory(CategoryIconManager iconManager) {
        this.iconManager = iconManager;
    }
    
    @Override
    public Properties<VisualSearchResult> newProperties() {
        return new SearchResultProperties(iconManager);
    }

    public static class SearchResultProperties extends Dialog implements Properties<VisualSearchResult> {
        private final CategoryIconManager iconManager;
        
        public SearchResultProperties(CategoryIconManager iconManager) {
            this.iconManager = iconManager;
            
            title.setEditable(false);
            genre.setEditable(false);
            rating.setEditable(false);
            year.setEditable(false);
            description.setEditable(false);
            
            addDetails();

            location.setLayout(new MigLayout("", "[50%!]", "[]"));
            readOnlyInfoModel.setColumnIdentifiers(new Object[] { tr("Address"), tr("Filename") });
            location.add(new JScrollPane(readOnlyInfo));

            addLocation();
        }

        @Override
        public void showProperties(VisualSearchResult vsr) {
            if (vsr.getCategory() == Category.OTHER) {
                remove(detailsContainer);
            }
            
            icon.setIcon(iconManager.getIcon(vsr.getCategory()));
            heading.setText(vsr.getHeading());
            filename.setText(vsr.getPropertyString(FilePropertyKey.NAME));
            subheading.setText(vsr.getSubHeading());
            fileSize.setText(vsr.getPropertyString(FilePropertyKey.FILE_SIZE));
            genre.setModel(new DefaultComboBoxModel(new String[]{ vsr.getPropertyString(FilePropertyKey.GENRE) }));
            rating.setModel(new DefaultComboBoxModel(new String[]{ vsr.getPropertyString(FilePropertyKey.RATING) }));
            populateMetadata(vsr);
            copyToClipboard.setAction(new CopyMagnetLinkToClipboardAction(vsr));
            title.setText(vsr.getPropertyString(FilePropertyKey.TITLE));
            year.setText(vsr.getPropertyString(FilePropertyKey.YEAR));
            description.setText(vsr.getPropertyString(FilePropertyKey.COMMENTS));

            // Clear the table
            readOnlyInfoModel.setRowCount(0);

            for (SearchResult result : vsr.getCoreSearchResults()) {
                for (RemoteHost host : result.getSources()) {
                    readOnlyInfoModel.addRow(new Object[] { host.getRenderName(),
                            result.getFileName() });
                }
            }

            showDialog(vsr.getPropertyString(FilePropertyKey.NAME), vsr.getCategory());
        }
        
        @Override
        protected void commit() {
            // TODO
        }
    }
}
