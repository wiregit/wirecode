package org.limewire.ui.swing.player;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.media.IncompatibleSourceException;
import javax.media.Player;
import javax.media.protocol.DataSource;
import javax.media.protocol.URLDataSource;

import com.lti.utils.OSUtils;

import net.sf.fmj.ejmf.toolkit.media.AbstractPlayer;
import net.sf.fmj.utility.URLUtils;


public class VideoPlayerFactory {
    public static Player createVideoPlayer(File file) throws IncompatibleSourceException, IOException {
        if (!OSUtils.isWindows() && !OSUtils.isMacOSX()) {
            throw new IllegalStateException("Video is only supported on Windows and Mac");
        }

        AbstractPlayer handler;
        DataSource source = new URLDataSource(new URL(URLUtils.createUrlStr(file)));
        if (OSUtils.isWindows()) {
            handler = new net.sf.fmj.ds.media.content.unknown.Handler();
        } else { // OSX
            handler = new net.sf.fmj.qt.media.content.unknown.Handler();
        }

        try {
            handler.setSource(source);
        } catch (UnsatisfiedLinkError e) {
            // TODO: this is not the best way to handle unsatisfied links but
            // our native launch fallback will work
            throw new IncompatibleSourceException(e);
        }

        handler.realize();
        return handler;
    }
}
