package org.limewire.ui.swing.player;

import java.awt.Canvas;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicReference;

import javax.media.IncompatibleSourceException;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.protocol.DataSource;
import javax.swing.SwingUtilities;

import org.limewire.service.ErrorService;
import org.limewire.ui.swing.util.MacOSXUtils;
import org.limewire.util.ExceptionUtils;
import org.limewire.util.OSUtils;

class VideoPlayerFactory {
    // Flag indicating whether the native library that provides the bridge between Java and Cocoa could be loaded.
    private static boolean initializationOfJavaToCocoaBridgeFailed = false;
    
    public Player createVideoPlayer(File file, final Container parentComponent) throws IncompatibleSourceException {
        if (!OSUtils.isWindows() && !OSUtils.isMacOSX()) {
            throw new IllegalStateException("Video is only supported on Windows and Mac");
        }

        Player handler;
        
        if(OSUtils.isWindows7()){
            return createWindows7Player(file, parentComponent);            
        } else if (OSUtils.isWindows()) {           
            handler = new net.sf.fmj.ds.media.content.unknown.Handler();
        } else { // OSX
            try {
                if (!initializationOfJavaToCocoaBridgeFailed)
                    handler = new net.sf.fmj.qt.media.content.unknown.JavaToCocoaHandler();
                else
                    handler = new net.sf.fmj.qt.media.content.unknown.QuickTimeForJavaHandler();
            } catch (ExceptionInInitializerError error) {
                // if the native cocoa wrapper library can't be loaded, then let's use the QuickTime for Java library instead
                ErrorService.error(error, "java.library.path=" + System.getProperty("java.library.path") + "\n\n" + "trace dependencies=" + MacOSXUtils.traceLibraryDependencies("rococoa.jnilib"));
                initializationOfJavaToCocoaBridgeFailed = true;
                handler = new net.sf.fmj.qt.media.content.unknown.QuickTimeForJavaHandler();
            }
        }

        setupPlayer(handler, file);

        final Player finalHandler = handler;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //remove all in case the mfCanvas was added.
                parentComponent.removeAll();
                parentComponent.add(finalHandler.getVisualComponent());
            }
        });
       return handler;
    }
    
    private Player createWindows7Player(File file, final Container parentComponent) throws IncompatibleSourceException {

        //Since the DS player supports 3rd party codecs and has a chance of supporting more files, we will try it before MF
         final Player dsPlayer = new net.sf.fmj.ds.media.content.unknown.Handler();    
         try {
             //we need to setup the player here so we can fall back to ds if it fails
             setupPlayer(dsPlayer, file);
             SwingUtilities.invokeLater(new Runnable() {
                 @Override
                 public void run() {
                     parentComponent.add(dsPlayer.getVisualComponent());                
                 }
             });
             return dsPlayer;
         } catch (IncompatibleSourceException e) {
             //ds can't play it.  try mf.
         }
         
         //DS failed.  Now we try MF.
         final AtomicReference<Canvas> mfCanvas = new AtomicReference<Canvas>(); 
         try {
             //create new canvas and add to parentComponent so we can get an hwnd
             SwingUtilities.invokeAndWait(new Runnable() {
                 @Override
                 public void run() {
                     Canvas canvas = new Canvas();
                     parentComponent.add(canvas);
                     //addNotify to make sure we have a working hwnd
                     parentComponent.addNotify();
                     mfCanvas.set(canvas);
                 }
             });
         } catch (InterruptedException e) {
             throw new IncompatibleSourceException(e.toString() + " \n" + ExceptionUtils.getStackTrace(e));
         } catch (InvocationTargetException e) {
             throw new IncompatibleSourceException(e.toString() + " \n" + ExceptionUtils.getStackTrace(e));
         }
         
         final Player mfPlayer = new net.sf.fmj.mf.media.content.unknown.Handler(mfCanvas.get());
         
         //let this throw the exception if it fails.  
         setupPlayer(mfPlayer, file);
         
         SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                 parentComponent.setPreferredSize(mfCanvas.get().getPreferredSize());
             }
         });
        return mfPlayer;
     
    }
    
    private void setupPlayer(Player player, File file) throws IncompatibleSourceException {
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
    
    private DataSource createDataSource(File file) throws MalformedURLException{
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
