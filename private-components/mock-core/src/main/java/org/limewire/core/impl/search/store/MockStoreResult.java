package org.limewire.core.impl.search.store;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.store.StoreResult;

/**
 * Implementation of StoreResult for the mock core.
 */
public class MockStoreResult implements StoreResult {

    private final Category category;
    private final URN urn;
    private final Map<FilePropertyKey, Object> propertyMap = 
        new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);
    private final List<SearchResult> resultList;
    
    private Icon albumIcon;
    private String fileExtension;
    private long size;
    
    /**
     * Constructs a MockStoreResult with the specified URN and category.
     */
    public MockStoreResult(URN urn, Category category) {
        this.urn = urn;
        this.category = category;
        this.resultList = new ArrayList<SearchResult>();
    }
    
    @Override
    public boolean isAlbum() {
        return (resultList.size() > 1);
    }
    
    @Override
    public Icon getAlbumIcon() {
        return albumIcon;
    }
    
    @Override
    public List<SearchResult> getAlbumResults() {
        return resultList;
    }
    
    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public String getFileExtension() {
        return fileExtension;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return propertyMap.get(key);
    }
    
    @Override
    public long getSize() {
        return size;
    }
    
    @Override
    public SortPriority getSortPriority() {
        return SortPriority.MIXED;
    }
    
    @Override
    public URN getUrn() {
        return urn;
    }
    
    public void addAlbumResult(SearchResult searchResult) {
        resultList.add(searchResult);
    }
    
    public void setAlbumIcon(Icon icon) {
        this.albumIcon = icon;
    }
    
    public void setFileExtension(String extension) {
        this.fileExtension = extension;
    }
    
    public void setProperty(FilePropertyKey key, Object value) {
        propertyMap.put(key, value);
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    public static class MockAlbumIcon implements Icon {
        private static final float SIZE_TO_THICKNESS = 7.0f;

        private final Color color;
        private final int size;

        public MockAlbumIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public int getIconHeight() {
            return this.size;
        }

        @Override
        public int getIconWidth() {
            return this.size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            // Create graphics.
            Graphics2D g2d = (Graphics2D) g.create();
            
            // Set graphics to use anti-aliasing for smoothness.
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Set line color and thickness.
            float thickness = Math.max(this.size / SIZE_TO_THICKNESS, 1.0f);
            g2d.setColor(this.color);
            g2d.setStroke(new BasicStroke(thickness));

            // Create shape.
            Shape backSlash = new Line2D.Double(0, 0, this.size, this.size);
            Shape slash = new Line2D.Double(0, this.size, this.size, 0);
            
            // Draw shape at specified position.
            g2d.translate(x, y);
            g2d.draw(backSlash);
            g2d.draw(slash);

            // Dispose graphics.
            g2d.dispose();
        }
    }
}
