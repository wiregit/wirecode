package org.limewire.ui.swing.search;

public interface ModeListener {
    
    public enum Mode { LIST, TABLE };
    
    void setMode(Mode mode);
}
