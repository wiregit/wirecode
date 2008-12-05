package org.limewire.ui.swing.components;

import java.awt.Rectangle;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.ui.swing.util.SwingUtils;

/**
 * An editor pane that forces synchronous page loading unless you use the
 * {@link #setPageAsynchronous(String, String)} method.
 * 
 * Much of this comes from JEditorPane and had to be copied out because of package-private problems.
 */
public class HTMLPane extends JEditorPane {
    
    private static final ExecutorService QUEUE = ExecutorsHelper.newProcessingQueue("HTMLPane Queue");

    private final EditorKit kit = new SynchronousEditorKit();    
    private HashMap<Object, Object> pageProperties;
    
    private volatile boolean pageLoaded;
    private Future<?> currentLoad;
    
    public HTMLPane() {
        setEditorKit(kit);
        setEditorKitForContentType(kit.getContentType(), kit);
        setContentType("text/html");
        setEditable(false);
    }
    
    /** Loads the given URL, loading the backup page if it fails to load. */
    public void setPageAsynchronous(final String url, final String backupPage) {        
        assert SwingUtilities.isEventDispatchThread();        
        if(currentLoad != null) {
            currentLoad.cancel(true);
        }        
        currentLoad = QUEUE.submit(new Callable<Void>() {
            @Override
            public Void call() {
                try {
                    setPageImpl(new URL(url));
                } catch(IOException iox) {
                    SwingUtils.invokeLater(new Runnable() {
                        public void run() {
                            setContentType("text/html");
                            setText(backupPage);
                            setCaretPosition(0);
                        }
                    });
                }
                return null;
            }
        });
    }
    
    public boolean isLastRequestSuccessful() {
        return pageLoaded;
    }
    
    @Override
    public void setPage(final URL page) throws IOException {
        setPageImpl(page);
    }
    
    // Note the use of SwingUtils vs SwingUtilities.
    // SwingUtils will invoke immediately if already in the dispatch thread,
    // SwingUtilities will always force to the end of the queue --
    // The uses here (of both) are very deliberate.
    private void setPageImpl(final URL page) throws IOException {
        if (page == null) {
            throw new IOException("invalid url");
        }

        pageLoaded = false;
        final AtomicReference<URL> loaded = new AtomicReference<URL>();
        SwingUtils.invokeAndWait(new Runnable() {
            public void run() {
                loaded.set(getPage());
                // reset scrollbar
                if (!page.equals(loaded.get()) && page.getRef() == null) {
                    scrollRectToVisible(new Rectangle(0, 0, 1, 1));
                }
            }
        });
        
        final AtomicBoolean reloaded = new AtomicBoolean(false);
        InputStream in = new InterruptableStream(getStream(page));
        if (kit != null) {
            final AtomicReference<Document> doc = new AtomicReference<Document>();
            SwingUtils.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    doc.set(initializeModel(kit, page));
                    setDocument(doc.get());
                }
            });
            read(in, doc.get());
            reloaded.set(true);
        }
        
        SwingUtils.invokeLater(new Runnable() {
            public void run() {
                final String reference = page.getRef();
                if (reference != null) {
                    if (!reloaded.get()) {
                        scrollToReference(reference);
                    } else {
                        // Force this to the back of the queue.
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                scrollToReference(reference);
                            }
                        });
                    }
                } else {
                    setCaretPosition(0);
                }
                getDocument().putProperty(Document.StreamDescriptionProperty, page);
                firePropertyChange("page", loaded.get(), page);
            }            
        });        
        pageLoaded = true; // purposely not in a finally -- if we fail it didn't load
    }

    // Copied from {@link JEditorPane#getStream(URL)} because of package-private problems.
    protected InputStream getStream(URL page) throws IOException {
        final URLConnection conn = page.openConnection();
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
            hconn.setInstanceFollowRedirects(false);
            int response = hconn.getResponseCode();
            boolean redirect = (response >= 300 && response <= 399);

            /*
             * In the case of a redirect, we want to actually change the URL
             * that was input to the new, redirected URL
             */
            if (redirect) {
                String loc = conn.getHeaderField("Location");
                if (loc.startsWith("http", 0)) {
                    page = new URL(loc);
                } else {
                    page = new URL(page, loc);
                }
                return getStream(page);
            }
        }

        SwingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                handleConnectionProperties(conn);
            }
        });
        return conn.getInputStream();
    }

    // Copied from {@link JEditorPane#handleConnectionProperties(URLConnection)} because of package-private problems.
    private void handleConnectionProperties(URLConnection conn) {
        if (pageProperties == null) {
            pageProperties = new HashMap<Object, Object>();
        }
        String type = conn.getContentType();
        if (type != null) {
            setContentType(type);
            pageProperties.put("content-type", type);
        }
        pageProperties.put(Document.StreamDescriptionProperty, conn.getURL());
        String enc = conn.getContentEncoding();
        if (enc != null) {
            pageProperties.put("content-encoding", enc);
        }
    }
    
    // Copied from {@link JEditorPane#initializeModel(EditorKit, URL)} because of package-private problems.
    private Document initializeModel(EditorKit kit, URL page) {
        Document doc = kit.createDefaultDocument();
        if (pageProperties != null) {
            // transfer properties discovered in stream to the
            // document property collection.
            for (Iterator<Object> iter = pageProperties.keySet().iterator(); iter.hasNext() ;) {
                Object key = iter.next();
                doc.putProperty(key, pageProperties.get(key));
            }
            pageProperties.clear();
        }
        if (doc.getProperty(Document.StreamDescriptionProperty) == null) {
            doc.putProperty(Document.StreamDescriptionProperty, page);
        }
        return doc;
    }

    /**
     * Updated to set the asynchronous priority to -1, although it shouldn't
     * matter because we explicitly load documents ourselves 
     */
    private static class SynchronousEditorKit extends HTMLEditorKit {
        @Override
        public Document createDefaultDocument() {
            Document doc = super.createDefaultDocument();
            ((HTMLDocument)doc).setAsynchronousLoadPriority(-1);
            return doc;
        }
    }
    
    /** An extension to {@link InputStream} that fails if the thread was interrupted. */
    private static class InterruptableStream extends FilterInputStream {
        public InterruptableStream(InputStream i) {
            super(i);
        }

        protected void checkInterrupted() throws IOException {
            if(Thread.interrupted()) {
                throw new IOException("read interrupted");
            }
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            checkInterrupted();
            return super.read(b, off, len);
        }

        public int read() throws IOException {
            checkInterrupted();
            return super.read();
        }

        public long skip(long n) throws IOException {
            checkInterrupted();
            return super.skip(n);
        }

        public int available() throws IOException {
            checkInterrupted();
            return super.available();
        }

        public void reset() throws IOException {
            checkInterrupted();
            super.reset();
        }
    }    
    
}
