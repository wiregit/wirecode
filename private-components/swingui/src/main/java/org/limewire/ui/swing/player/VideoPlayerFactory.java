package org.limewire.ui.swing.player;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.media.IncompatibleSourceException;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.protocol.DataSource;

import org.limewire.util.ExceptionUtils;
import org.limewire.util.OSUtils;




public class VideoPlayerFactory {
    public static Player createVideoPlayer(File file) throws IncompatibleSourceException {
        if (!OSUtils.isWindows() && !OSUtils.isMacOSX()) {
            throw new IllegalStateException("Video is only supported on Windows and Mac");
        }

        Player handler;
        if (OSUtils.isWindows()) {
            if(OSUtils.isWindows7()){
                handler = new net.sf.fmj.mf.media.content.unknown.Handler();    
                try {
                    //we need to setup the player here so we can fall back to ds if it fails
                    setupPlayer(handler, file);
                    return handler;
                } catch (IncompatibleSourceException e) {
                    //mf can't play it.  try ds.
                }
            }
            handler = new net.sf.fmj.ds.media.content.unknown.Handler();
        } else { // OSX
            handler = new net.sf.fmj.qt.media.content.unknown.Handler();
        }

       setupPlayer(handler, file);
       return handler;
    }
    
    private static void setupPlayer(Player player, File file) throws IncompatibleSourceException {
        try {
            player.setSource(createDataSource(file));

        } catch (IOException e) {
            throw new IncompatibleSourceException(e.toString() + " \n" + ExceptionUtils.getStackTrace(e));
        } catch (UnsatisfiedLinkError e) {
            // TODO: this is not the best way to handle unsatisfied links but
            // our native launch fallback will work
            throw new IncompatibleSourceException(e.toString() + " \n" + ExceptionUtils.getStackTrace(e));
        }
        player.realize();
    }
    
    private static DataSource createDataSource(File file) throws MalformedURLException{
        //FMJ's handling of files is incredibly fragile.  This works with both quicktime and directshow.
        DataSource source = new net.sf.fmj.media.protocol.file.DataSource();
        if (OSUtils.isMacOSX()) {
            // On OS-X escaping characters such as spaces, parenthesis, etc are causing the 
            // files not to be loaded by FMJ. So let's not escape any characters. 
            source.setLocator(new MediaLocator("file:///" + file.getAbsolutePath()));
        } else {
            // This produces a URL with the form file:/path rather than file:///path
            // This URL form is accepted on Windows but not OS X.
            String urlString = file.toURI().toURL().toExternalForm();
            source.setLocator(new MediaLocator(urlString));
        }
        return source;
    }

}
