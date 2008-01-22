import java.util.Set;

import org.limewire.io.IpPort;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallbackAdapter;
import com.limegroup.gnutella.LimeWireCore;
import com.limegroup.gnutella.LimeWireCoreModule;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.search.HostData;

public class TutorialSearchResults {

  public static int queryHits = 0;

  @Singleton
  private static class ConsoleActivityCallback extends
      ActivityCallbackAdapter {
    public void handleQueryResult(RemoteFileDesc rfd, HostData data,
        Set<? extends IpPort> loc) {
      synchronized (System.out) {
        System.out.println("Query hit from " + rfd.getHost() + ":"
            + rfd.getPort() + ":");
        System.out.println("   " + rfd.getFileName());
        queryHits++;
      }
    }
  }

  public static void main(String[] args) {
    Injector injector = Guice.createInjector(new LimeWireCoreModule(
        ConsoleActivityCallback.class));
    LimeWireCore core = injector.getInstance(LimeWireCore.class);

    core.getLifecycleManager().start();
    String query = "speeches";
    System.out.println("Connect to the Gnutella network and search for \""
        + query + "\"");

    while (!core.getConnectionManager().isConnected()) {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    System.out.println("Connected.");
    SearchServices searchServices = core.getSearchServices();
    searchServices.query(searchServices.newQueryGUID(), query);

    while (TutorialSearchResults.queryHits < 1) {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    System.out.println("Got " + TutorialSearchResults.queryHits
        + " query hits, now exiting.");

    core.getLifecycleManager().shutdown();
  }

}