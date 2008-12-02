package org.limewire.ui.swing.upload;

import javax.swing.JLabel;

import org.jdesktop.swingx.JXPanel;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UploadPanel extends JXPanel{
    
    public static final String NAME = "UploadPanel";
    
    @Inject
    public UploadPanel(){
        add(new JLabel("I am an upload panel. bite me."));
    }

}
