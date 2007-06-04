package org.limewire.store.storeserver.local;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.limewire.store.storeserver.api.AbstractDispatchee;
import org.limewire.store.storeserver.api.Server;
import org.limewire.store.storeserver.util.CenterHelper;


/**
 * Pops up a java thing with a shopping cart to stay in sync with the page.
 * 
 * @author jpalm
 */
class ShoppingCartDemo {

  public static void main(String[] args) {
    new ShoppingCartDemo().realMain(args);
  }
  
  private final Demo main = new Demo();
  private final ShoppingCartPane pane = new ShoppingCartPane();
  private final ShoppingCartDispatcher dispatcher = new ShoppingCartDispatcher();

  private void realMain(String[] args) {
    main.start();
    main.getLocalServer().setDispatchee(dispatcher);
    JFrame f = new JFrame("Shopping cart demo");
    f.add(pane);
    f.pack();
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    CenterHelper.center(f);
    f.setVisible(true);
  }
  
  /** Has a queue of messages. */
  final class ShoppingCartDispatcher extends AbstractDispatchee {
    
    private final List<String> queue = new ArrayList<String>();

    public ShoppingCartDispatcher() {
      super(main.getLocalServer());
    }

    public String dispatch(final String cmd, final Map<String, String> args) {
      if (cmd.equals("GetMsg")) {
        if (queue.isEmpty()) {
          return "0";
        } else {
          final String msg = queue.remove(0);
          return msg;
        }
      } else if (cmd.equals("SetStatus")) {
        final String count = args.get("count");
        final String price = args.get("price");
        pane.setCount(count);
        pane.setPrice(price);
        return Server.Responses.OK;
      }
      return Server.Responses.UNKNOWN_COMMAND + ":" + cmd + ":" + args;
    }
    
    public void postMsg(String msg) {
      queue.add(msg);
      System.out.println("msgs: " + queue);
    }

    @Override
    protected void connectionChanged(boolean isConnected) {
        System.out.println("connectionChanged: " + isConnected);
    }
    
  }
  
  /** Pane with count, price, and clear. */
  final class ShoppingCartPane extends JPanel {
    
    private final JLabel countLabel = new JLabel("0");
    private final JLabel priceLabel = new JLabel("0.00");
        
    ShoppingCartPane() {
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      add(new JLabel("Shopping Cart on Client"));
      final JPanel p = new JPanel(new GridLayout(2, 2));
      p.add(new JLabel("Count"));
      p.add(countLabel);
      p.add(new JLabel("Price $"));
      p.add(priceLabel);
      add(p);
      add(new JButton(new AbstractAction("Clear") {
        public void actionPerformed(final ActionEvent e) {
          dispatcher.postMsg("Clear");
        }
      }));
      add(new JButton(new AbstractAction("Add") {
        public void actionPerformed(final ActionEvent e) {
          dispatcher.postMsg("Add");
        }
      }));

    }
    
    public void setCount(String s) {
      countLabel.setText(s);
    }

    public void setPrice(String s) {
      priceLabel.setText(s);
    }

  }

}
