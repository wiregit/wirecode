package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

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

    private static class SearchResultProperties extends Dialog implements Properties<VisualSearchResult> {
        private final CategoryIconManager iconManager;
        
        public SearchResultProperties(CategoryIconManager iconManager) {
            this.iconManager = iconManager;
            
            title.setEditable(false);
            genre.setEditable(false);
            rating.setEditable(false);
            year.setEditable(false);
            description.setEditable(false);
            
            details.setLayout(new MigLayout("fillx", "[20%!][20%!][]", "[][][][][][]"));
            details.add(new JLabel(tr("Title")), "wrap");
            details.add(title, "span, growx, wrap");
            details.add(new JLabel(tr("Genre")));
            details.add(new JLabel(tr("Rating")));
            details.add(new JLabel(tr("Year")), "wrap");
            details.add(genre);
            details.add(rating);
            details.add(year, "growx, wrap");
            details.add(new JLabel(tr("Description")), "wrap");
            details.add(description, "grow, span");

            addDetails();

            location.setLayout(new MigLayout("", "[50%!]", "[]"));
            readOnlyInfoModel.setColumnIdentifiers(new Object[] { tr("Address"), tr("Filename") });
            location.add(new JScrollPane(readOnlyInfo));

            addLocation();
        }

        @Override
        public void showProperties(VisualSearchResult vsr) {
            icon.setIcon(iconManager.getIcon(vsr.getCategory()));
            heading.setText(vsr.getHeading());
            filename.setText(vsr.getPropertyString(FilePropertyKey.NAME));
            subheading.setText(vsr.getSubHeading());
            fileSize.setText(vsr.getPropertyString(FilePropertyKey.FILE_SIZE));
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

            showDialog(vsr.getPropertyString(FilePropertyKey.NAME));
        }

        private void populateMetadata(VisualSearchResult vsr) {
            Map<String, String> metadata = new LinkedHashMap<String, String>();
            switch(vsr.getCategory()) {
                case AUDIO:
                    addBitrateMetadata(vsr.getPropertyString(FilePropertyKey.BITRATE), metadata);
                    addSampleRateMetadata(vsr.getPropertyString(FilePropertyKey.SAMPLE_RATE), metadata);
                    break;
                case VIDEO:
                    addBitrateMetadata(vsr.getPropertyString(FilePropertyKey.BITRATE), metadata);
                    addDimensionMetadata(vsr.getPropertyString(FilePropertyKey.WIDTH), vsr.getProperty(FilePropertyKey.HEIGHT), metadata);
                    break;
                case IMAGE:
                    addDimensionMetadata(vsr.getPropertyString(FilePropertyKey.WIDTH), vsr.getProperty(FilePropertyKey.HEIGHT), metadata);
                    break;
                case DOCUMENT:
                    addDateCreatedMetadata(vsr.getPropertyString(FilePropertyKey.DATE_CREATED), metadata);
            }
            addHashMetadata(vsr.getURN(), metadata);
            setMetadataText(metadata);
        }

        @Override
        protected void commit() {
            // TODO
        }
    }
}
