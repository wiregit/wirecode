package org.limewire.store.storeserver.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DateFormat;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.limewire.store.storeserver.core.Note;
import org.limewire.store.storeserver.core.Constants.Level;


/**
 * A simple debugging panel.
 * 
 * @author Jeffrey Palm
 */
public final class DebugPanel extends JPanel implements Note {

  // ----------------------------------------------------------------------
  // Interface
  // ----------------------------------------------------------------------

  private Level level = Level.ERROR;
  private JFrame frame;

  public JFrame getFrame() {
    return this.frame;
  }

  void setFrame(final JFrame frame) {
    this.frame = frame;
  }

  /**
   * Sets and returns the current message level.
   * 
   * @arg level the new {@link #Level}
   * @return the level passed in.
   */
  public Level setLevel(final Level level) {
    this.level = level;
    return this.level;
  }

  /**
   * Calls {@link #note(Object,Level)} with the current level, which defaults to {@link Level#ERROR}.
   * 
   * @arg msg message to print
   * @return msg passed in
   */
  public Object note(final Object msg) {
    note(msg, level);
    return msg;
  }

  /**
   * Color codes <code>msg</code> according to <code>level</code> and prints it to the screen.
   * 
   * @arg msg message to print
   * @arg level level at which to print <code>msg</code>
   * @return <code>true</code> if we printed something
   */
  public void note(final Object msg, final Level level) {
    //
    // First check whether we should be debugging
    //
    if (!client.getDebug())
      return;
    Color c;
    if (level == Level.MESSAGE) {
      c = Color.black;
    } else if (level == Level.WARNING) {
      c = Color.blue;
    } else if (level == Level.ERROR) {
      c = Color.orange;
    } else if (level == Level.FATAL) {
      c = Color.red;
    } else {
      throw new IllegalArgumentException("Invalid level '" + level + "'");
    }
    c = c.darker();
    DateFormat df = DateFormat.getTimeInstance();
    String s = "[" + simpleName() + "] " + msg + System.getProperty("line.separator");
    SimpleAttributeSet attrs = new SimpleAttributeSet();
    StyleConstants.setForeground(attrs, c);
    try {
      text.getStyledDocument().insertString(text.getStyledDocument().getLength(), s, attrs);
    } catch (BadLocationException e) { e.printStackTrace(); }
  }

  private String simpleName() {
    return Util.simpleName(client.getClass());
  }

  /**
   * Prints a status message.
   * 
   * @arg msg message to set status
   * @return passed in msg
   */
  public Object setStatus(final Object m) {
    String msg = m == null ? "" : String.valueOf(m);
    this.statusLabel.setText(String.valueOf(msg));
    return msg;
  }

  /**
   * Factory method for creating a new instance and popping up a frame.
   * 
   * @arg client proxied client
   * @return new instance
   */
  public static DebugPanel showNewInstance(final Debuggable client) {
    JFrame f = new JFrame(String.valueOf(client));
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    DebugPanel d = newInstance(client);
    d.setFrame(f);
    f.getContentPane().add(d);
    f.pack();
    f.setVisible(true);
    return d;
  }

  public boolean setFrameLocation(final int x, final int y) {
    JFrame f = getFrame();
    if (f == null)
      return false;
    f.setLocation(new Point(x, y));
    return true;
  }

  /**
   * Factory method for creating a new instance.
   * 
   * @arg client proxied client
   * @return new instance
   */
  public static DebugPanel newInstance(final Debuggable client) {
    return new DebugPanel(client);
  }

  // ----------------------------------------------------------------------
  // Implementation
  // ----------------------------------------------------------------------

  /**
   * A proxied client that can turn on and off debugging.
   */
  public interface Debuggable {
    boolean getDebug();

    void setDebug(boolean debug);
  }
  
  /**
   * Simple implementation.
   * 
   * @author jpalm
   */
  private static abstract class DebuggableImpl implements Debuggable {
    private boolean debug;

    DebuggableImpl(final boolean debug) {
      setDebug(debug);
    }

    public final boolean getDebug() {
      return debug;
    }

    public final void setDebug(final boolean debug) {
      this.debug = debug;
    }
  }

  private final Debuggable client;
  private final JTextPane text;
  private final DefaultStyledDocument doc;
  private final JLabel statusLabel = new JLabel("...");

  private DebugPanel(final Debuggable client) {
    this.client = client;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    //
    // text pane
    //
    text = new JTextPane(doc = new DefaultStyledDocument());
    text.setPreferredSize(new Dimension(900, Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 100));
    text.setEditable(false);
    JScrollPane scr = new JScrollPane(text);
    add(scr);
    //
    // toggle button
    //
    JPanel p = new JPanel();
    p.setLayout(new FlowLayout(FlowLayout.LEFT));
    JCheckBox cb = new JCheckBox(new AbstractAction("Debug") {
      public void actionPerformed(final ActionEvent e) {
        DebugPanel.this.client.setDebug(!DebugPanel.this.client.getDebug());
      }
    });
    cb.setSelected(this.client.getDebug());
    p.add(cb);
    p.add(new JButton(new AbstractAction("Clear") {
      public void actionPerformed(final ActionEvent arg0) {
        text.setText("");
      }
    }));
    p.add(statusLabel);
    add(p);
  }

  // ----------------------------------------------------------------------
  // Testing
  // ----------------------------------------------------------------------

  public static void main(final String[] args) throws Exception {
    DebugPanel dp = showNewInstance(new DebuggableImpl(true) {
    });
    final PrintStream out = System.out;
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    String s = "";
    for (;;) {
      out.println("Options:");
      out.println(" - 1: " + Level.MESSAGE);
      out.println(" - 2: " + Level.WARNING);
      out.println(" - 3: " + Level.ERROR);
      out.println(" - 4: " + Level.FATAL);
      out.println(" - 5: " + "Status");
      out.println(" - q: quit");
      out.print("> ");
      out.flush();
      s = in.readLine();
      if (s.startsWith("q") || s.startsWith("Q"))
        break;
      out.print("Enter message: ");
      out.flush();
      String msg = in.readLine();
      if (!"".equals(s)) {
        int val = -1;
        try {
          val = Integer.parseInt(s);
        } catch (NumberFormatException e) { e.printStackTrace(); }
        if (val == -1) {
          System.err.println("Level must be between 1 and 4");
        } else if (val == 5) {
          dp.setStatus(msg);
        } else if (1 <= val && val <= 4) {
          Level level;
          switch (val) {
            case 1:
              level = Level.MESSAGE;
              break;
            case 2:
              level = Level.WARNING;
              break;
            case 3:
              level = Level.ERROR;
              break;
            case 4:
              level = Level.FATAL;
              break;
            default:
              level = Level.FATAL;
              break;
          }
          dp.note(msg, level);
          dp.setLevel(level);
        } else {
          dp.note(msg);
        }
      }
    }
    in.close();
    System.exit(0);
  }
}
