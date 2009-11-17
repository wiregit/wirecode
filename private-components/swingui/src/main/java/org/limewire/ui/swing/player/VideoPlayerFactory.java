package org.limewire.ui.swing.player;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.media.IncompatibleSourceException;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.protocol.DataSource;

import org.limewire.util.OSUtils;

import net.sf.fmj.utility.URLUtils;



public class VideoPlayerFactory {
    public static Player createVideoPlayer(File file) throws IncompatibleSourceException, IOException {
        if (!OSUtils.isWindows() && !OSUtils.isMacOSX()) {
            throw new IllegalStateException("Video is only supported on Windows and Mac");
        }

        Player handler;
        if (OSUtils.isWindows()) {
            handler = new net.sf.fmj.ds.media.content.unknown.Handler();
        } else { // OSX
            handler = new net.sf.fmj.qt.media.content.unknown.Handler();
        }

        try {
            handler.setSource(createDataSource(file));
        } catch (UnsatisfiedLinkError e) {
            // TODO: this is not the best way to handle unsatisfied links but
            // our native launch fallback will work
            throw new IncompatibleSourceException(e);
        }

        handler.realize();
        return handler;
    }
    
    private static DataSource createDataSource(File file){
        //FMJ's handling of files is incredibly fragile.  This works with both quicktime and directshow.
        DataSource source = new net.sf.fmj.media.protocol.file.DataSource();
        if (OSUtils.isMacOSX()) {
            // On OS-X escaping characters such as spaces, parenthesis, etc are causing the 
            // files not to be loaded by FMJ. So let's not escape any characters. 
            source.setLocator(new MediaLocator("file:///" + file.getAbsolutePath()));
        } else {
            try {
                // This produces a URL with the form file:/path rather than file:///path
                // This URL form is accepted on Windows but not OS X.
                String urlString = file.toURI().toURL().toExternalForm();
                source.setLocator(new MediaLocator(urlString));
            } catch (MalformedURLException e) {
                // It's preferable not to use the URLUtils.createUrlStr method,
                // because it encodes some characters that don't need to be encoded
                // and throws exceptions when encountering non-ASCII characters.
                source.setLocator(new MediaLocator(URLUtils.createUrlStr(file)));
            }                                   
        }
        return source;
    }

}
