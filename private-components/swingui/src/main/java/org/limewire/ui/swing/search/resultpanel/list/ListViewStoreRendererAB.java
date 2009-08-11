package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreTrackResult;
import org.limewire.core.api.search.store.StoreStyle.Type;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.IconButton;
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
     * Constructs a store renderer with the specified icon manager and store
     * style.
     */
    public ListViewStoreRendererAB(CategoryIconManager categoryIconManager,
            Provider<SearchHeadingDocumentBuilder> headingBuilder,
            Provider<SearchResultTruncator> headingTruncator,
            StoreStyle storeStyle) {
        super(categoryIconManager, headingBuilder, headingTruncator, storeStyle);
    }

    @Override
    protected void initAlbumComponent() {
        albumPanel.setOpaque(false);
        
        albumCoverButton = new IconButton();
        
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
                                albumHeadingLabel, ActionEvent.ACTION_PERFORMED, e.getDescription()));
                    }
                }
            }
        });
        
        albumSubHeadingLabel = new NoDancingHtmlLabel();
        albumSubHeadingLabel.setFont(storeStyle.getAlbumFont());
        albumSubHeadingLabel.setForeground(storeStyle.getAlbumForeground());
        
        albumWidthResolver = new HeadingFontWidthResolver(albumHeadingLabel, storeStyle.getArtistFont());
        
        albumInfoPanel = new JXPanel();
        albumInfoPanel.setOpaque(false);
        
        albumTracksButton = new IconButton(showTracksAction);
        albumTracksButton.setFont(storeStyle.getShowTracksFont());
        albumTracksButton.setForeground(storeStyle.getShowTracksForeground());
        albumTracksButton.setHideActionText(false);
        
        albumInfoButton = new IconButton(showInfoAction);
        albumInfoButton.setFont(storeStyle.getInfoFont());
        albumInfoButton.setForeground(storeStyle.getInfoForeground());
        albumInfoButton.setHideActionText(false);
        
        albumDownloadPanel = new JXPanel();
        albumDownloadPanel.setOpaque(false);
        
        albumPriceLabel = new JLabel();
        albumPriceLabel.setFont(storeStyle.getPriceFont());
        albumPriceLabel.setForeground(storeStyle.getPriceForeground());
        
        albumStreamButton = new IconButton(streamAction);
        albumStreamButton.setIcon(storeStyle.getStreamIcon());
        
        albumDownloadButton = new IconButton(downloadAction);
        albumDownloadButton.setIcon(storeStyle.getDownloadAlbumIcon());
        
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
            albumDownloadPanel.add(albumStreamButton, "alignx right, aligny 50%, gapright 4");
        }
        albumDownloadPanel.add(albumDownloadButton, "alignx right, aligny 50%, gapright 30");
        
        // Layout components in container.
        albumPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        albumPanel.add(albumCoverButton, "spany, alignx left, aligny 50%, shrinkprio 0, growprio 0");
        if (storeStyle.getType() == Type.STYLE_A) {
            albumPanel.add(albumStreamButton, "spany, alignx left, aligny 50%, gapleft 6, shrinkprio 0, growprio 0");
        }
        albumPanel.add(albumTextPanel, "spany, alignx left, aligny 50%, gapleft 6, growx, shrinkprio 200, growprio 200, pushx 200");
        albumPanel.add(albumInfoPanel, "alignx right, aligny top, gapleft 4, shrinkprio 0, growprio 0, wrap");
        albumPanel.add(albumDownloadPanel, "alignx right, aligny 50%, gaptop 8, gapleft 4, shrinkprio 0, growprio 0");
    }

    @Override
    protected void initMediaComponent() {
        mediaPanel.setOpaque(false);
        
        mediaIconButton = new IconButton();
        
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
        
        mediaPriceLabel = new JLabel();
        mediaPriceLabel.setFont(storeStyle.getPriceFont());
        mediaPriceLabel.setForeground(storeStyle.getPriceForeground());
        
        mediaStreamButton = new IconButton(streamAction);
        mediaStreamButton.setIcon(storeStyle.getStreamIcon());
        
        mediaDownloadButton = new IconButton(downloadAction);
        mediaDownloadButton.setIcon(storeStyle.getDownloadTrackIcon());
        
        mediaInfoButton = new IconButton(showInfoAction);
        mediaInfoButton.setFont(storeStyle.getInfoFont());
        mediaInfoButton.setForeground(storeStyle.getInfoForeground());
        mediaInfoButton.setHideActionText(false);
        
        mediaTextPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, nogrid, novisualpadding"));
        mediaTextPanel.add(mediaHeadingLabel, "left, growx, pushx 200, hidemode 3, wrap");
        mediaTextPanel.add(mediaSubHeadingLabel, "left, growx, hidemode 3");
        
        // Layout components in container.
        mediaPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        if (storeStyle.getType() == Type.STYLE_A) {
            mediaPanel.add(mediaStreamButton, "alignx left, aligny 50%, shrinkprio 0, growprio 0");
        } else {
            mediaPanel.add(mediaIconButton, "alignx left, aligny 50%, shrinkprio 0, growprio 0");
        }
        mediaPanel.add(mediaTextPanel, "alignx left, aligny 50%, gapleft 6, growx, shrinkprio 200, growprio 200, pushx 200");
        if (storeStyle.getType() == Type.STYLE_A) {
            mediaPanel.add(mediaPriceLabel, "alignx right, aligny 50%, gapleft 6, gapright 6, shrinkprio 0, growprio 0");
        } else {
            mediaPanel.add(mediaStreamButton, "alignx right, aligny 50%, gapleft 6, gapright 6, shrinkprio 0, growprio 0");
        }
        mediaPanel.add(mediaDownloadButton, "alignx right, aligny 50%, gapright 12, shrinkprio 0, growprio 0");
        mediaPanel.add(mediaInfoButton, "alignx right, aligny top, shrinkprio 0, growprio 0");
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
        
        JLabel trackLabel = new JLabel();
        trackLabel.setFont(storeStyle.getTrackFont());
        trackLabel.setForeground(storeStyle.getTrackForeground());
        trackLabel.setText((String) result.getProperty(FilePropertyKey.NAME));
        
        JLabel priceLabel = new JLabel();
        priceLabel.setFont(storeStyle.getPriceFont());
        priceLabel.setForeground(storeStyle.getPriceForeground());
        priceLabel.setText(result.getPrice());
        
        // Layout components in container.
        trackPanel.setLayout(new MigLayout("insets 4 6 4 6, gap 0! 0!, novisualpadding"));
        if (storeStyle.getType() == Type.STYLE_A) {
            trackPanel.add(streamButton, "alignx left, gapright 6, growprio 0, shrinkprio 0");
        }
        trackPanel.add(trackLabel, "alignx left, growx, growprio 200, shrinkprio 200, pushx 200");
        if (storeStyle.getType() == Type.STYLE_A) {
            trackPanel.add(priceLabel, "alignx right, gapright 12, growprio 0, shrinkprio 0");
        } else {
            trackPanel.add(streamButton, "alignx right, gapright 6, growprio 0, shrinkprio 0");
        }
        trackPanel.add(downloadButton, "alignx right, growprio 0, shrinkprio 0");
        
        // Apply style to show/hide components.
        downloadButton.setVisible(storeStyle.isDownloadButtonVisible());
        priceLabel.setVisible(storeStyle.isPriceVisible());
        
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
        
        albumHeadingLabel.setText(getHeadingHtml(albumWidthResolver, albumHeadingWidth, editing));
        albumHeadingLabel.setToolTipText(result.getHeading());
        albumSubHeadingLabel.setText(result.getSubheading());
        
        albumPriceLabel.setText(vsr.getStoreResult().getPrice());
        
        // Tracks and Info buttons may be hidden when not editing.
        albumTracksButton.setVisible(!storeStyle.isShowTracksOnHover() || editing);
        albumInfoButton.setVisible(!storeStyle.isShowInfoOnHover() || editing);
        
        // Apply style to show/hide components.
        albumDownloadButton.setVisible(storeStyle.isDownloadButtonVisible());
        albumPriceLabel.setVisible(storeStyle.isPriceVisible());
    }
    
    @Override
    protected void updateMedia(VisualStoreResult vsr, RowDisplayResult result, boolean editing) {
        mediaIconButton.setIcon(categoryIconManager.getIcon(vsr));
        
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
        
        mediaHeadingLabel.setText(getHeadingHtml(mediaWidthResolver, mediaHeadingWidth, editing));
        mediaHeadingLabel.setToolTipText(result.getHeading());
        mediaSubHeadingLabel.setText(result.getSubheading());
        
        mediaPriceLabel.setText(vsr.getStoreResult().getPrice());
        
        // Info button may be hidden when not editing.
        mediaInfoButton.setVisible(!storeStyle.isShowInfoOnHover() || editing);
        
        // Apply style to show/hide components.
        mediaDownloadButton.setVisible(storeStyle.isDownloadButtonVisible());
        mediaPriceLabel.setVisible(storeStyle.isPriceVisible());
    }
}
