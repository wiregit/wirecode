package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.action.BitziLookupAction;
import org.limewire.ui.swing.properties.Dialog;
import org.limewire.ui.swing.properties.DialogParam;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SearchResultPropertiesFactory implements PropertiesFactory<VisualSearchResult> {
    private final DialogParam dialogParam;
    
    @Inject
    public SearchResultPropertiesFactory(DialogParam dialogParam) {
        this.dialogParam = dialogParam;
    }
    
    @Override
    public Properties<VisualSearchResult> newProperties() {
        return new SearchResultProperties(dialogParam);
    }

    public static class SearchResultProperties extends Dialog implements Properties<VisualSearchResult> {
        private final CategoryIconManager categoryIconManager;
        
        public SearchResultProperties(DialogParam dialogParam) {
            super(dialogParam);
            this.categoryIconManager = dialogParam.getCategoryIconManager();
            GuiUtils.assignResources(this);
            
            disableEdit(album, author, artist, company, year, title, track);
            disableComponent(description, genre, rating, platform);
            
            location.setLayout(new MigLayout("nocache", "[50%!]", "[]"));
            readOnlyInfoModel.setColumnIdentifiers(new Object[] { tr("Address"), tr("Filename") });
            location.add(new JScrollPane(readOnlyInfo));
        }
        
        protected void disableComponent(JComponent... comps) {
            for(JComponent comp : comps) {
                comp.setEnabled(false);
            }
        }

        @Override
        public void showProperties(VisualSearchResult vsr) {
            icon.setIcon(categoryIconManager.getIcon(vsr, iconManager));
            heading.setText(vsr.getHeading());
            filename.setText(vsr.getPropertyString(FilePropertyKey.NAME));
            fileSize.setText(vsr.getPropertyString(FilePropertyKey.FILE_SIZE));
            genre.setModel(new DefaultComboBoxModel(new String[]{ vsr.getPropertyString(FilePropertyKey.GENRE) }));
            rating.setModel(new DefaultComboBoxModel(new String[]{ vsr.getPropertyString(FilePropertyKey.RATING) }));
            populateMetadata(vsr);
            copyToClipboard.setAction(new CopyMagnetLinkToClipboardAction(vsr));
            moreFileInfo.setAction(new BitziLookupAction(vsr));
            album.setText(vsr.getPropertyString(FilePropertyKey.ALBUM));
            title.setText(vsr.getPropertyString(FilePropertyKey.TITLE));
            year.setText(vsr.getPropertyString(FilePropertyKey.YEAR));
            description.setText(vsr.getPropertyString(FilePropertyKey.COMMENTS));

            // Clear the table
            readOnlyInfoModel.setRowCount(0);

            for (SearchResult result : vsr.getCoreSearchResults()) {
                for (RemoteHost host : result.getSources()) {
                    readOnlyInfoModel.addRow(new Object[] { host.getFriendPresence().getFriend().getRenderName(),
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
