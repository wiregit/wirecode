package org.limewire.core.api.library;

import java.awt.Image;

public interface ImageLocalFileItem extends LocalFileItem {
    
    public static int WIDTH = 150;
    public static int HEIGHT = 150;
    
    public Image getThumbnail();

}
