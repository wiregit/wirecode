package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
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
    
    protected final Action downloadAction;
    protected final Action streamAction;
    protected final Action showInfoAction;
    protected final Action showTracksAction;
    
    private JTable table;
    private VisualStoreResult vsr;
    private int row;

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
        this.downloadAction = new DownloadAction();
        this.streamAction = new StreamAction();
        this.showInfoAction = new ShowInfoAction();
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
//        albumTrackPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        albumTrackPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, fill, novisualpadding"));
        albumTrackPanel.setOpaque(false);
        
        // Layout renderer components.
        setLayout(new MigLayout("insets 6 6 0 6, gap 0! 0!, novisualpadding, hidemode 3"));
        add(albumPanel, "alignx left, aligny 50%, growx, shrinkprio 200, growprio 200, pushx 200, wrap");
        add(mediaPanel, "alignx left, aligny 50%, growx, shrinkprio 200, growprio 200, pushx 200, wrap");
        add(albumTrackPanel, "span 3, left, aligny top, gap 30 30 6 0, grow");
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
    public void update(JTable table, VisualStoreResult vsr, RowDisplayResult result,
            boolean editing, int row) {
        // Save table and store result.
        this.table = table;
        this.vsr = vsr;
        this.row = row;
        
        if (vsr.getStoreResult().isAlbum()) {
            albumPanel.setVisible(true);
            mediaPanel.setVisible(false);
            showTracksAction.putValue(Action.NAME, vsr.isShowTracks() ? 
                    I18n.tr("Hide Tracks").toUpperCase() : I18n.tr("Show Tracks").toUpperCase());
            updateAlbum(vsr, result, editing);
            updateAlbumTracks(vsr);
            
        } else {
            albumPanel.setVisible(false);
            mediaPanel.setVisible(true);
            albumTrackPanel.setVisible(false);
            updateMedia(vsr, result, editing);
        }
    }
    
    /**
     * Updates album content for specified VisualStoreResult.
     */
    protected abstract void updateAlbum(VisualStoreResult vsr, RowDisplayResult result, boolean editing);
    
    /**
     * Updates media content for specified VisualStoreResult.
     */
    protected abstract void updateMedia(VisualStoreResult vsr, RowDisplayResult result, boolean editing);
    
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
                    albumTrackPanel.add(comp, "align left, gapbottom 1, growx, wrap");
                }
            } else {
                albumTrackPanel.setVisible(false);
            }
            
        } else {
            albumTrackPanel.setVisible(false);
        }
    }
    
    /**
     * Action to download store result.
     */
    private class DownloadAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO implement
        }
    }
    
    /**
     * Action to stream store result.
     */
    private class StreamAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO implement
        }
    }
    
    /**
     * Action to show album or media information.
     */
    private class ShowInfoAction extends AbstractAction {
        
        public ShowInfoAction() {
            putValue(Action.NAME, I18n.tr("Info").toUpperCase());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (vsr != null) {
                System.out.println("showInfo.actionPerformed");
                // TODO implement
            }
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
                putValue(Action.NAME, vsr.isShowTracks() ? 
                        I18n.tr("Hide Tracks").toUpperCase() : I18n.tr("Show Tracks").toUpperCase());
                
                // Post event to update row heights.
                if (table instanceof ListViewTable) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            ((ListViewTable) table).updateRowSizes();
                            if (row >= 0) {
                                table.editCellAt(row, 0);
                            }
                        }
                    });
                }
            }
        }
    }
}
