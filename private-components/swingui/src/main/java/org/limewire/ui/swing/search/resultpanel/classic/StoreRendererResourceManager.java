package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

/**
 * Resource manager for store renderers.
 */
public class StoreRendererResourceManager {
    @Resource(key="StoreRenderer.albumIcon")
    private Icon albumIcon;
    @Resource(key="StoreRenderer.audioIcon")
    private Icon audioIcon;
    @Resource(key="StoreRenderer.albumCollapsedIcon")
    private Icon albumCollapsedIcon;
    @Resource(key="StoreRenderer.albumExpandedIcon")
    private Icon albumExpandedIcon;
    @Resource(key="StoreRenderer.classicBuyIcon")
    private Icon buyIcon;
    @Resource(key="StoreRenderer.classicPauseIcon")
    private Icon pauseIcon;
    @Resource(key="StoreRenderer.classicPlayIcon")
    private Icon playIcon;
    
    @Resource(key="IconLabelRenderer.disabledForegroundColor")
    private Color disabledForegroundColor;
    @Resource(key="IconLabelRenderer.font")
    private Font font;
    @Resource(key="IconLabelRenderer.downloadingIcon")
    private Icon downloadIcon;
    @Resource(key="IconLabelRenderer.libraryIcon")
    private Icon libraryIcon;
    @Resource(key="IconLabelRenderer.spamIcon")
    private Icon spamIcon;
    
    /**
     * Constructs a RendererResources object. 
     */
    @Inject
    public StoreRendererResourceManager() {
        GuiUtils.assignResources(this);
    }
    
    public Icon getAlbumIcon() {
        return albumIcon;
    }
    
    public Icon getAudioIcon() {
        return audioIcon;
    }
    
    public Icon getAlbumCollapsedIcon() {
        return albumCollapsedIcon;
    }
    
    public Icon getAlbumExpandedIcon() {
        return albumExpandedIcon;
    }
    
    public Icon getBuyIcon() {
        return buyIcon;
    }
    
    public Icon getPauseIcon() {
        return pauseIcon;
    }
    
    public Icon getPlayIcon() {
        return playIcon;
    }
    
    public Color getDisabledForegroundColor() {
        return disabledForegroundColor;
    }
    
    public Icon getDownloadIcon() {
        return downloadIcon;
    }
    
    public Font getFont() {
        return font;
    }
    
    public Icon getLibraryIcon() {
        return libraryIcon;
    }
    
    public Icon getSpamIcon() {
        return spamIcon;
    }
}