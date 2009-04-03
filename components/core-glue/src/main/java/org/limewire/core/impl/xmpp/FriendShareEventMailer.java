package org.limewire.core.impl.xmpp;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.limewire.collection.Periodic;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.PasswordManager;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.ManagedListStatusEvent;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class FriendShareEventMailer implements RegisteringEventListener<FriendShareListEvent> {

    private static final Log LOG = LogFactory.getLog(FriendShareEventMailer.class);

    private final PasswordManager passwordManager;
    private final ScheduledExecutorService scheduledExecutorService;

    private final Map<String, LibraryChangedSender> listeners;

    // Package private for testing
    final AtomicBoolean fileManagerLoaded = new AtomicBoolean(false);

    @Inject
    FriendShareEventMailer(PasswordManager passwordManager,
            @Named("backgroundExecutor")ScheduledExecutorService scheduledExecutorService) {
        this.passwordManager = passwordManager;
        this.scheduledExecutorService = scheduledExecutorService;
        listeners = new ConcurrentHashMap<String, LibraryChangedSender>();
    }

    @Inject
    public void register(FileManager fileManager) {
        fileManager.getManagedFileList().addManagedListStatusListener(new FinishedLoadingListener());
    }

    @Inject
    public void register(ListenerSupport<FriendShareListEvent> friendShareListEventListenerSupport) {
        friendShareListEventListenerSupport.addListener(this);
    }

    public void handleEvent(final FriendShareListEvent event) {
        if(event.getType() == FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED) {
            LibraryChangedSender listener = new LibraryChangedSender(event.getFriend());
            listeners.put(event.getFriend().getId(), listener);
            event.getFileList().getModel().addListEventListener(listener);
        } else if(event.getType() == FriendShareListEvent.Type.FRIEND_SHARE_LIST_REMOVED) {
            event.getFileList().getModel().removeListEventListener(listeners.remove(event.getFriend().getId()));
        }
    }

    class FinishedLoadingListener implements EventListener<ManagedListStatusEvent> {
        @SuppressWarnings("unchecked")
        @BlockingEvent
        public void handleEvent(ManagedListStatusEvent evt) {
            if(evt.getType() == ManagedListStatusEvent.Type.LOAD_COMPLETE) {
                fileManagerLoaded.set(true);
            }
        }
    }

    class LibraryChangedSender implements ListEventListener<LocalFileItem> {

        private final Friend friend;

        private final Periodic libraryRefreshPeriodic;

        LibraryChangedSender(Friend friend){
            this.friend = friend;
            this.libraryRefreshPeriodic = new Periodic(new ScheduledLibraryRefreshSender(), scheduledExecutorService);
        }

        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            if(fileManagerLoaded.get()) {
                libraryRefreshPeriodic.rescheduleIfLater(5000);
            }
        }

        class ScheduledLibraryRefreshSender implements Runnable {

            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                // TODO only send notification if friend
                // TODO browsed since last notification
                try {
                    sendMail();
                } catch (MessagingException e) {
                    LOG.error("error mailing share event", e);
                }
            }
        }

        private void sendMail() throws MessagingException {
            Network network = friend.getNetwork();
            String from = network.getCanonicalizedLocalID() + "@" + network.getNetworkName();
            String to = friend.getId() + "@" + network.getNetworkName();
            Properties p = new Properties();
            p.put("mail.smtp.user", from);
            p.put("mail.smtp.host", network.getSMTPHost()); //smtp.gmail.com
            p.put("mail.smtp.port", network.getSMTPPort()); // 465
            p.put("mail.smtp.starttls.enable","true");
            p.put("mail.smtp.auth", "true");
            p.put("mail.smtps.auth", "true");
            p.put("mail.smtp.debug", "true");
            p.put("mail.smtp.socketFactory.port", network.getSMTPPort());
            p.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            p.put("mail.smtp.socketFactory.fallback", "false");

            Authenticator auth = new SMTPAuthenticator(network);
            Session session = Session.getInstance(p, auth);
            session.setDebug(true);

            //session = Session.getDefaultInstance(p);
            MimeMessage msg = new MimeMessage(session);
            msg.setText(getBody(friend));
            msg.setSubject(getSubject(friend));
            Address fromAddr = new InternetAddress(from);
            msg.setFrom(fromAddr);
            Address toAddr = new InternetAddress(friend.getId() + "@" + network.getNetworkName());
            msg.addRecipient(Message.RecipientType.TO, toAddr);
            Transport.send(msg);
        }

        private String getSubject(Friend friend) {
            String body = friend.getNetwork().getCanonicalizedLocalID();
            body += " has shared files with you on LimeWire\n";
            return body;
        }

        private String getBody(Friend friend) {
            String body = friend.getNetwork().getCanonicalizedLocalID();
            body += " has shared files with you on LimeWire\n";
            return body;
        }

        private class SMTPAuthenticator extends javax.mail.Authenticator {

            private final Network network;

            public SMTPAuthenticator(Network network) {
                this.network = network;
            }

            public PasswordAuthentication getPasswordAuthentication() {
                try {
                    return new PasswordAuthentication(network.getCanonicalizedLocalID() + "@" + network.getNetworkName(),
                            passwordManager.loadPassword(network.getCanonicalizedLocalID()));
                            // TODO XMPPAccountConfiguration.getUserInputLocalID()
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
