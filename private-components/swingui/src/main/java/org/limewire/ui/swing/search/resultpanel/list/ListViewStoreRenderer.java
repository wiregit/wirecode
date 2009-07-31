package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.resultpanel.ListViewTable;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

/**
 * Renderer component for store results.  ListViewStoreRenderer handles both
 * album and single media results.  Album results can be expanded/collapsed
 * to display/hide their track listing.
 */
abstract class ListViewStoreRenderer extends JXPanel {

    protected final CategoryIconManager categoryIconManager;
    protected final StoreStyle storeStyle;

    protected final JXPanel albumPanel;
    protected final JXPanel mediaPanel;
    protected final JXPanel albumTrackPanel;
    
    protected final Action showTracksAction;
    
    private JTable table;
    private VisualStoreResult vsr;

    /**
     * Constructs a ListViewStoreRenderer.
     */
    public ListViewStoreRenderer(
            CategoryIconManager categoryIconManager,
            StoreStyle storeStyle) {
        
        this.categoryIconManager = categoryIconManager;
        this.storeStyle = storeStyle;
        this.albumPanel = new JXPanel();
        this.mediaPanel = new JXPanel();
        this.albumTrackPanel = new JXPanel();
        this.showTracksAction = new ShowTracksAction();
        
        initComponents();
    }

    /**
     * Initializes the components in the renderer.
     */
    private void initComponents() {
        // Initialize album/media panels.
        initAlbumComponent();
        initMediaComponent();
        
        // Initialize album track container.
        albumTrackPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        albumTrackPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, fill, novisualpadding"));
        
        // Layout renderer components.
        setLayout(new MigLayout("insets 6 6 0 6, gap 0! 0!, novisualpadding, hidemode 3"));
        add(albumPanel, "alignx left, aligny 50%, growx, shrinkprio 200, growprio 200, pushx 200, wrap");
        add(mediaPanel, "alignx left, aligny 50%, growx, shrinkprio 200, growprio 200, pushx 200, wrap");
        add(albumTrackPanel, "span 3, left, aligny top, gap 16 16 6 0, grow");
    }
    
    /**
     * Initializes component to display an album.
     */
    protected abstract void initAlbumComponent();
    
    /**
     * Initializes component to display a single media file.
     */
    protected abstract void initMediaComponent();
    
    /**
     * Creates a component to display an album track.
     */
    protected abstract Component createTrackComponent(SearchResult result);

    /**
     * Updates the renderer to display the specified VisualStoreResult.
     */
    public void update(JTable table, VisualStoreResult vsr, RowDisplayResult result) {
        // Save table and store result.
        this.table = table;
        this.vsr = vsr;
        
        if (vsr.getStoreResult().isAlbum()) {
            albumPanel.setVisible(true);
            mediaPanel.setVisible(false);
            showTracksAction.putValue(Action.NAME, vsr.isShowTracks() ? I18n.tr("Hide Tracks") : I18n.tr("Show Tracks"));
            updateAlbum(vsr, result);
            updateAlbumTracks(vsr);
            
        } else {
            albumPanel.setVisible(false);
            mediaPanel.setVisible(true);
            albumTrackPanel.setVisible(false);
            updateMedia(vsr, result);
        }
    }
    
    /**
     * Updates album content for specified VisualStoreResult.
     */
    protected abstract void updateAlbum(VisualStoreResult vsr, RowDisplayResult result);
    
    /**
     * Updates media content for specified VisualStoreResult.
     */
    protected abstract void updateMedia(VisualStoreResult vsr, RowDisplayResult result);
    
    /**
     * Adds display panels when multiple tracks are available.
     */
    private void updateAlbumTracks(VisualStoreResult vsr) {
        albumTrackPanel.removeAll();
        
        if (vsr.getStoreResult().getAlbumResults().size() > 1) {
            if (vsr.isShowTracks()) {
                albumTrackPanel.setVisible(true);
                List<SearchResult> fileList = vsr.getStoreResult().getAlbumResults();
                for (SearchResult result : fileList) {
                    Component comp = createTrackComponent(result);
                    albumTrackPanel.add(comp, "align left, growx, wrap");
                }
            } else {
                albumTrackPanel.setVisible(false);
            }
            
        } else {
            albumTrackPanel.setVisible(false);
        }
    }
    
    /**
     * Action to show or hide album tracks.
     */
    private class ShowTracksAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (vsr != null) {
                // Toggle indicator.
                vsr.setShowTracks(!vsr.isShowTracks());
                
                // Adjust action text.
                putValue(Action.NAME, vsr.isShowTracks() ? I18n.tr("Hide Tracks") : I18n.tr("Show Tracks"));
                
                // Post event to update row heights.
                if (table instanceof ListViewTable) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            ((ListViewTable) table).updateRowSizes();
                        }
                    });
                }
            }
        }
    }
}
