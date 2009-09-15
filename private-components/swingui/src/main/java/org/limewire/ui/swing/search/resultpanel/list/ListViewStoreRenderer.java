package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.ui.swing.components.CustomLineBorder;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.resultpanel.ListViewTable;
import org.limewire.ui.swing.search.resultpanel.ResultsTable;
import org.limewire.ui.swing.search.resultpanel.SearchHeading;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilder;
import org.limewire.ui.swing.search.resultpanel.SearchResultMenu;
import org.limewire.ui.swing.search.resultpanel.SearchResultMenuFactory;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator;
import org.limewire.ui.swing.search.resultpanel.SearchResultMenu.ViewType;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator.FontWidthResolver;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.search.store.StoreBrowserPanel;
import org.limewire.ui.swing.search.store.StoreController;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.swing.DefaultEventTableModel;

import com.google.inject.Provider;

/**
 * Renderer component for store results.  ListViewStoreRenderer handles both
 * album and single media results.  Album results can be expanded/collapsed
 * to display/hide their track listing.
 */
abstract class ListViewStoreRenderer extends JXPanel {
    protected static final String HTML_BEGIN = "<html>";
    protected static final String HTML_END = "</html>";
    private static final int LEFT_COLUMN_WIDTH = 450;

    protected final StoreStyle storeStyle;
    protected final CategoryIconManager categoryIconManager;
    protected final Provider<SearchHeadingDocumentBuilder> headingBuilder;
    protected final Provider<SearchResultTruncator> headingTruncator;
    protected final SearchResultMenuFactory popupMenuFactory;
    protected final StoreController storeController;

    protected final JXPanel albumPanel;
    protected final JXPanel mediaPanel;
    protected final JXPanel albumTrackPanel;
    
    protected final Action downloadAction;
    protected final Action streamAction;
    protected final Action showInfoAction;
    protected final Action showTracksAction;
    
    private MouseListener popupListener;
    private JTable table;
    private VisualStoreResult vsr;
    private int row;
    private int col;

    /**
     * Constructs a ListViewStoreRenderer.
     */
    public ListViewStoreRenderer(
            StoreStyle storeStyle,
            CategoryIconManager categoryIconManager,
            Provider<SearchHeadingDocumentBuilder> headingBuilder,
            Provider<SearchResultTruncator> headingTruncator,
            SearchResultMenuFactory popupMenuFactory,
            StoreController storeController) {
        
        this.storeStyle = storeStyle;
        this.categoryIconManager = categoryIconManager;
        this.headingBuilder = headingBuilder;
        this.headingTruncator = headingTruncator;
        this.popupMenuFactory = popupMenuFactory;
        this.storeController = storeController;
        
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
        setBorder(new CustomLineBorder(Color.WHITE, 1, Color.WHITE, 0, Color.WHITE, 1, Color.WHITE, 0));
        setToolTipText(null);
        
        // Initialize album/media panels.
        initAlbumComponent();
        initMediaComponent();
        
        // Initialize popup listener.
        installPopupListener(this);
        installPopupListener(albumPanel);
        installPopupListener(mediaPanel);
        
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
    protected abstract Component createTrackComponent(TrackResult result);
    
    /**
     * Installs the popup and selection listener on the specified component.
     */
    protected void installPopupListener(Component component) {
        if (popupListener == null) {
            popupListener = new SelectionPopupListener();
        }
        
        component.addMouseListener(popupListener);
    }
    
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
            
        } else {
            albumPanel.setVisible(false);
            mediaPanel.setVisible(true);
            albumTrackPanel.setVisible(false);
            updateMedia(vsr, rowResult, editing);
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
        
        if (vsr.getStoreResult().isAlbum()) {
            if (vsr.isShowTracks()) {
                albumTrackPanel.setVisible(true);
                List<TrackResult> trackList = vsr.getStoreResult().getAlbumResults();
                for (TrackResult result : trackList) {
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
     * Updates the table row selection based on the specified mouse event.
     */
    private void updateSelection(MouseEvent e) {
        if (table.isEditing()) {
            // Get cell being edited by this editor.
            int editRow = table.getEditingRow();
            int editCol = table.getEditingColumn();
            
            // Update the selection.  We also prepare the editor to apply
            // the selection colors to the current editor component.
            if ((editRow > -1) && (editRow < table.getRowCount())) {
                table.changeSelection(editRow, editCol, e.isControlDown(), e.isShiftDown());
                table.prepareEditor(table.getCellEditor(), editRow, editCol);
            }
        }
        
        // Request focus so Enter key can be handled.
        e.getComponent().requestFocusInWindow();
    }
    
    /**
     * Action to download store result.
     */
    private class DownloadAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (vsr != null) {
                storeController.download(vsr);
            }
        }
    }
    
    /**
     * Action to download store track result.
     */
    protected class DownloadTrackAction extends AbstractAction {
        private final TrackResult trackResult;

        public DownloadTrackAction(TrackResult trackResult) {
            this.trackResult = trackResult;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            storeController.downloadTrack(trackResult);
        }
    }
    
    /**
     * Action to stream store result.
     */
    private class StreamAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (vsr != null) {
                storeController.stream(vsr);
            }
        }
    }
    
    /**
     * Action to stream store track result.
     */
    protected class StreamTrackAction extends AbstractAction {
        private final TrackResult trackResult;

        public StreamTrackAction(TrackResult trackResult) {
            this.trackResult = trackResult;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            storeController.streamTrack(trackResult);
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
                new StoreBrowserPanel(storeController).showInfo(vsr);
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
    
    /**
     * Mouse listener to update selection and display popup menu.
     */
    private class SelectionPopupListener extends MousePopupListener {
        
        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                updateSelection(e);
            }
            super.mousePressed(e);
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if ((vsr != null) && (e.getClickCount() == 2) && SwingUtilities.isLeftMouseButton(e)) {  
                storeController.download(vsr);
            } else {
                super.mouseClicked(e);
            }
        }
        
        @Override
        public void handlePopupMouseEvent(MouseEvent e) {
            // Update selection if mouse is not in selected row.
            if (table.isEditing()) {
                int editRow = table.getEditingRow();
                if (!table.isRowSelected(editRow)) {
                    updateSelection(e);
                }
            }
            
            // Create list of selected results.
            List<VisualSearchResult> selectedResults = new ArrayList<VisualSearchResult>();
            DefaultEventTableModel model = ((ResultsTable) table).getEventTableModel();
            int[] selectedRows = table.getSelectedRows();
            for (int row : selectedRows) {
                Object element = model.getElementAt(row);
                if (element instanceof VisualSearchResult) {
                    selectedResults.add((VisualSearchResult) element);
                }
            }
            
            // If nothing selected, use current result.
            if (selectedResults.size() == 0) {
                selectedResults.add(vsr);
            }
            
            // Display context menu.
            SearchResultMenu searchResultMenu = popupMenuFactory.create(
                    storeController.getDownloadHandler(), selectedResults, ViewType.List);
            searchResultMenu.show(e.getComponent(), e.getX()+3, e.getY()+3);
        }
    }
}
