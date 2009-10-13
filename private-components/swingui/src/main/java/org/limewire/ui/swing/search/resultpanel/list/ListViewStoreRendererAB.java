package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.core.api.search.store.StoreStyle.Type;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.resultpanel.HeadingFontWidthResolver;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilder;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator.FontWidthResolver;
import org.limewire.ui.swing.search.resultpanel.classic.StoreRendererResourceManager;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRenderer.NoDancingHtmlLabel;
import org.limewire.ui.swing.search.store.StoreController;
import org.limewire.ui.swing.util.CategoryIconManager;

import com.google.inject.Provider;

/**
 * Implementation of ListViewStoreRenderer for styles A and B.
 */
class ListViewStoreRendererAB extends ListViewStoreRenderer {

    private JButton albumCoverButton;
    
    private HTMLLabel albumHeadingLabel;
    private JLabel albumSubHeadingLabel;
    private FontWidthResolver albumWidthResolver;
    private int albumHeadingWidth;
    
    private JXPanel albumInfoPanel;
    private JButton albumTracksButton;
    private JButton albumInfoButton;
    
    private JXPanel albumDownloadPanel;
    private JLabel albumPriceLabel;
    private JButton albumStreamButton;
    private JButton albumDownloadButton;
    
    private JButton mediaIconButton;
    
    private HTMLLabel mediaHeadingLabel;
    private JLabel mediaSubHeadingLabel;
    private FontWidthResolver mediaWidthResolver;
    private int mediaHeadingWidth;
    
    private JLabel mediaPriceLabel;
    private JButton mediaStreamButton;
    private JButton mediaDownloadButton;
    private JButton mediaInfoButton;
    
    /**
     * Constructs a store renderer with the specified store style and services.
     */
    public ListViewStoreRendererAB(StoreStyle storeStyle,
            CategoryIconManager categoryIconManager,
            Provider<SearchHeadingDocumentBuilder> headingBuilder,
            Provider<SearchResultTruncator> headingTruncator,
            LibraryMediator libraryMediator,
            MainDownloadPanel mainDownloadPanel,
            StoreRendererResourceManager storeResourceManager,
            MousePopupListener popupListener,
            StoreController storeController) {
        super(storeStyle, categoryIconManager, headingBuilder, headingTruncator,
                libraryMediator, mainDownloadPanel, storeResourceManager,
                popupListener, storeController);
    }

    @Override
    protected void initAlbumComponent() {
        albumPanel.setOpaque(false);
        
        albumCoverButton = new IconButton(downloadAction);
        
        JXPanel albumTextPanel = new JXPanel();
        albumTextPanel.setOpaque(false);
        
        // Create heading label.  We override paint to cache heading width.
        albumHeadingLabel = new HTMLLabel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                albumHeadingWidth = getSize().width;
            }
        };
        albumHeadingLabel.setOpenUrlsNatively(false);
        albumHeadingLabel.setOpaque(false);
        albumHeadingLabel.setFocusable(false);
        albumHeadingLabel.setMargin(new Insets(2, 0, 2, 3));
        albumHeadingLabel.setMinimumSize(new Dimension(0, 22));
        albumHeadingLabel.addHyperlinkListener(new HeadingHyperlinkListener(albumHeadingLabel));
        installPopupListener(albumHeadingLabel);
        
        albumSubHeadingLabel = new NoDancingHtmlLabel();
        
        albumInfoPanel = new JXPanel();
        albumInfoPanel.setOpaque(false);
        
        albumTracksButton = new IconButton(showTracksAction);
        albumTracksButton.setHideActionText(false);
        
        albumInfoButton = new IconButton(showInfoAction);
        albumInfoButton.setHideActionText(false);
        
        albumDownloadPanel = new JXPanel();
        albumDownloadPanel.setOpaque(false);
        
        albumPriceLabel = new JLabel();
        
        albumStreamButton = new IconButton(streamAction);
        
        albumDownloadButton = new IconButton(downloadAction);
        
        applyAlbumStyle();
        
        // Layout album text components.
        albumTextPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, nogrid, novisualpadding"));
        albumTextPanel.add(albumHeadingLabel, "left, growx, pushx 200, hidemode 3, wrap");
        albumTextPanel.add(albumSubHeadingLabel, "left, growx, hidemode 3");
        
        // Layout album info buttons.
        albumInfoPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        albumInfoPanel.add(albumTracksButton, "alignx right, aligny top, gapright 9, shrinkprio 0, growprio 0");
        albumInfoPanel.add(albumInfoButton, "alignx right, aligny top, shrinkprio 0, growprio 0");
        
        // Layout album download components.
        albumDownloadPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        if (storeStyle.getType() == Type.STYLE_A) {
            albumDownloadPanel.add(albumPriceLabel, "alignx right, aligny 50%, gapright 8");
        } else {
            albumDownloadPanel.add(albumStreamButton, "alignx right, aligny top, gapright 4, hidemode 3");
        }
        albumDownloadPanel.add(albumDownloadButton, "alignx right, aligny 50%, gapright 30");
        
        // Layout components in container.
        albumPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        albumPanel.add(albumCoverButton, "spany, alignx left, aligny 50%, shrinkprio 0, growprio 0");
        if (storeStyle.getType() == Type.STYLE_A) {
            albumPanel.add(albumStreamButton, "spany, alignx left, aligny 50%, gapleft 6, shrinkprio 0, growprio 0, hidemode 3");
        }
        albumPanel.add(albumTextPanel, "spany, alignx left, aligny 50%, gapleft 6, growx, shrinkprio 200, growprio 200, pushx 200");
        albumPanel.add(albumInfoPanel, "alignx right, aligny top, gapleft 4, shrinkprio 0, growprio 0, wrap");
        albumPanel.add(albumDownloadPanel, "alignx right, aligny 50%, gaptop 8, gapleft 4, shrinkprio 0, growprio 0");
    }

    @Override
    protected void initMediaComponent() {
        mediaPanel.setOpaque(false);
        
        mediaIconButton = new IconButton(downloadAction);
        
        JXPanel mediaTextPanel = new JXPanel();
        mediaTextPanel.setOpaque(false);
        
        // Create heading label.  We override paint to cache heading width.
        mediaHeadingLabel = new HTMLLabel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                mediaHeadingWidth = getSize().width;
            }
        };
        mediaHeadingLabel.setOpenUrlsNatively(false);
        mediaHeadingLabel.setOpaque(false);
        mediaHeadingLabel.setFocusable(false);
        mediaHeadingLabel.setMargin(new Insets(2, 0, 2, 3));
        mediaHeadingLabel.setMinimumSize(new Dimension(0, 22));
        mediaHeadingLabel.addHyperlinkListener(new HeadingHyperlinkListener(mediaHeadingLabel));
        installPopupListener(mediaHeadingLabel);
        
        mediaSubHeadingLabel = new NoDancingHtmlLabel();
        
        mediaPriceLabel = new JLabel();
        
        mediaStreamButton = new IconButton(streamAction);
        
        mediaDownloadButton = new IconButton(downloadAction);
        
        mediaInfoButton = new IconButton(showInfoAction);
        mediaInfoButton.setHideActionText(false);
        
        applyMediaStyle();
        
        // Layout media text components.
        mediaTextPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, nogrid, novisualpadding"));
        mediaTextPanel.add(mediaHeadingLabel, "left, growx, pushx 200, hidemode 3, wrap");
        mediaTextPanel.add(mediaSubHeadingLabel, "left, growx, hidemode 3");
        
        // Layout components in container.
        mediaPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        if (storeStyle.getType() == Type.STYLE_A) {
            mediaPanel.add(mediaStreamButton, "alignx left, aligny 50%, shrinkprio 0, growprio 0, hidemode 3");
        } else {
            mediaPanel.add(mediaIconButton, "alignx left, aligny 50%, shrinkprio 0, growprio 0");
        }
        mediaPanel.add(mediaTextPanel, "alignx left, aligny 50%, gapleft 6, growx, shrinkprio 200, growprio 200, pushx 200");
        if (storeStyle.getType() == Type.STYLE_A) {
            mediaPanel.add(mediaPriceLabel, "alignx right, aligny 50%, gapleft 6, gapright 6, shrinkprio 0, growprio 0");
        } else {
            mediaPanel.add(mediaStreamButton, "alignx right, aligny 40%, gapleft 6, gapright 6, shrinkprio 0, growprio 0, hidemode 3");
        }
        mediaPanel.add(mediaDownloadButton, "alignx right, aligny 50%, gapright 12, shrinkprio 0, growprio 0");
        mediaPanel.add(mediaInfoButton, "alignx right, aligny top, shrinkprio 0, growprio 0");
    }
    
    @Override
    protected Component createTrackComponent(TrackResult result) {
        JXPanel trackPanel = new JXPanel();
        trackPanel.setOpaque(true);
        trackPanel.setBackground(Color.WHITE);
        
        JButton streamButton = new IconButton(new StreamTrackAction(result));
        streamButton.setIcon(storeStyle.getStreamIcon());
        
        JButton downloadButton = new IconButton(new DownloadTrackAction(result));
        if (storeController.isPayAsYouGo()) {
            downloadButton.setIcon(storeStyle.getBuyTrackIcon());
        } else {
            downloadButton.setIcon(storeStyle.getDownloadTrackIcon());
        }
        
        JLabel trackLabel = new JLabel();
        trackLabel.setFont(storeStyle.getTrackFont());
        trackLabel.setForeground(storeStyle.getTrackForeground());
        trackLabel.setText((String) result.getProperty(FilePropertyKey.TITLE));
        
        JLabel priceLabel = new JLabel();
        priceLabel.setFont(storeStyle.getPriceFont());
        priceLabel.setForeground(storeStyle.getPriceForeground());
        priceLabel.setText(result.getPrice());
        
        // Layout components in container.
        trackPanel.setLayout(new MigLayout("insets 4 6 4 6, gap 0! 0!, novisualpadding"));
        if (storeStyle.getType() == Type.STYLE_A) {
            trackPanel.add(streamButton, "alignx left, gapright 6, growprio 0, shrinkprio 0, hidemode 3");
        }
        trackPanel.add(trackLabel, "alignx left, growx, growprio 200, shrinkprio 200, pushx 200");
        if (storeStyle.getType() == Type.STYLE_A) {
            trackPanel.add(priceLabel, "alignx right, gapright 12, growprio 0, shrinkprio 0");
        } else {
            trackPanel.add(streamButton, "alignx right, aligny top, gapright 6, growprio 0, shrinkprio 0, hidemode 3");
        }
        trackPanel.add(downloadButton, "alignx right, growprio 0, shrinkprio 0");
        
        // Apply style to show/hide components.
        streamButton.setVisible(storeStyle.isStreamButtonVisible());
        downloadButton.setVisible(storeStyle.isDownloadButtonVisible());
        priceLabel.setVisible(storeStyle.isPriceVisible());
        
        return trackPanel;
    }
    
    @Override
    protected void updateAlbum(VisualStoreResult vsr, RowDisplayResult rowResult, boolean editing) {
        // Set album cover.
        albumCoverButton.setIcon(vsr.getStoreResult().getAlbumIcon());
        
        // Update subheading visibility.
        switch (rowResult.getConfig()) {
        case HeadingOnly:
            albumSubHeadingLabel.setVisible(false);
            break;
        case HeadingAndSubheading:
            albumSubHeadingLabel.setVisible(true);
            break;
        case HeadingAndMetadata:
            albumSubHeadingLabel.setVisible(false);
            break;
        case HeadingSubHeadingAndMetadata:
        default:
            albumSubHeadingLabel.setVisible(true);
            break;
        }
        
        // Set price field.
        albumPriceLabel.setText(vsr.getStoreResult().getPrice());
        
        // Tracks and Info buttons may be hidden when not editing.
        albumTracksButton.setVisible(!storeStyle.isShowTracksOnHover() || editing);
        albumInfoButton.setVisible(!storeStyle.isShowInfoOnHover() || editing);
        
        // Apply style to show/hide components.
        albumStreamButton.setVisible(storeStyle.isStreamButtonVisible());
        albumDownloadButton.setVisible(storeStyle.isDownloadButtonVisible());
        albumPriceLabel.setVisible(storeStyle.isPriceVisible());
        
        if (storeController.isPayAsYouGo()) {
            albumDownloadButton.setIcon(storeStyle.getBuyAlbumIcon());
        } else {
            albumDownloadButton.setIcon(storeStyle.getDownloadAlbumIcon());
        }
        
        // Set text fields.
        if (editing && (albumHeadingWidth == 0)) {
            albumHeadingWidth = calcAlbumHeadingWidth();
        }
        albumHeadingLabel.setText(getHeadingHtml(rowResult, albumWidthResolver, albumHeadingWidth, editing));
        albumHeadingLabel.setToolTipText(HTML_BEGIN + rowResult.getHeading() + HTML_END);
        albumSubHeadingLabel.setText(rowResult.getSubheading());
    }
    
    @Override
    protected void updateMedia(VisualStoreResult vsr, RowDisplayResult rowResult, boolean editing) {
        // Set category icon.
        mediaIconButton.setIcon(getIcon(vsr));
        
        // Update subheading visibility.
        switch (rowResult.getConfig()) {
        case HeadingOnly:
            mediaSubHeadingLabel.setVisible(false);
            break;
        case HeadingAndSubheading:
            mediaSubHeadingLabel.setVisible(true);
            break;
        case HeadingAndMetadata:
            mediaSubHeadingLabel.setVisible(false);
            break;
        case HeadingSubHeadingAndMetadata:
        default:
            mediaSubHeadingLabel.setVisible(true);
            break;
        }
        
        // Set price field.
        mediaPriceLabel.setText(vsr.getStoreResult().getPrice());
        
        // Info button may be hidden when not editing.
        mediaInfoButton.setVisible(!storeStyle.isShowInfoOnHover() || editing);
        
        // Apply style to show/hide components.
        mediaStreamButton.setVisible(storeStyle.isStreamButtonVisible());
        mediaDownloadButton.setVisible(storeStyle.isDownloadButtonVisible());
        mediaPriceLabel.setVisible(storeStyle.isPriceVisible());
        
        if (storeController.isPayAsYouGo()) {
            mediaDownloadButton.setIcon(storeStyle.getBuyTrackIcon());
        } else {
            mediaDownloadButton.setIcon(storeStyle.getDownloadTrackIcon());
        }
        
        // Set text fields.
        if (editing && (mediaHeadingWidth == 0)) {
            mediaHeadingWidth = calcMediaHeadingWidth();
        }
        mediaHeadingLabel.setText(getHeadingHtml(rowResult, mediaWidthResolver, mediaHeadingWidth, editing));
        mediaHeadingLabel.setToolTipText(HTML_BEGIN + rowResult.getHeading() + HTML_END);
        mediaSubHeadingLabel.setText(rowResult.getSubheading());
    }
    
    @Override
    protected void applyStyle() {
        applyAlbumStyle();
        applyMediaStyle();
    }
    
    /**
     * Applies the current style to the album components.
     */
    private void applyAlbumStyle() {
        albumHeadingLabel.setHtmlFont(storeStyle.getHeadingFont());
        albumHeadingLabel.setHtmlForeground(storeStyle.getHeadingForeground());
        albumHeadingLabel.setHtmlLinkForeground(storeStyle.getHeadingForeground());
        
        albumSubHeadingLabel.setFont(storeStyle.getSubHeadingFont());
        albumSubHeadingLabel.setForeground(storeStyle.getSubHeadingForeground());
        
        albumWidthResolver = new HeadingFontWidthResolver(albumHeadingLabel, storeStyle.getHeadingFont());
        
        albumTracksButton.setFont(storeStyle.getShowTracksFont());
        albumTracksButton.setForeground(storeStyle.getShowTracksForeground());
        
        albumInfoButton.setFont(storeStyle.getInfoFont());
        albumInfoButton.setForeground(storeStyle.getInfoForeground());
        
        albumPriceLabel.setFont(storeStyle.getPriceFont());
        albumPriceLabel.setForeground(storeStyle.getPriceForeground());
        
        albumStreamButton.setIcon(storeStyle.getStreamIcon());
    }
    
    /**
     * Applies the current style to the media components.
     */
    private void applyMediaStyle() {
        mediaHeadingLabel.setHtmlFont(storeStyle.getHeadingFont());
        mediaHeadingLabel.setHtmlForeground(storeStyle.getHeadingForeground());
        mediaHeadingLabel.setHtmlLinkForeground(storeStyle.getHeadingForeground());
        
        mediaSubHeadingLabel.setFont(storeStyle.getSubHeadingFont());
        mediaSubHeadingLabel.setForeground(storeStyle.getSubHeadingForeground());
        
        mediaWidthResolver = new HeadingFontWidthResolver(mediaHeadingLabel, storeStyle.getHeadingFont());
        
        mediaPriceLabel.setFont(storeStyle.getPriceFont());
        mediaPriceLabel.setForeground(storeStyle.getPriceForeground());
        
        mediaStreamButton.setIcon(storeStyle.getStreamIcon());
        
        mediaInfoButton.setFont(storeStyle.getInfoFont());
        mediaInfoButton.setForeground(storeStyle.getInfoForeground());
    }
    
    /**
     * Returns the estimated album heading width.  The width depends on the 
     * specific component layout for the renderer.
     */
    private int calcAlbumHeadingWidth() {
        // Apply cell dimensions to component and perform layout.
        Rectangle cellRect = getCellRect();
        setSize(cellRect.width, cellRect.height);
        doLayout();
        
        // Compute heading width by subtracting gap sizes and button widths.
        if (storeStyle.getType() == Type.STYLE_A) {
            return getWidth() - 28 - albumCoverButton.getWidth() - albumStreamButton.getWidth() - 
                Math.max(albumInfoPanel.getWidth(), albumDownloadPanel.getWidth());
        } else {
            return getWidth() - 22 - albumCoverButton.getWidth() - 
                Math.max(albumInfoPanel.getWidth(), albumDownloadPanel.getWidth());
        }
    }
    
    /**
     * Returns the estimated media heading width.  The width depends on the 
     * specific component layout for the renderer.
     */
    private int calcMediaHeadingWidth() {
        // Apply cell dimensions to component and perform layout.
        Rectangle cellRect = getCellRect();
        setSize(cellRect.width, cellRect.height);
        doLayout();
        
        // Compute heading width by subtracting gap sizes and button widths.
        if (storeStyle.getType() == Type.STYLE_A) {
            return getWidth() - 42 - mediaStreamButton.getWidth() - mediaPriceLabel.getWidth() - 
                mediaDownloadButton.getWidth() - mediaInfoButton.getWidth();
        } else {
            return getWidth() - 42 - mediaIconButton.getWidth() - mediaStreamButton.getWidth() -
                mediaDownloadButton.getWidth() - mediaInfoButton.getWidth();
        }
    }
}
