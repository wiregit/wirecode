package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreTrackResult;
import org.limewire.core.api.search.store.StoreStyle.Type;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.RolloverCursorListener;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.resultpanel.HeadingFontWidthResolver;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilder;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator.FontWidthResolver;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRenderer.NoDancingHtmlLabel;
import org.limewire.ui.swing.util.CategoryIconManager;

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
    private JButton albumDownloadButton;
    private JButton albumPriceButton;
    
    private HTMLLabel mediaHeadingLabel;
    private JLabel mediaSubHeadingLabel;
    private FontWidthResolver mediaWidthResolver;
    private int mediaHeadingWidth;
    
    private JButton mediaStreamButton;
    private JButton mediaDownloadButton;
    private JButton mediaPriceButton;
    
    /**
     * Constructs a store renderer with the specified icon manager and store
     * style.
     */
    public ListViewStoreRendererCD(CategoryIconManager categoryIconManager,
            Provider<SearchHeadingDocumentBuilder> headingBuilder,
            Provider<SearchResultTruncator> headingTruncator,
            StoreStyle storeStyle) {
        super(categoryIconManager, headingBuilder, headingTruncator, storeStyle);
    }

    @Override
    protected void initAlbumComponent() {
        albumPanel.setOpaque(false);
        
        albumCoverButton = new IconButton();
        
        JXPanel albumTextPanel = new JXPanel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                if (albumDownloadButton.isVisible()) {
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
        albumHeadingLabel.setHtmlFont(storeStyle.getArtistFont());
        albumHeadingLabel.setHtmlForeground(storeStyle.getArtistForeground());
        albumHeadingLabel.setHtmlLinkForeground(storeStyle.getArtistForeground());
        albumHeadingLabel.setMargin(new Insets(3, 0, 3, 3));
        albumHeadingLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        albumHeadingLabel.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == EventType.ACTIVATED) {
                    if (e.getDescription().equals("#download")) {
                        downloadAction.actionPerformed(new ActionEvent(
                                mediaHeadingLabel, ActionEvent.ACTION_PERFORMED, e.getDescription()));
                    }
                }
            }
        });
        
        albumSubHeadingLabel = new NoDancingHtmlLabel();
        albumSubHeadingLabel.setFont(storeStyle.getAlbumFont());
        albumSubHeadingLabel.setForeground(storeStyle.getAlbumForeground());
        
        albumWidthResolver = new HeadingFontWidthResolver(albumHeadingLabel, storeStyle.getArtistFont());
        
        albumTracksButton = new IconButton(showTracksAction);
        albumTracksButton.setFont(storeStyle.getShowTracksFont());
        albumTracksButton.setForeground(storeStyle.getShowTracksForeground());
        albumTracksButton.setHideActionText(false);
        
        albumStreamButton = new IconButton(streamAction);
        albumStreamButton.setIcon(storeStyle.getStreamIcon());
        
        albumDownloadButton = new IconButton(downloadAction);
        albumDownloadButton.setIcon(storeStyle.getDownloadAlbumIcon());
        
        albumPriceButton = new PriceButton(downloadAction);
        
        // Layout components in container.
        albumHeadingPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, nogrid, novisualpadding"));
        albumHeadingPanel.add(albumHeadingLabel, "left, aligny 50%, growx, hidemode 3"); 
        if (storeStyle.getType() == Type.STYLE_C) {
            albumHeadingPanel.add(albumDownloadButton, "left, aligny 50%, hidemode 3");
        } else {
            albumHeadingPanel.add(albumPriceButton, "left, aligny 75%, hidemode 3");
        }
        
        albumTextPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, nogrid, novisualpadding"));
        albumTextPanel.add(albumHeadingPanel, "left, aligny 50%, growx, hidemode 3, wrap");
        albumTextPanel.add(albumSubHeadingLabel, "left, aligny 50%, growx, shrinkprio 200, hidemode 3, wrap");
        albumTextPanel.add(albumStreamButton, "left, aligny 50%, gaptop 3, hidemode 3");
        albumTextPanel.add(albumTracksButton, "left, aligny 50%, gaptop 3, gapleft 6, hidemode 3");
        
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
                if (mediaDownloadButton.isVisible()) {
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
        mediaHeadingLabel.setHtmlFont(storeStyle.getArtistFont());
        mediaHeadingLabel.setHtmlForeground(storeStyle.getArtistForeground());
        mediaHeadingLabel.setHtmlLinkForeground(storeStyle.getArtistForeground());
        mediaHeadingLabel.setMargin(new Insets(3, 0, 3, 3));
        mediaHeadingLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        mediaHeadingLabel.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == EventType.ACTIVATED) {
                    if (e.getDescription().equals("#download")) {
                        downloadAction.actionPerformed(new ActionEvent(
                                mediaHeadingLabel, ActionEvent.ACTION_PERFORMED, e.getDescription()));
                    }
                }
            }
        });
        
        mediaSubHeadingLabel = new NoDancingHtmlLabel();
        mediaSubHeadingLabel.setFont(storeStyle.getAlbumFont());
        mediaSubHeadingLabel.setForeground(storeStyle.getAlbumForeground());
        
        mediaWidthResolver = new HeadingFontWidthResolver(mediaHeadingLabel, storeStyle.getArtistFont());
        
        mediaStreamButton = new IconButton(streamAction);
        mediaStreamButton.setIcon(storeStyle.getStreamIcon());
        
        mediaDownloadButton = new IconButton(downloadAction);
        mediaDownloadButton.setIcon(storeStyle.getDownloadTrackIcon());
        
        mediaPriceButton = new PriceButton(downloadAction);
        
        // Layout components in container.
        mediaHeadingPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, nogrid, novisualpadding"));
        mediaHeadingPanel.add(mediaHeadingLabel, "left, aligny 50%, growx, wmax pref, hidemode 3");
        if (storeStyle.getType() == Type.STYLE_C) {
            mediaHeadingPanel.add(mediaDownloadButton, "left, aligny 50%, hidemode 3");
        } else {
            mediaHeadingPanel.add(mediaPriceButton, "left, aligny 75%, hidemode 3");
        }
        
        mediaTextPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, nogrid, novisualpadding"));
        mediaTextPanel.add(mediaHeadingPanel, "left, aligny 50%, growx, hidemode 3, wrap");
        mediaTextPanel.add(mediaSubHeadingLabel, "left, aligny 50%, growx, hidemode 3");
        
        mediaPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        mediaPanel.add(mediaStreamButton, "alignx left, aligny 50%, shrinkprio 0, growprio 0");
        mediaPanel.add(mediaTextPanel, "alignx left, aligny 50%, gapleft 6, growx, shrinkprio 200, growprio 200, pushx 200");
    }

    @Override
    protected Component createTrackComponent(StoreTrackResult result) {
        JXPanel trackPanel = new JXPanel();
        trackPanel.setOpaque(true);
        trackPanel.setBackground(Color.WHITE);
        
        JButton streamButton = new IconButton(new StreamTrackAction(result));
        streamButton.setIcon(storeStyle.getStreamIcon());
        
        JButton downloadButton = new IconButton(new DownloadTrackAction(result));
        downloadButton.setIcon(storeStyle.getDownloadTrackIcon());
        
        JButton priceButton = new PriceButton(new DownloadTrackAction(result));
        priceButton.setText(result.getPrice());
        
        JLabel trackLabel = new JLabel();
        trackLabel.setFont(storeStyle.getTrackFont());
        trackLabel.setForeground(storeStyle.getTrackForeground());
        trackLabel.setText((String) result.getProperty(FilePropertyKey.NAME));
        
        JLabel lengthLabel = new JLabel();
        lengthLabel.setFont(storeStyle.getTrackLengthFont());
        lengthLabel.setForeground(storeStyle.getTrackLengthForeground());
        lengthLabel.setText("2:30"); // TODO convert from FilePropertyKey.LENGTH
        
        // Layout components in container.
        trackPanel.setLayout(new MigLayout("insets 6 6 6 6, gap 0! 0!, novisualpadding"));
        trackPanel.add(streamButton, "alignx left, gapright 6, growprio 0, shrinkprio 0");
        trackPanel.add(trackLabel, "alignx left, growx, growprio 200, shrinkprio 200");
        if (storeStyle.getType() == Type.STYLE_C) {
            trackPanel.add(downloadButton, "alignx left, gapleft 6, growprio 0, shrinkprio 0");
        } else {
            trackPanel.add(priceButton, "alignx left, gapleft 6, growprio 0, shrinkprio 0");
        }
        trackPanel.add(lengthLabel, "alignx right, growprio 0, shrinkprio 0, pushx 200");
        
        //System.out.println("prefSize=" + trackPanel.getPreferredSize()); // TODO REMOVE
        
        // Apply style to show/hide components.
        downloadButton.setVisible(storeStyle.isDownloadButtonVisible());
        priceButton.setVisible(storeStyle.isPriceVisible());
        
        return trackPanel;
    }

    @Override
    protected void updateAlbum(VisualStoreResult vsr, RowDisplayResult result, boolean editing) {
        albumCoverButton.setIcon(vsr.getStoreResult().getAlbumIcon());
        
        switch (result.getConfig()) {
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
        
        albumHeadingLabel.setText(getHeadingHtml(albumWidthResolver, getAlbumHeadingWidth(), editing));
        albumHeadingLabel.setToolTipText(result.getHeading());
        albumSubHeadingLabel.setText(result.getSubheading());
        
        albumPriceButton.setText(vsr.getStoreResult().getPrice());
        
        // Apply style to show/hide components.
        albumTracksButton.setVisible(!storeStyle.isShowTracksOnHover() || editing);
        albumDownloadButton.setVisible(storeStyle.isDownloadButtonVisible());
        albumPriceButton.setVisible(storeStyle.isPriceVisible());
    }

    @Override
    protected void updateMedia(VisualStoreResult vsr, RowDisplayResult result, boolean editing) {
        
        switch (result.getConfig()) {
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
        
        mediaHeadingLabel.setText(getHeadingHtml(mediaWidthResolver, getMediaHeadingWidth(), editing));
        mediaHeadingLabel.setToolTipText(result.getHeading());
        mediaSubHeadingLabel.setText(result.getSubheading());
        
        mediaPriceButton.setText(vsr.getStoreResult().getPrice());
        
        // Apply style to show/hide components.
        mediaDownloadButton.setVisible(storeStyle.isDownloadButtonVisible());
        mediaPriceButton.setVisible(storeStyle.isPriceVisible());
    }
    
    /**
     * Returns the estimated album heading width.
     */
    private int getAlbumHeadingWidth() {
        if (albumHeadingWidth > 0) {
            return albumHeadingWidth;
            
        } else {
            // Estimate heading width based on column width.
            int columnWidth = getColumnWidth();
            int coverWidth = albumCoverButton.getIcon().getIconWidth();
            if (albumDownloadButton.isVisible()) {
                return columnWidth - 21 - coverWidth - albumDownloadButton.getSize().width;
            } else {
                return columnWidth - 21 - coverWidth - albumPriceButton.getSize().width;
            }
        }
    }
    
    /**
     * Returns the estimated media heading width.
     */
    private int getMediaHeadingWidth() {
        if (mediaHeadingWidth > 0) {
            return mediaHeadingWidth;
            
        } else {
            // Estimate heading width based on column width.
            int columnWidth = getColumnWidth();
            int streamWidth = mediaStreamButton.getSize().width;
            if (mediaDownloadButton.isVisible()) {
                return columnWidth - 18 - streamWidth - mediaDownloadButton.getSize().width;
            } else {
                return columnWidth - 18 - streamWidth - mediaPriceButton.getSize().width;
            }
        }
    }
    
    /**
     * A button that displays the price and downloads the file.
     */
    private class PriceButton extends JXButton {
        
        public PriceButton(Action action) {
            super(action);
            
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
            
            // Install listener to show hand cursor.
            new RolloverCursorListener().install(this);
        }
    }
}