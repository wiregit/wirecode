package org.limewire.store.storeserver.core;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.limewire.store.storeserver.util.DebugPanel;
import org.limewire.store.storeserver.util.Util;
import org.limewire.store.storeserver.util.DebugPanel.Debuggable;


/**
 * Base class for servers, both local and remote.
 * 
 * @author jpalm
 */
public abstract class Server implements Runnable, Debuggable {

  private final int port;
  private boolean done = false;
  
  /** 
   * Set by {@link #start(Server)}.
   */
  private Thread runner;
  protected final Thread getRunner() {
    return runner;
  }

  private final Map<String, Handler> names2handlers = new HashMap<String, Handler>();
  
  /**
   * Returns and starts a {@link Thread} for <tt>s</tt>.
   * 
   * @param s the server in questions
   * @return a {@link Thread} for <tt>s</tt>
   */
  public static Thread start(final Server s) {
    Thread t = new Thread(s);
    t.start();
    s.runner = t;
    return t;
  }


  public Server(final int port) {
    this.port = port;
    Handler[] hs = createHandlers();
    note("creating " + hs.length + " handler(s)...");
    for (Handler h : hs) {
      this.names2handlers.put(h.name().toLowerCase(), h);
      note(" - " + h.name());
    }
    note("connecting on port " + port);
  }

  /**
   * @return the list of handlers that will take your requests.
   */
  protected abstract Handler[] createHandlers();

  /**
   * Send a message.
   * 
   * @param args
   */
  public abstract String sendMsg(String msg, Map<String, String> args);

  void realMain(final String[] args) {
    start(this);
  }

  public final void run() {
    this.hasShutDown = false;
    noteRun();
    go();
  }
  
  /**
   * Override this to know when we're starting.
   */
  protected void noteRun() { }

  public final void setDone(final boolean done) {
    this.done = done;
  }

  public final boolean isDone() {
    return this.done;
  }

  public final int getPort() {
    return port;
  }

  public void go() {
    try {
      final ServerSocket ss = new ServerSocket(port);
      final Socket s = ss.accept();
      handle(s, ss);
    } catch (IOException e) {
      handle(e);
      if (e instanceof java.net.BindException)
        setDone(true);
    }
    if (!isDone()) go();
  }

  protected final void handle(final Throwable t) {
    handle(t, null);
  }

  protected final void handle(final Throwable t, final String msg) {
    t.printStackTrace();
  }

  private void handle(final Socket s, final ServerSocket ss) throws IOException {
    final BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
    String line;
    String req = null;
    final Map<String, Map<String, String>> headers = new HashMap<String, Map<String, String>>();
    while ((line = in.readLine()) != null) {

      if (line.length() == 0)
        break;
      if (line.startsWith("GET")) {
        final int ispace1 = line.indexOf(" ");
        final int ispace2 = line.indexOf(" ", ispace1 + 1);
        req = line.substring(ispace1 + 1, ispace2);
      } else {
        final int icolon = line.indexOf(":");
        if (icolon != -1) {
          final String name = line.substring(0, icolon);
          final String rest = line.substring(icolon + 1);
          final Map<String, String> pairs;
          //
          // This is a total hack
          //
          if (name.equals("User-Agent")) {
            pairs = new HashMap<String, String>(1);
            pairs.put(name, rest);
          } else {
            pairs = Util.parseHeader(rest);
          }
          headers.put(name, pairs);
        }
      }

    }
    final PrintWriter o = new PrintWriter(s.getOutputStream(), true);

    // sanity check
    if (req == null) {
      error("couldn't find a request");
      return;
    }

    final String request = req.startsWith("/") ? req.substring(1) : req;
    note("request: " + request);

    println(o, "HTTP/1.1 200 OK");
    if (isFile(request)) {
      final String f = makeFile(request);
      final File file = new File(f);
      printContentLength(o, file.length());
      final BufferedReader r = new BufferedReader(new FileReader(f));
      String l;
      while ((l = r.readLine()) != null) {
        o.print(l);
        o.print("\n");
      }
      r.close();
    } else {
      //
      // give the client a change to modify the headers
      //
      final String ip = Util.getIPAddress(s.getInetAddress());
      final Request incoming = new Request(ip);
      String res = handle(request, o, incoming);
      printContentLength(o, res.length());
      o.print(res);
    }

    o.flush();

    // close everything
    in.close();
    o.close();
    s.close();
    ss.close();
  }

  private void printContentLength(final PrintWriter o, final long len) {
    o.print("Content-length: ");
    println(o, String.valueOf(len));
    o.print("Last-modified: ");
    println(o, Util.createCookieDate()); // todo wrong
    o.print(Constants.NEWLINE);
  }

  private void println(final PrintWriter o, final String line) {
    o.print(line);
    o.print(Constants.NEWLINE);
  }

  /**
   * Create an instance of Handler from the top level name as well as trying a static inner class
   * and calls its {@link Handler#handle()} method.
   * 
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws Exception
   */
  private String handle(final String request, final PrintWriter out, Request incoming) {
    final String req = makeFile(request);
    final Handler h = names2handlers.get(req.toLowerCase());
    if (h == null) {
      error("!!! Couldn't create a handler for " + req);
      return report(ErrorCodes.UNKNOWN_COMMAND);
    }
    note("have handler: " + h.name());
    final String res = h.handle(getArgs(request), incoming);
    note("response from " + h.name() + ": " + res);
    return res;
  }

  static Map<String, String> getArgs(final String request) {
    if (request == null || request.length() == 0) {
      return Util.EMPTY_MAP_OF_STRING_X_STRING;
    }
    final int ihuh = request.indexOf('?');
    if (ihuh == -1) {
      return Util.EMPTY_MAP_OF_STRING_X_STRING;
    }
    final String rest = request.substring(ihuh + 1);
    return Util.parseArgs(rest);
  }

  /**
   * @return <tt>true</tt> if <tt>request</tt> is a file
   */
  private boolean isFile(final String request) {
    return new File(makeFile(request)).exists();
  }

  /**
   * Removes hash and other stuff.
   */
  private String makeFile(final String s) {
    String res = s;
    final char[] cs = {'#', '?'};
    for (char c : cs) {
      final int id = res.indexOf(c);
      if (id != -1)
        res = res.substring(0, id);
    }
    return res;
  }

  private Note note;
  private boolean debug;

  public boolean getDebug() {
    return debug;
  }

  public void setDebug(final boolean debug) {
    this.debug = debug;
  }

  /** We want the debug panels to tile. */
  static int numDebugPanels = 0;

  public final void beLoud() {
    setDebug(true);
    DebugPanel d = DebugPanel.showNewInstance(this);
    setNote(d);
    if (numDebugPanels == 1) {
      Toolkit tk = Toolkit.getDefaultToolkit();
      d.setFrameLocation(0, tk.getScreenSize().height / 2);
    }
    numDebugPanels++;
  }

  public final void setNote(final Note note) {
    this.note = note;
  }

  /**
   * Only set when someone has called {@link #shutDown(long)} to differentiate between
   * forced and accidental shutdowns.
   */
  private boolean hasShutDown;
  protected final boolean hasShutDown() {
    return this.hasShutDown;
  }
  
  /**
   * Attempts to join this thread and then set Done to <tt>true</tt>.
   * 
   * @param millis
   *          millisend to wait for a join
   */
  public final void shutDown(final long millis) {
    note("shutting down");
    this.hasShutDown = true;
    setDone(true);
    synchronized (runner) {
      runner.interrupt();
      try {
        runner.join(millis);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    runner = null;
  }

  /**
   * Calls {@link #shutDown(long)} with <tt>1000</tt>.
   */
  public final void shutDown() {
    shutDown(1000);
  }

  public final void note(final Object msg, final Constants.Level level) {
    if (note == null) {
      final String str = "[" + simpleName() + "] " + msg;
      System.err.println(str);
    } else {
      note.note(msg, level);
    }
  }

  public final void error(final Object msg) {
    note(msg, Constants.Level.ERROR);
  }

  public final void note(final Object msg) {
    note(msg, Constants.Level.MESSAGE);
  }

  private String simpleName() {
    String s = getClass().getName();
    int ilastDot = s.lastIndexOf(".");
    return ilastDot == -1 ? s : s.substring(ilastDot + 1);
  }

  /**
   * Wraps the message <tt>msg</tt> using callback function <tt>callback</tt>.
   * The msesage is surrounded by {@link Constants.CALLBACK_QUOTE}s and all
   * quotes in the message, {@link Constants.CALLBACK_QUOTE}, are escaped.
   * 
   * @param callback  the function in which <tt>msg</tt> is wrapped
   * @param msg       the message
   * @return the message <tt>msg</tt> using callback function <tt>callback</tt>
   */
  protected final String wrapCallback(final String callback, final String msg) {
    if (Util.isEmpty(callback)) {
      return msg;
    } else {
      char q = Constants.CALLBACK_QUOTE;
      String s = Constants.CALLBACK_QUOTE_STRING;
      return callback + "(" + q + msg.replace(s, "\\" + s) + q + ")";
    }
  }

  /**
   * Wraps the message <tt>error</tt> in the call back {@link Constants.ERROR_CALLBACK}.
   * <br>
   * Example: If the error message is <tt>"You stink!"</tt> the wrapped
   *          message would be <tt>error("You stink!")</tt>.
   *
   * @param error the error message
   * @return the message <tt>error</tt> in the call back
   */
  public final String report(final String error) {
    return wrapCallback(Constants.ERROR_CALLBACK, Util.wrapError(error));
  }

  // ------------------------------------------------------------
  // Handlers
  // ------------------------------------------------------------
  
  /** 
   * Handles commands. 
   */
  public interface Handler {
    
    /**
     * Perform some operation on the incoming message and return the result.
     * 
     * @param args  CGI params
     * @param req   incoming {@link Request}
     * @return      the result of performing some operation on the incoming message
     */
    String handle(Map<String, String> args, Request req);

    /**
     * Returns the unique name of this instance.
     * 
     * @return the unique name of this instance
     */
    String name();
  }
  
  /** 
   * Handles commands, but does NOT return a result.
   */
  public interface Listener {
    
    /**
     * Perform some operation on the incoming message.
     * 
     * @param args  CGI params
     * @param req   incoming {@link Request}
     */
    void handle(Map<String, String> args, Request req);

    /**
     * Returns the unique name of this instance.
     * 
     * @return the unique name of this instance
     */
    String name();
  }
  
  private static abstract class HasName {

      private final String name;

      public HasName(final String name) {
        this.name = name;
      }

      public HasName() {
        String n = getClass().getName();
        int ilast;
        ilast = n.lastIndexOf(".");
        if (ilast != -1) n = n.substring(ilast + 1);
        ilast = n.lastIndexOf("$");
        if (ilast != -1) n = n.substring(ilast + 1);
        this.name = n;
      }

      public final String name() {
        return name;
      }

      protected final String getArg(final Map<String, String> args, final String key) {
        final String res = args.get(key);
        return res == null ? "" : res;
      }
      
  }
  
  /**
   * Generic base class for {@link Handler}s.
   * 
   * @author jpalm
   */
  public abstract static class AbstractHandler extends HasName implements Handler {
      public AbstractHandler(String name) { super(name); }
      public AbstractHandler() { super(); }
  }  

  
  /**
   * Generic base class for {@link Listener}s.
   * 
   * @author jpalm
   */
  public abstract class AbstractListener extends HasName implements Listener {
      public AbstractListener(String name) { super(name); }
      public AbstractListener() { super(); }
  }  

  /**
   * A {@link Handler} requiring a callback specified by the parameter {@link Parameters#CALLBACK}.
   */
  protected abstract class HandlerWithCallback extends AbstractHandler {

    public final String handle(final Map<String, String> args, Request req) {
      String callback = getArg(args, Parameters.CALLBACK);
      if (callback == null) {
        return report(ErrorCodes.MISSING_CALLBACK_PARAMETER);
      }
      return wrapCallback(callback, handleRest(args, req));
    }

    /**
     * Returns the result <b>IN PLAIN TEXT</b>.  Override this to provide functionality 
     * after the {@link Parameters#CALLBACK} argument has been extracted.  This method
     * should <b>NOT</b> wrap the result in the callback, nor should it be called from
     * any other method except this abstract class.
     * 
     * @param args  original, untouched arguments
     * @param req   originating {@link Request} object
     * @return      result <b>IN PLAIN TEXT</b>
     */
    abstract String handleRest(Map<String, String> args, Request req);
  }
  
  /**
   * Reponses sent back from servers.
   * 
   * @author jpalm
   */  
  public final static class Responses {
      
      private Responses() { }

      /**
       * Success.
       */
      public final static String OK = "ok";
      
      /**
       * When there was a command sent to the local host, but no
       * {@link Dispatchee} was set up to handle it.
       */
      public static final String NO_DISPATCHEE = "no.dispatcher";
      
      /**
       * When there was a {@link Dispatchee} to handle this command, but it didn't understand it.
       */
      public static final String UNKNOWN_COMMAND = "unknown.command";


  }
  
  /**
   * Collection of all the commands we send.
   * 
   * @author jpalm
   */
  public final static class Commands {

    private Commands() { }

    /**
     * Sent from Code to Local with no parameters.
     */
    public final static String START_COM      = "StartCom";
    
    /**
     * Sent from Local to Remote with parameters.
     * <ul>
     * <li>{@link Parameters#PUBLIC}</li>
     * <li>{@link Parameters#PRIVATE}</li>
     * </ul>
     */  
    public final static String STORE_KEY      = "StoreKey";
    
    /**
     * Sent from Code to Remote with parameters.
     * <ul>
     * <li>{@link Parameters#PRIVATE}</li>
     * </ul>
     */ 
    public final static String GIVE_KEY       = "GiveKey";
    
    /**
     * Send from Code to Local with no parameters.
     */
    public final static String DETATCH        = "Detatch";
    
    /**
     * Sent from Code to Local  with parameters.
     * <ul>
     * <li>{@link Parameters#PRIVATE}</li>
     * </ul>
     */   
    public static final String AUTHENTICATE   = "Authenticate";

    /* Testing */
    
    /**
     * Sent from Code to Local with parameters.
     * <ul>
     * <li>{@link Parameters#MSG}</li>
     * </ul>
     */   
    public final static String ECHO = "Echo";
    public final static String ALERT = "Alert";
  }  
  
  /**
   * Parameter names.
   * 
   * @author jpalm
   */
  public final static class Parameters {

      private Parameters() {
      }

      /**
       * Name of the callback function.
       */
      public static final String CALLBACK = "callback";

      /**
       * Private key.
       */
      public static final String PRIVATE = "private";

      /**
       * Public key.
       */
      public static final String PUBLIC = "public";

      /**
       * Name of the command to send to the {@link Dispatchee}.
       */
      public static final String COMMAND = "command";

      /**
       * Message to send to the <tt>ECHO</tt> command.
       */
      public static final String MSG = "msg";

      /**
       * Name of a URL.
       */
      public static final String URL = "url";

  }  
  
  /**
   * Codes that are sent to the code (javascript) when an error occurs.
   * 
   * @author jpalm
   */
  public final static class ErrorCodes {

    private ErrorCodes() { }

    public static final String INVALID_PUBLIC_KEY             = "invalid.public.key";
    public static final String INVALID_PRIVATE_KEY            = "invalid.private.key";
    public static final String INVALID_PUBLIC_KEY_OR_IP       = "invalid.public.key.or.ip.address";
    public static final String MISSING_CALLBACK_PARAMETER     = "missing.callback.parameter";
    public static final String UNKNOWN_COMMAND                = "unkown.command";
    public static final String UNITIALIZED_PRIVATE_KEY        = "uninitialized.private.key";
    public static final String MISSING_PRIVATE_KEY_PARAMETER  = "missing.private.parameter";
    public static final String MISSING_PUBLIC_KEY_PARAMETER   = "missing.public.parameter";
    public static final String MISSING_COMMAND_PARAMETER      = "missing.command.parameter";
    
  }  
  
  /**
   * A general place for constants.
   * 
   * @author jpalm
   */
  public final static class Constants {
      
      private Constants() {}

      /**
       * Various levels for messages.
       */
      public enum Level {
          MESSAGE, WARNING, ERROR, FATAL,
      }

      /**
       * The length of the public and private keys generated.
       */
      public static final int KEY_LENGTH = 10; // TODO

      public final static String NEWLINE = "\r\n";

      /**
       * The quote used to surround callbacks.  We need to escape this in the
       * strings that we pass back to the callback.
       */
      public static final char CALLBACK_QUOTE = '\'';

      /**
       * The String version of {@link #CALLBACK_QUOTE}.
       */
      public static final String CALLBACK_QUOTE_STRING = String.valueOf(CALLBACK_QUOTE);

      /**
       * The callback in which error messages are wrapped.
       */
      public static final String ERROR_CALLBACK = "error";

      /**
       * The string that separates arguments in the {@link Parameters#MSG} argument
       * when the command {@link Parameters#COMMAND} parameter is <tt>Msg</tt>.  This is
       * the urlencoded version of <tt>&</tt>, which is <tt>%26</tt>. 
       */
      public static final String ARGUMENT_SEPARATOR = "%26";

  }
}
