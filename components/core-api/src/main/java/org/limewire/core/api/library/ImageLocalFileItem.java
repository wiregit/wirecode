package org.limewire.core.api.library;

import java.awt.Image;

public interface ImageLocalFileItem extends LocalFileItem {
    
    public static int WIDTH = 120;
    public static int HEIGHT = 120;
    
    public Image getThumbnail();

}
