package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreStyle.Type;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.search.resultpanel.list.ListViewTableEditorRenderer.NoDancingHtmlLabel;
import org.limewire.ui.swing.util.CategoryIconManager;

/**
 * Implementation of ListViewStoreRenderer for styles A and B.
 */
class ListViewStoreRendererAB extends ListViewStoreRenderer {

    private JButton albumCoverButton;
    
    private JXPanel albumTextPanel;
    private HTMLLabel albumHeadingLabel;
    private JLabel albumSubHeadingLabel;
    
    private JXPanel albumInfoPanel;
    private JButton albumTracksButton;
    private JButton albumInfoButton;
    
    private JXPanel albumDownloadPanel;
    private JLabel albumPriceLabel;
    private JButton albumStreamButton;
    private JButton albumDownloadButton;
    
    private JButton mediaIconButton;
    
    private JXPanel mediaTextPanel;
    private HTMLLabel mediaHeadingLabel;
    private JLabel mediaSubHeadingLabel;
    private JLabel mediaPriceLabel;
    private JButton mediaStreamButton;
    private JButton mediaDownloadButton;
    private JButton mediaInfoButton;
    
    /**
     * Constructs a store renderer with the specified icon manager and store
     * style.
     */
    public ListViewStoreRendererAB(CategoryIconManager categoryIconManager, StoreStyle storeStyle) {
        super(categoryIconManager, storeStyle);
    }

    @Override
    protected void initAlbumComponent() {
        albumPanel.setOpaque(false);
        
        albumCoverButton = new IconButton();
        
        albumTextPanel = new JXPanel();
        albumTextPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        albumTextPanel.setOpaque(false);
        
        albumHeadingLabel = new HTMLLabel();
        albumHeadingLabel.setOpenUrlsNatively(false);
        albumHeadingLabel.setOpaque(false);
        albumHeadingLabel.setFocusable(false);
        albumHeadingLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
//        StyleSheet mainStyle = ((HTMLDocument) albumHeadingLabel.getDocument()).getStyleSheet();
//        String rules = "body { font-family: " + storeStyle.getArtistFont().getFamily() + "; }" +
//            ".title { color: " + headingColor + "; font-size: " + storeStyle.getArtistFont().getSize() + "; }" +
//            "a { color: " + headingColor + "; }";
//        StyleSheet newStyle = new StyleSheet();
//        newStyle.addRule(rules);
//        mainStyle.addStyleSheet(newStyle);
        albumHeadingLabel.setHtmlFont(storeStyle.getArtistFont());
        albumHeadingLabel.setHtmlForeground(storeStyle.getArtistForeground());
        albumHeadingLabel.setHtmlLinkForeground(storeStyle.getArtistForeground());
        
        albumSubHeadingLabel = new NoDancingHtmlLabel();
        albumSubHeadingLabel.setFont(storeStyle.getAlbumFont());
        albumSubHeadingLabel.setForeground(storeStyle.getAlbumForeground());
        
        albumInfoPanel = new JXPanel();
        albumInfoPanel.setOpaque(false);
        
        albumTracksButton = new IconButton();
        albumTracksButton.setFont(storeStyle.getShowTracksFont());
        albumTracksButton.setForeground(storeStyle.getShowTracksForeground());
        albumTracksButton.setHideActionText(false);
        albumTracksButton.setAction(showTracksAction);
        
        albumInfoButton = new IconButton();
        albumInfoButton.setFont(storeStyle.getInfoFont());
        albumInfoButton.setForeground(storeStyle.getInfoForeground());
        albumInfoButton.setHideActionText(false);
        albumInfoButton.setAction(showInfoAction);
        
        albumDownloadPanel = new JXPanel();
        albumDownloadPanel.setOpaque(false);
        
        albumPriceLabel = new JLabel();
        albumPriceLabel.setFont(storeStyle.getPriceFont());
        albumPriceLabel.setForeground(storeStyle.getPriceForeground());
        
        albumStreamButton = new IconButton();
        albumStreamButton.setIcon(storeStyle.getStreamIcon());
        
        albumDownloadButton = new IconButton();
        albumDownloadButton.setIcon(storeStyle.getDownloadAlbumIcon());
        
        albumTextPanel.setLayout(new MigLayout("nogrid, insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        albumTextPanel.add(albumHeadingLabel, "left, shrinkprio 200, growx, wmax pref, hidemode 3, wrap");
        albumTextPanel.add(albumSubHeadingLabel, "left, shrinkprio 200, growx, hidemode 3");
        
        albumInfoPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0! 0!, novisualpadding"));
        albumInfoPanel.add(albumTracksButton, "alignx right, aligny top, gapright 9, shrinkprio 0, growprio 0");
        albumInfoPanel.add(albumInfoButton, "alignx right, aligny top, shrinkprio 0, growprio 0");
        
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
        
        mediaTextPanel = new JXPanel();
        mediaTextPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        mediaTextPanel.setOpaque(false);
        
        mediaHeadingLabel = new HTMLLabel();
        mediaHeadingLabel.setOpenUrlsNatively(false);
        mediaHeadingLabel.setOpaque(false);
        mediaHeadingLabel.setFocusable(false);
        mediaHeadingLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
//        StyleSheet mainStyle = ((HTMLDocument) mediaHeadingLabel.getDocument()).getStyleSheet();
//        String rules = "body { font-family: " + storeStyle.getArtistFont().getFamily() + "; }" +
//            ".title { color: " + headingColor + "; font-size: " + storeStyle.getArtistFont().getSize() + "; }" +
//            "a { color: " + headingColor + "; }";
//        StyleSheet newStyle = new StyleSheet();
//        newStyle.addRule(rules);
//        mainStyle.addStyleSheet(newStyle);
        mediaHeadingLabel.setHtmlFont(storeStyle.getArtistFont());
        mediaHeadingLabel.setHtmlForeground(storeStyle.getArtistForeground());
        mediaHeadingLabel.setHtmlLinkForeground(storeStyle.getArtistForeground());
        
        mediaSubHeadingLabel = new NoDancingHtmlLabel();
        mediaSubHeadingLabel.setFont(storeStyle.getAlbumFont());
        mediaSubHeadingLabel.setForeground(storeStyle.getAlbumForeground());
        
        mediaPriceLabel = new JLabel();
        mediaPriceLabel.setFont(storeStyle.getPriceFont());
        mediaPriceLabel.setForeground(storeStyle.getPriceForeground());
        
        mediaStreamButton = new IconButton();
        mediaStreamButton.setIcon(storeStyle.getStreamIcon());
        
        mediaDownloadButton = new IconButton();
        mediaDownloadButton.setIcon(storeStyle.getDownloadTrackIcon());
        
        mediaInfoButton = new IconButton();
        mediaInfoButton.setFont(storeStyle.getInfoFont());
        mediaInfoButton.setForeground(storeStyle.getInfoForeground());
        mediaInfoButton.setHideActionText(false);
        mediaInfoButton.setAction(showInfoAction);
        
        mediaTextPanel.setLayout(new MigLayout("nogrid, ins 0 0 0 0, gap 0! 0!, novisualpadding"));
        mediaTextPanel.add(mediaHeadingLabel, "left, shrinkprio 200, growx, wmax pref, hidemode 3, wrap");
        mediaTextPanel.add(mediaSubHeadingLabel, "left, shrinkprio 200, growx, hidemode 3");
        
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
    protected Component createTrackComponent(SearchResult result) {
        JXPanel trackPanel = new JXPanel();
        trackPanel.setOpaque(true);
        trackPanel.setBackground(Color.WHITE);
        
        JButton streamButton = new IconButton();
        streamButton.setIcon(storeStyle.getStreamIcon());
        
        JButton downloadButton = new IconButton();
        downloadButton.setIcon(storeStyle.getDownloadTrackIcon());
        
        JLabel trackLabel = new JLabel();
        trackLabel.setFont(storeStyle.getTrackFont());
        trackLabel.setForeground(storeStyle.getTrackForeground());
        trackLabel.setText((String) result.getProperty(FilePropertyKey.NAME));
        
        JLabel priceLabel = new JLabel();
        priceLabel.setFont(storeStyle.getPriceFont());
        priceLabel.setForeground(storeStyle.getPriceForeground());
        priceLabel.setText("1 Credit"); // TODO replace with result price
        
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
        
        System.out.println("prefSize=" + trackPanel.getPreferredSize()); // TODO REMOVE
        
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
        
        albumHeadingLabel.setText(result.getHeading());
        albumSubHeadingLabel.setText(result.getSubheading());
        
        albumPriceLabel.setText(vsr.getStoreResult().getPrice());
        
        // Tracks and Info buttons hidden in Style A when not editing.
        albumInfoPanel.setVisible((storeStyle.getType() != Type.STYLE_A) || editing);
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
        
        mediaHeadingLabel.setText(result.getHeading());
        mediaSubHeadingLabel.setText(result.getSubheading());
        
        mediaPriceLabel.setText(vsr.getStoreResult().getPrice());
        
        // Info button hidden in Style A when not editing.
        mediaInfoButton.setVisible((storeStyle.getType() != Type.STYLE_A) || editing);
    }
}
