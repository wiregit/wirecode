package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
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
import org.limewire.util.CommonUtils;

import com.google.inject.Provider;

/**
 * Implementation of ListViewStoreRenderer for styles C and D.
 */
class ListViewStoreRendererCD extends ListViewStoreRenderer {

    private JButton albumCoverButton;
    
    private HTMLLabel albumHeadingLabel;
    private JLabel albumSubHeadingLabel;
    private FontWidthResolver albumWidthResolver;
    private int albumHeadingWidth;
    
    private JButton albumTracksButton;
    private JButton albumStreamButton;
    private IconButton albumDownloadButton;
    private JButton albumPriceButton;
    
    private HTMLLabel mediaHeadingLabel;
    private JLabel mediaSubHeadingLabel;
    private FontWidthResolver mediaWidthResolver;
    private int mediaHeadingWidth;
    
    private JButton mediaStreamButton;
    private IconButton mediaDownloadButton;
    private JButton mediaPriceButton;
    
    /**
     * Constructs a store renderer with the specified store style and services.
     */
    public ListViewStoreRendererCD(StoreStyle storeStyle,
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
        
        JXPanel albumTextPanel = new JXPanel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                if (storeStyle.getType() == Type.STYLE_C) {
                    albumHeadingWidth = getSize().width - albumDownloadButton.getSize().width;
                } else {
                    albumHeadingWidth = getSize().width - albumPriceButton.getSize().width;
                }
            }
        };
        albumTextPanel.setOpaque(false);
        
        JXPanel albumHeadingPanel = new JXPanel();
        albumHeadingPanel.setOpaque(false);
        
        albumHeadingLabel = new HTMLLabel();
        albumHeadingLabel.setOpenUrlsNatively(false);
        albumHeadingLabel.setOpaque(false);
        albumHeadingLabel.setFocusable(false);
        albumHeadingLabel.setMargin(new Insets(2, 0, 2, 3));
        albumHeadingLabel.setMinimumSize(new Dimension(0, 22));
        albumHeadingLabel.addHyperlinkListener(new HeadingHyperlinkListener(albumHeadingLabel));
        installPopupListener(albumHeadingLabel);
        
        albumSubHeadingLabel = new NoDancingHtmlLabel();
        
        albumTracksButton = new IconButton(showTracksAction);
        albumTracksButton.setHideActionText(false);
        
        albumStreamButton = new IconButton(streamAction);
        
        albumDownloadButton = new IconButton();
        albumDownloadButton.removeActionHandListener();
        installPopupListener(albumDownloadButton);
        
        albumPriceButton = new PriceButton();
        installPopupListener(albumPriceButton);
        
        applyAlbumStyle();
        
        // Layout components in container.
        albumHeadingPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, nogrid, novisualpadding, hidemode 3"));
        albumHeadingPanel.add(albumHeadingLabel, "left, aligny 50%, growx"); 
        if (storeStyle.getType() == Type.STYLE_C) {
            albumHeadingPanel.add(albumDownloadButton, "left, aligny 50%");
        } else {
            albumHeadingPanel.add(albumPriceButton, "left, aligny 75%");
        }
        
        albumTextPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, nogrid, novisualpadding"));
        albumTextPanel.add(albumHeadingPanel, "left, aligny 50%, growx, hidemode 3, wrap");
        albumTextPanel.add(albumSubHeadingLabel, "left, aligny 50%, growx, shrinkprio 200, hidemode 3, wrap");
        albumTextPanel.add(albumStreamButton, "left, aligny 50%, gaptop 3, gapright 6, hidemode 3");
        albumTextPanel.add(albumTracksButton, "left, aligny 50%, gaptop 3");
        
        albumPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        albumPanel.add(albumCoverButton, "alignx left, aligny top, shrinkprio 0, growprio 0");
        albumPanel.add(albumTextPanel, "alignx left, aligny 50%, gapleft 9, growx, shrinkprio 200, growprio 200, pushx 200");
    }

    @Override
    protected void initMediaComponent() {
        mediaPanel.setOpaque(false);
        
        JXPanel mediaTextPanel = new JXPanel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                if (storeStyle.getType() == Type.STYLE_C) {
                    mediaHeadingWidth = getSize().width - mediaDownloadButton.getSize().width;
                } else {
                    mediaHeadingWidth = getSize().width - mediaPriceButton.getSize().width;
                }
            }
        };
        mediaTextPanel.setOpaque(false);
        
        JXPanel mediaHeadingPanel = new JXPanel();
        mediaHeadingPanel.setOpaque(false);
        
        mediaHeadingLabel = new HTMLLabel();
        mediaHeadingLabel.setOpenUrlsNatively(false);
        mediaHeadingLabel.setOpaque(false);
        mediaHeadingLabel.setFocusable(false);
        mediaHeadingLabel.setMargin(new Insets(2, 0, 2, 3));
        mediaHeadingLabel.setMinimumSize(new Dimension(0, 22));
        mediaHeadingLabel.addHyperlinkListener(new HeadingHyperlinkListener(mediaHeadingLabel));
        installPopupListener(mediaHeadingLabel);
        
        mediaSubHeadingLabel = new NoDancingHtmlLabel();
        
        mediaStreamButton = new IconButton(streamAction);
        
        mediaDownloadButton = new IconButton();
        mediaDownloadButton.removeActionHandListener();
        installPopupListener(mediaDownloadButton);
        
        mediaPriceButton = new PriceButton();
        installPopupListener(mediaPriceButton);
        
        applyMediaStyle();
        
        // Layout components in container.
        mediaHeadingPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, nogrid, novisualpadding, hidemode 3"));
        mediaHeadingPanel.add(mediaHeadingLabel, "left, aligny 50%, growx, wmax pref");
        if (storeStyle.getType() == Type.STYLE_C) {
            mediaHeadingPanel.add(mediaDownloadButton, "left, aligny 50%");
        } else {
            mediaHeadingPanel.add(mediaPriceButton, "left, aligny 75%");
        }
        
        mediaTextPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, nogrid, novisualpadding"));
        mediaTextPanel.add(mediaHeadingPanel, "left, aligny 50%, growx, hidemode 3, wrap");
        mediaTextPanel.add(mediaSubHeadingLabel, "left, aligny 50%, growx, hidemode 3");
        
        mediaPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        mediaPanel.add(mediaStreamButton, "alignx left, aligny 50%, shrinkprio 0, growprio 0, hidemode 3");
        mediaPanel.add(mediaTextPanel, "alignx left, aligny 50%, gapleft 6, growx, shrinkprio 200, growprio 200, pushx 200");
    }

    @Override
    protected Component createTrackComponent(TrackResult result) {
        JXPanel trackPanel = new JXPanel();
        trackPanel.setOpaque(true);
        trackPanel.setBackground(Color.WHITE);
        
        JButton streamButton = new IconButton(new StreamTrackAction(result));
        streamButton.setIcon(storeStyle.getStreamIcon());
        
        IconButton downloadButton = new IconButton();
        downloadButton.removeActionHandListener();
        if (storeController.isPayAsYouGo()) {
            downloadButton.setIcon(storeStyle.getBuyTrackIcon());
        } else {
            downloadButton.setIcon(storeStyle.getDownloadTrackIcon());
        }
        
        JButton priceButton = new PriceButton();
        priceButton.setText(result.getPrice());
        
        JLabel trackLabel = new JLabel();
        trackLabel.setFont(storeStyle.getTrackFont());
        trackLabel.setForeground(storeStyle.getTrackForeground());
        trackLabel.setText((String) result.getProperty(FilePropertyKey.TITLE));
        
        JLabel lengthLabel = new JLabel();
        lengthLabel.setFont(storeStyle.getTrackLengthFont());
        lengthLabel.setForeground(storeStyle.getTrackLengthForeground());
        lengthLabel.setText(CommonUtils.seconds2time((Long) result.getProperty(FilePropertyKey.LENGTH)));
        
        // Layout components in container.
        trackPanel.setLayout(new MigLayout("insets 6 6 6 6, gap 0! 0!, novisualpadding"));
        trackPanel.add(streamButton, "alignx left, aligny 50%, gapright 6, growprio 0, shrinkprio 0, hidemode 3");
        trackPanel.add(trackLabel, "alignx left, aligny 50%, growx, growprio 200, shrinkprio 200");
        if (storeStyle.getType() == Type.STYLE_C) {
            trackPanel.add(downloadButton, "alignx left, aligny 50%, gapleft 6, growprio 0, shrinkprio 0");
        } else {
            trackPanel.add(priceButton, "alignx left, aligny 50%, gapleft 6, growprio 0, shrinkprio 0");
        }
        trackPanel.add(lengthLabel, "alignx right, aligny 50%, growprio 0, shrinkprio 0, pushx 200");
        
        // Apply style to show/hide components.
        streamButton.setVisible(storeStyle.isStreamButtonVisible());
        downloadButton.setVisible(storeStyle.isDownloadButtonVisible());
        priceButton.setVisible(storeStyle.isPriceVisible());
        
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
        albumPriceButton.setText(vsr.getStoreResult().getPrice());
        
        // Apply style to show/hide components.
        albumStreamButton.setVisible(storeStyle.isStreamButtonVisible());
        albumTracksButton.setVisible(!storeStyle.isShowTracksOnHover() || editing);
        albumDownloadButton.setVisible(storeStyle.isDownloadButtonVisible());
        albumPriceButton.setVisible(storeStyle.isPriceVisible());
        
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
        mediaPriceButton.setText(vsr.getStoreResult().getPrice());
        
        // Apply style to show/hide components.
        mediaStreamButton.setVisible(storeStyle.isStreamButtonVisible());
        mediaDownloadButton.setVisible(storeStyle.isDownloadButtonVisible());
        mediaPriceButton.setVisible(storeStyle.isPriceVisible());
        
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
        
        mediaStreamButton.setIcon(storeStyle.getStreamIcon());
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
        if (storeStyle.getType() == Type.STYLE_C) {
            return getWidth() - 21 - albumCoverButton.getWidth() - albumDownloadButton.getWidth();
        } else {
            return getWidth() - 21 - albumCoverButton.getWidth() - albumPriceButton.getWidth();
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
        if (storeStyle.getType() == Type.STYLE_C) {
            return getWidth() - 18 - mediaStreamButton.getWidth() - mediaDownloadButton.getWidth();
        } else {
            return getWidth() - 18 - mediaStreamButton.getWidth() - mediaPriceButton.getWidth();
        }
    }
    
    /**
     * A button that displays the price and downloads the file.
     */
    private class PriceButton extends JXButton {
        
        public PriceButton() {
            super();
            
            setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
            setFocusPainted(false);
            setFont(storeStyle.getPriceFont());
            setForeground(storeStyle.getPriceForeground());
            
            if (storeStyle.isPriceButtonVisible()) {
                setContentAreaFilled(true);
                setBackgroundPainter(new RectanglePainter<JXButton>(0, 0, 0, 0, 15, 15, true,
                        storeStyle.getPriceBackground(), 1.0f, storeStyle.getPriceBorderColor()));
            } else {
                setContentAreaFilled(false);
            }
        }
    }
}
