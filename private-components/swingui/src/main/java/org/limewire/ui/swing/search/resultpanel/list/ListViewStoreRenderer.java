package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Category;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.ui.swing.components.CustomLineBorder;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.resultpanel.HeadingFontWidthResolver;
import org.limewire.ui.swing.search.resultpanel.ListViewTable;
import org.limewire.ui.swing.search.resultpanel.SearchHeading;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilder;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator.FontWidthResolver;
import org.limewire.ui.swing.search.resultpanel.classic.StoreRendererResourceManager;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.search.store.StoreBrowserPanel;
import org.limewire.ui.swing.search.store.StoreController;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

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

    private final CategoryIconManager categoryIconManager;
    private final Provider<SearchHeadingDocumentBuilder> headingBuilder;
    private final Provider<SearchResultTruncator> headingTruncator;
    private final LibraryMediator libraryMediator;
    private final MainDownloadPanel mainDownloadPanel;
    private final StoreRendererResourceManager storeResourceManager;
    private final MouseListener popupListener;
    protected final StoreController storeController;
    
    protected final Action downloadAction;
    protected final Action streamAction;
    protected final Action showInfoAction;
    protected final Action showTracksAction;
    
    protected final JXPanel albumPanel;
    protected final JXPanel mediaPanel;
    protected final JXPanel downloadPanel;
    protected final JXPanel albumTrackPanel;
    
    private IconButton downloadIconButton;
    private HTMLLabel downloadHeadingLabel;
    private JButton downloadInfoButton;
    private FontWidthResolver downloadWidthResolver;
    private int downloadHeadingWidth;
    
    protected StoreStyle storeStyle;
    
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
            LibraryMediator libraryMediator,
            MainDownloadPanel mainDownloadPanel,
            StoreRendererResourceManager storeResourceManager,
            MousePopupListener popupListener,
            StoreController storeController) {
        
        this.storeStyle = storeStyle;
        this.categoryIconManager = categoryIconManager;
        this.headingBuilder = headingBuilder;
        this.headingTruncator = headingTruncator;
        this.libraryMediator = libraryMediator;
        this.mainDownloadPanel = mainDownloadPanel;
        this.storeResourceManager = storeResourceManager;
        this.popupListener = popupListener;
        this.storeController = storeController;
        
        this.downloadAction = new DownloadAction();
        this.streamAction = new StreamAction();
        this.showInfoAction = new ShowInfoAction();
        this.showTracksAction = new ShowTracksAction();
        
        this.albumPanel = new JXPanel();
        this.mediaPanel = new JXPanel();
        this.downloadPanel = new JXPanel();
        this.albumTrackPanel = new JXPanel();
        
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
        initDownloadComponent();
        
        // Initialize popup listener.
        installPopupListener(this);
        installPopupListener(albumPanel);
        installPopupListener(mediaPanel);
        installPopupListener(downloadPanel);
        
        // Initialize album track container.
        albumTrackPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, fill, novisualpadding"));
        albumTrackPanel.setOpaque(false);
        
        // Layout renderer components.
        setLayout(new MigLayout("insets 6 0 0 0, gap 0! 0!, novisualpadding, hidemode 3"));
        add(albumPanel, "alignx left, aligny 50%, gap 6 6 0 6, growx, pushx 200, wrap");
        add(mediaPanel, "alignx left, aligny 50%, gap 6 6 0 6, growx, pushx 200, wrap");
        add(downloadPanel, "alignx left, aligny 50%, gap 6 6 0 6, growx, pushx 200, wrap");
        add(albumTrackPanel, "span 3, left, aligny top, gap 36 36 0 6, grow, wrap");
    }
    
    /**
     * Initializes component to display a downloaded file.
     */
    private void initDownloadComponent() {
        downloadPanel.setOpaque(false);
        
        downloadIconButton = new IconButton();
        downloadIconButton.removeActionHandListener();
        
        // Create heading label.  We override paint to cache heading width.
        downloadHeadingLabel = new HTMLLabel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                downloadHeadingWidth = getSize().width;
            }
        };
        downloadHeadingLabel.setOpenUrlsNatively(false);
        downloadHeadingLabel.setOpaque(false);
        downloadHeadingLabel.setFocusable(false);
        downloadHeadingLabel.setMargin(new Insets(2, 0, 2, 3));
        downloadHeadingLabel.setMinimumSize(new Dimension(0, 22));
        downloadHeadingLabel.addHyperlinkListener(new HeadingHyperlinkListener(downloadHeadingLabel));
        installPopupListener(downloadHeadingLabel);
        
        downloadInfoButton = new IconButton(showInfoAction);
        downloadInfoButton.setHideActionText(false);
        
        applyDownloadStyle();
        
        // Layout components in container.
        downloadPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        downloadPanel.add(downloadIconButton, "alignx left, aligny 50%, shrinkprio 0, growprio 0");
        downloadPanel.add(downloadHeadingLabel, "alignx left, aligny 50%, gapleft 6, growx, shrinkprio 200, growprio 200, pushx 200");
        downloadPanel.add(downloadInfoButton, "alignx right, aligny top, shrinkprio 0, growprio 0");
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
        component.addMouseListener(popupListener);
    }
    
    /**
     * Returns the bounds of the current cell.
     */
    protected Rectangle getCellRect() {
        return (table != null) ? table.getCellRect(row, col, false) : new Rectangle(0, 0);
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
     * Returns the display icon for the specified store result.
     */
    protected Icon getIcon(VisualStoreResult vsr) {
        if (vsr.isSpam()) {
            return storeResourceManager.getSpamIcon();
        }
        
        switch (vsr.getDownloadState()) {
        case DOWNLOADING:
            return storeResourceManager.getDownloadIcon();
            
        case DOWNLOADED:
        case LIBRARY:
            return storeResourceManager.getLibraryIcon();
            
        default:
            if (vsr.getStoreResult().isAlbum()) {
                return storeResourceManager.getAlbumIcon();
            } else if (vsr.getCategory() == Category.AUDIO) {
                return storeResourceManager.getAudioIcon();
            }
            return categoryIconManager.getIcon(vsr);
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
        
        if (vsr.getDownloadState() != BasicDownloadState.NOT_STARTED) {
            albumPanel.setVisible(false);
            mediaPanel.setVisible(false);
            downloadPanel.setVisible(true);
            albumTrackPanel.setVisible(false);
            updateDownload(vsr, rowResult, editing);
            
        } else if (vsr.getStoreResult().isAlbum()) {
            albumPanel.setVisible(true);
            mediaPanel.setVisible(false);
            downloadPanel.setVisible(false);
            showTracksAction.putValue(Action.NAME, vsr.isShowTracks() ? 
                    I18n.tr("Hide Tracks").toUpperCase() : I18n.tr("Show Tracks").toUpperCase());
            updateAlbum(vsr, rowResult, editing);
            updateAlbumTracks(vsr);
            
        } else {
            albumPanel.setVisible(false);
            mediaPanel.setVisible(true);
            downloadPanel.setVisible(false);
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
                List<TrackResult> trackList = vsr.getStoreResult().getTracks();
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
     * Updating download view for specified VisualStoreResult.
     */
    private void updateDownload(VisualStoreResult vsr, RowDisplayResult rowResult, boolean editing) {
        // Set category icon.
        downloadIconButton.setIcon(getIcon(vsr));
        
        // Info button may be hidden when not editing.
        downloadInfoButton.setVisible(!storeStyle.isShowInfoOnHover() || editing);
        
        // Initialize heading width if necessary. 
        if (editing && (downloadHeadingWidth == 0)) {
            // Apply cell dimensions to component and perform layout.
            Rectangle cellRect = getCellRect();
            setSize(cellRect.width, cellRect.height);
            doLayout();
            // Compute heading width by subtracting gap sizes and button widths. 
            downloadHeadingWidth = getWidth() - 18 - downloadIconButton.getWidth() - downloadInfoButton.getWidth();
        }
        
        // Set text field.
        downloadHeadingLabel.setText(getHeadingHtml(rowResult, downloadWidthResolver, downloadHeadingWidth, editing));
        downloadHeadingLabel.setToolTipText(HTML_BEGIN + rowResult.getHeading() + HTML_END);
    }
    
    /**
     * Returns true if the current style matches the specified style.
     */
    public boolean isCurrentStyle(StoreStyle storeStyle) {
        return (this.storeStyle.getType() == storeStyle.getType());
    }
    
    /**
     * Updates the renderer using the specified StoreStyle.  The type of the
     * specified style should match the type of the current style.
     */
    public void updateStyle(StoreStyle storeStyle) {
        if (isCurrentStyle(storeStyle)) {
            this.storeStyle = storeStyle;
            applyDownloadStyle();
            applyStyle();
        }
    }
    
    /**
     * Applies the current style to the renderer.  This method is called when
     * the style is updated with new icons while in use.
     */
    protected abstract void applyStyle();
    
    /**
     * Applies the current style to the download renderer.
     */
    private void applyDownloadStyle() {
        downloadHeadingLabel.setHtmlFont(storeStyle.getHeadingFont());
        downloadHeadingLabel.setHtmlForeground(storeStyle.getHeadingForeground());
        downloadHeadingLabel.setHtmlLinkForeground(storeStyle.getHeadingForeground());
        
        downloadWidthResolver = new HeadingFontWidthResolver(downloadHeadingLabel, storeStyle.getHeadingFont());
        
        downloadInfoButton.setFont(storeStyle.getInfoFont());
        downloadInfoButton.setForeground(storeStyle.getInfoForeground());
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
     * Hyperlink listener for heading labels.
     */
    public class HeadingHyperlinkListener implements HyperlinkListener {
        private final Component source;

        public HeadingHyperlinkListener(Component source) {
            this.source = source;
        }
        
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == EventType.ACTIVATED) {
                String linkDescription = e.getDescription();
                if ("#download".equals(linkDescription)) {
                    downloadAction.actionPerformed(new ActionEvent(source,
                            ActionEvent.ACTION_PERFORMED, e.getDescription()));
                } else if ("#downloading".equals(linkDescription)) {
                    mainDownloadPanel.selectAndScrollTo(vsr.getUrn());
                } else if ("#library".equals(linkDescription)) {
                    libraryMediator.selectInLibrary(vsr.getUrn());
                }
            }
        }
    }
}
