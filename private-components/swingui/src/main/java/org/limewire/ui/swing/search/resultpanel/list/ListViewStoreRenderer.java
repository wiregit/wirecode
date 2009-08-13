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
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreTrackResult;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.resultpanel.ListViewTable;
import org.limewire.ui.swing.search.resultpanel.SearchHeading;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilder;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator.FontWidthResolver;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Provider;

/**
 * Renderer component for store results.  ListViewStoreRenderer handles both
 * album and single media results.  Album results can be expanded/collapsed
 * to display/hide their track listing.
 */
abstract class ListViewStoreRenderer extends JXPanel {
    private static final int LEFT_COLUMN_WIDTH = 450;

    protected final CategoryIconManager categoryIconManager;
    protected final Provider<SearchHeadingDocumentBuilder> headingBuilder;
    protected final Provider<SearchResultTruncator> headingTruncator;
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
    private int col;

    /**
     * Constructs a ListViewStoreRenderer.
     */
    public ListViewStoreRenderer(
            CategoryIconManager categoryIconManager,
            Provider<SearchHeadingDocumentBuilder> headingBuilder,
            Provider<SearchResultTruncator> headingTruncator,
            StoreStyle storeStyle) {
        
        this.categoryIconManager = categoryIconManager;
        this.headingBuilder = headingBuilder;
        this.headingTruncator = headingTruncator;
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
        // TODO replace with custom top & bottom border
        setBorder(BorderFactory.createLineBorder(Color.WHITE));
        setToolTipText(null);
        
        // Initialize album/media panels.
        initAlbumComponent();
        initMediaComponent();
        
        // Initialize album track container.
        albumTrackPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, fill, novisualpadding"));
        albumTrackPanel.setOpaque(false);
        
        // Layout renderer components.
        setLayout(new MigLayout("insets 6 0 0 0, gap 0! 0!, novisualpadding, hidemode 3"));
        add(albumPanel, "alignx left, aligny 50%, gap 6 6 0 6, growx, pushx 200, wrap");
        add(mediaPanel, "alignx left, aligny 50%, gap 6 6 0 6, growx, pushx 200, wrap");
        add(albumTrackPanel, "span 3, left, aligny top, gap 36 36 0 6, grow, wrap");
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
    protected abstract Component createTrackComponent(StoreTrackResult result);
    
    /**
     * Returns the column width.
     */
    protected int getColumnWidth() {
        if (table != null) {
            return table.getColumnModel().getColumn(col).getWidth();
        } else {
            return 0;
        }
    }

    /**
     * Returns the heading as an HTML document.
     */
    protected String getHeadingHtml(final RowDisplayResult rowResult,
            final FontWidthResolver fontWidthResolver, 
            int headingWidth, boolean editing) {
        // The visible rect width is always 0 for renderers so getVisibleRect()
        // won't work here.  Width is zero the first time editorpane is 
        // rendered - use a wide default (roughly width of left column).
        int width = headingWidth == 0 ? LEFT_COLUMN_WIDTH : headingWidth;
        
        // Make the width seem a little smaller than usual to trigger a more 
        // hungry truncation.  Otherwise, the JEditorPane word-wrapping logic
        // kicks in and the edge word just disappears.
        final int fudgeFactorPixelWidth = width - 10;
        
        // Create SearchHeading object to supply heading text.
        SearchHeading searchHeading = new SearchHeading() {
            @Override
            public String getText() {
                String headingText = rowResult.getHeading();
                return headingTruncator.get().truncateHeading(headingText,
                        fudgeFactorPixelWidth, fontWidthResolver);
            }

            @Override
            public String getText(String adjoiningFragment) {
                int adjoiningTextPixelWidth = fontWidthResolver.getPixelWidth(adjoiningFragment);
                String headingText = rowResult.getHeading();
                return headingTruncator.get().truncateHeading(headingText,
                        fudgeFactorPixelWidth - adjoiningTextPixelWidth, fontWidthResolver);
            }
        };
        
        // Build HTML document and return.
        if (editing) {
            return headingBuilder.get().getHeadingDocument(searchHeading, vsr.getDownloadState(), rowResult.isSpam());
        } else {
            return headingBuilder.get().getHeadingDocument(searchHeading, vsr.getDownloadState(), rowResult.isSpam(), false);
        }
    }
    
    /**
     * Updates the renderer to display the specified VisualStoreResult.
     */
    public void update(JTable table, VisualStoreResult vsr, RowDisplayResult rowResult,
            boolean editing, int row, int col) {
        // Save table and store result.
        this.table = table;
        this.vsr = vsr;
        this.row = row;
        this.col = col;
        
        if (vsr.getStoreResult().isAlbum() && (vsr.getDownloadState() == BasicDownloadState.NOT_STARTED)) {
            albumPanel.setVisible(true);
            mediaPanel.setVisible(false);
            showTracksAction.putValue(Action.NAME, vsr.isShowTracks() ? 
                    I18n.tr("Hide Tracks").toUpperCase() : I18n.tr("Show Tracks").toUpperCase());
            updateAlbum(vsr, rowResult, editing);
            updateAlbumTracks(vsr);
            //System.out.println("album.prefSize=" + getPreferredSize()); // TODO REMOVE
            
        } else {
            albumPanel.setVisible(false);
            mediaPanel.setVisible(true);
            albumTrackPanel.setVisible(false);
            updateMedia(vsr, rowResult, editing);
            //System.out.println("media.prefSize=" + getPreferredSize()); // TODO REMOVE
        }
    }
    
    /**
     * Updates album content for specified VisualStoreResult.
     */
    protected abstract void updateAlbum(VisualStoreResult vsr, RowDisplayResult rowResult, boolean editing);
    
    /**
     * Updates media content for specified VisualStoreResult.
     */
    protected abstract void updateMedia(VisualStoreResult vsr, RowDisplayResult rowResult, boolean editing);
    
    /**
     * Adds display panels when multiple tracks are available.
     */
    private void updateAlbumTracks(VisualStoreResult vsr) {
        albumTrackPanel.removeAll();
        
        if (vsr.getStoreResult().getAlbumResults().size() > 1) {
            if (vsr.isShowTracks()) {
                albumTrackPanel.setVisible(true);
                List<StoreTrackResult> trackList = vsr.getStoreResult().getAlbumResults();
                for (StoreTrackResult result : trackList) {
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
            if (vsr != null) {
                System.out.println("download.actionPerformed: " + vsr.getHeading());
                // TODO implement
            }
        }
    }
    
    protected class DownloadTrackAction extends AbstractAction {
        private final StoreTrackResult trackResult;

        public DownloadTrackAction(StoreTrackResult trackResult) {
            this.trackResult = trackResult;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("downloadTrack.actionPerformed: " + trackResult.getProperty(FilePropertyKey.NAME));
            // TODO Auto-generated method stub
        }
        
    }
    
    /**
     * Action to stream store result.
     */
    private class StreamAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (vsr != null) {
                System.out.println("stream.actionPerformed: " + vsr.getHeading());
                // TODO implement
            }
        }
    }
    
    protected class StreamTrackAction extends AbstractAction {
        private final StoreTrackResult trackResult;

        public StreamTrackAction(StoreTrackResult trackResult) {
            this.trackResult = trackResult;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("streamTrack.actionPerformed: " + trackResult.getProperty(FilePropertyKey.NAME));
            // TODO Auto-generated method stub
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
                System.out.println("showInfo.actionPerformed: " + vsr.getHeading());
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
                                table.editCellAt(row, col);
                            }
                        }
                    });
                }
            }
        }
    }
}
