package com.limegroup.gnutella.library.monitor.inotify;

/*
 * This is adapted from 
 * Jason Venner's example on the jna mailing lists.
 */
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class InotifyEvent implements Runnable {

    /**
     * The queue that the run method unpacks events onto.
     */
    private Queue<Event> eventQueue;

    protected static Logger logger = Logger.getLogger(InotifyEvent.class);

    public final static int IN_ACCESS = 0x00000001; /* File was accessed. */

    public final static int IN_MODIFY = 0x00000002; /* File was modified. */

    public final static int IN_ATTRIB = 0x00000004; /* Metadata changed. */

    public final static int IN_CLOSE_WRITE = 0x00000008; /*
                                                          * Writtable file was
                                                          * closed.
                                                          */

    public final static int IN_CLOSE_NOWRITE = 0x00000010; /*
                                                            * Unwrittable file
                                                            * closed.
                                                            */

    public final static int IN_OPEN = 0x00000020; /* File was opened. */

    public final static int IN_MOVED_FROM = 0x00000040; /*
                                                         * File was moved from
                                                         * X.
                                                         */

    public final static int IN_MOVED_TO = 0x00000080; /* File was moved to Y. */

    public final static int IN_CREATE = 0x00000100; /* Subfile was created. */

    public final static int IN_DELETE = 0x00000200; /* Subfile was deleted. */

    public final static int IN_DELETE_SELF = 0x00000400; /* Self was deleted. */

    public final static int IN_MOVE_SELF = 0x00000800; /* Self was moved. */

    /* Events sent by the kernel. */
    public final static int IN_UNMOUNT = 0x00002000; /*
                                                      * Backing fs was
                                                      * unmounted.
                                                      */

    public final static int IN_Q_OVERFLOW = 0x00004000; /*
                                                         * Event queued
                                                         * overflowed.
                                                         */

    public final static int IN_IGNORED = 0x00008000; /* File was ignored. */

    public final static int IN_CLOSE = (IN_CLOSE_WRITE | IN_CLOSE_NOWRITE); /*
                                                                             * Close.
                                                                             */

    public final static int IN_MOVE = (IN_MOVED_FROM | IN_MOVED_TO); /* Moves. */

    /* Special flags. */
    public final static int IN_ONLYDIR = 0x01000000; /*
                                                      * Only watch the path if
                                                      * it is a directory.
                                                      */

    public final static int IN_DONT_FOLLOW = 0x02000000; /*
                                                          * Do not follow a sym
                                                          * link.
                                                          */

    public final static int IN_MASK_ADD = 0x20000000; /*
                                                       * Add to the mask of an
                                                       * already existing watch.
                                                       */

    public final static int IN_ISDIR = 0x40000000; /*
                                                    * Event occurred against
                                                    * dir.
                                                    */

    public final static int IN_ONESHOT = 0x80000000; /* Only send event once. */

    /* All events which a program can wait on. */
    public final static int IN_ALL_EVENTS = (IN_ACCESS | IN_MODIFY | IN_ATTRIB | IN_CLOSE_WRITE
            | IN_CLOSE_NOWRITE | IN_OPEN | IN_MOVED_FROM | IN_MOVED_TO | IN_CREATE | IN_DELETE
            | IN_DELETE_SELF | IN_MOVE_SELF);

    /**
     * Decode the inotify masks.
     * 
     * @param mask The mask to decode, only known bits are decoded.
     * @param sb a StringBuffer to build the result in. May be null
     * @return The decoded string.
     */
    public static String decodeMask(final int mask, StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }
        int len = sb.length();
        if ((mask & IN_ACCESS) == IN_ACCESS) {
            sb.append("IN_ACCESS|");
        }

        if ((mask & IN_MODIFY) == IN_MODIFY) {
            sb.append("IN_MODIFY|");
        }

        if ((mask & IN_ATTRIB) == IN_ATTRIB) {
            sb.append("IN_ATTRIB|");
        }

        if ((mask & IN_CLOSE_WRITE) == IN_CLOSE_WRITE) {
            sb.append("IN_CLOSE_WRITE|");
        }

        if ((mask & IN_CLOSE_NOWRITE) == IN_CLOSE_NOWRITE) {
            sb.append("IN_CLOSE_NOWRITE|");
        }

        if ((mask & IN_OPEN) == IN_OPEN) {
            sb.append("IN_OPEN|");
        }

        if ((mask & IN_MOVED_FROM) == IN_MOVED_FROM) {
            sb.append("IN_MOVED_FROM|");
        }

        if ((mask & IN_MOVED_TO) == IN_MOVED_TO) {
            sb.append("IN_MOVED_TO|");
        }

        if ((mask & IN_CREATE) == IN_CREATE) {
            sb.append("IN_CREATE|");
        }

        if ((mask & IN_DELETE) == IN_DELETE) {
            sb.append("IN_DELETE|");
        }

        if ((mask & IN_DELETE_SELF) == IN_DELETE_SELF) {
            sb.append("IN_DELETE_SELF|");
        }

        if ((mask & IN_MOVE_SELF) == IN_MOVE_SELF) {
            sb.append("IN_MOVE_SELF|");
        }

        if ((mask & IN_UNMOUNT) == IN_UNMOUNT) {
            sb.append("IN_UNMOUNT|");
        }

        if ((mask & IN_Q_OVERFLOW) == IN_Q_OVERFLOW) {
            sb.append("IN_Q_OVERFLOW|");
        }

        if ((mask & IN_IGNORED) == IN_IGNORED) {
            sb.append("IN_IGNORED|");
        }

        if ((mask & IN_ONLYDIR) == IN_ONLYDIR) {
            sb.append("IN_ONLYDIR|");
        }

        if ((mask & IN_DONT_FOLLOW) == IN_DONT_FOLLOW) {
            sb.append("IN_DONT_FOLLOW|");
        }

        if ((mask & IN_MASK_ADD) == IN_MASK_ADD) {
            sb.append("IN_MASK_ADD|");
        }

        if ((mask & IN_ISDIR) == IN_ISDIR) {
            sb.append("IN_ISDIR|");
        }

        if ((mask & IN_ONESHOT) == IN_ONESHOT) {
            sb.append("IN_ONESHOT|");
        }

        // If anything was added to sb, trim the trailing '|'
        if (len != sb.length()) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /** Provide the low level access to the Clibrary */
    public static final CLibrary l = CLibrary.INSTANCE;

    /** Provide the hooks for parsing out the Inotify Event objects. */
    InotifyEventPayload pl = new InotifyEventPayload();

    /**
     * The watch event for an inotify watch. Since the native objects are
     * variable size we need to construct this out of the memory that the native
     * items were read into.
     * 
     */
    public static class Event {
        /** The return value from the inotify_add_watch that set the watch for. */
        public Integer wd;

        /** The flags that are relevant for this event. */
        public int mask;

        /**
         * The cookie to tie this event with other watches for other directories
         * - MOVE_TO, MOVE_FROM...
         */
        public int cookie;

        /**
         * The actual byte length of the full payload object including the
         * padding on the path.
         */
        public int len;

        /** The path for this event. */
        public String path;

        /** The parent directory if the original watch is on a directory. */
        public String watchParent;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[Wd=");
            sb.append(wd);
            sb.append(", ");
            sb.append("Mask=");
            decodeMask(mask, sb);
            sb.append(", ");
            sb.append("Cookie=");
            sb.append(", ");
            sb.append("Len=");
            sb.append(len);
            sb.append(", ");
            sb.append(path);
            if (watchParent != null) {
                sb.append(", ");
                sb.append(watchParent);
            }
            sb.append("] ");
            return sb.toString();
        }

    }

    /**
     * Get the <code>EventQueue</code> value.
     * 
     * @return a <code>Queue<Event></code> value
     */
    public final Queue<Event> getEventQueue() {
        return eventQueue;
    }

    /**
     * Set the <code>EventQueue</code> value.
     * 
     * @param newEventQueue The new EventQueue value.
     */
    public final void setEventQueue(final Queue<Event> newEventQueue) {
        this.eventQueue = newEventQueue;
    }

    /**
     * The object used to unpack an event payload.
     * 
     * The payload objects are not fixed size, they are essentially the core 4
     * field structure with some number of chars and some null bytes. The len
     * field indicates the number of bytes after the core field, and bytes will
     * be a null terminated c string. There may be more than one null byte at
     * the end of the string. This is to ensure that the next payload starts on
     * the appropriately aligned boundary.
     * 
     * This object mimics the inotify_event structure.
     */
    protected class InotifyEventPayload extends com.sun.jna.Structure {
        public class ByValue extends InotifyEventPayload implements Structure.ByValue {
        }

        /** The id of the tracker that requested this event. */
        public int wd;

        public int mask;

        public int cookie;

        public int len;

        public Event set(Memory m, int offset, Event e, InotifyEvent watcher) {
            useMemory(m, offset);
            read();
            if (e == null) {
                e = new Event();
            }
            e.wd = wd;
            e.watchParent = watcher.getPathByWatchDescriptor(e.wd);
            e.mask = mask;
            e.cookie = cookie;
            int baseSize = size();
            int byteOffset = offset + baseSize;
            if (logger.isDebugEnabled()) {
                logger.debug("String offset based on len " + len + " is " + byteOffset);
            }
            e.path = m.getString(byteOffset);
            e.len = offset + baseSize + len;
            return e;
        }
    };

    /** Hook to JNA to enable clibrary access for the Inotify specific bits. */
    interface CLibrary extends Library {

        CLibrary INSTANCE = (CLibrary) Native.loadLibrary((Platform.isWindows() ? "msvcrt" : "c"),
                CLibrary.class);

        int inotify_init();

        int inotify_add_watch(int fd, String pathname, int mask);

        int inotify_rm_watch(int fd, int wd);

        int read(int fd, Pointer buf, int size);

        int close(int fd);

        void perror(String message);
    }

    /** The handle returned by inotify_init. */
    protected int watchHandle = -1;

    /** The map of watch descriptors to watch paths. */
    Map<Integer, String> pathByWatchDescriptor = new HashMap<Integer, String>();

    Set<Integer> oneShotWatchDescriptors = new HashSet<Integer>();

    /** Insure the handle is closed when this instance goes bybye. */
    protected void finalize() {
        shutdown();
    }

    /** Initialize the inotify interface if needed. This method is idempotent. */
    public synchronized void init() throws IOException {
        if (watchHandle == -1) {
            watchHandle = l.inotify_init();
            if (watchHandle == -1) {
                throw new IOException("Unable to initialize inotify subsystem");
            }
        }
    }

    public synchronized void shutdown() {
        if (watchHandle != -1) {
            l.close(watchHandle);
            watchHandle = -1;
            pathByWatchDescriptor.clear(); // They go when the descriptor is
            // closed.
        }
    }

    public synchronized Integer register(String path, int flags) throws IOException {
        init();
        Integer wd = l.inotify_add_watch(watchHandle, path, flags);
        if (logger.isDebugEnabled()) {
            logger.debug("Registering watch on " + path + " for " + decodeMask(flags, null)
                    + " watch descriptor " + wd);
        }
        if (wd == -1) {
            throw new IOException("WatchHandle " + watchHandle + " Unable to register " + path
                    + " with flags " + flags);
        }
        pathByWatchDescriptor.put(wd, path);
        if ((flags & IN_ONESHOT) == IN_ONESHOT) {
            oneShotWatchDescriptors.add(wd);
        }
        return wd;
    }

    public synchronized boolean unregister(Integer wd) {
        if (watchHandle == -1) {
            return false;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("UnRegistering watch descriptor " + wd + " path? "
                    + pathByWatchDescriptor.get(wd) + " oneShot? "
                    + oneShotWatchDescriptors.contains(wd));
        }

        if (l.inotify_rm_watch(watchHandle, wd) == 0) {
            pathByWatchDescriptor.remove(wd);
            oneShotWatchDescriptors.remove(wd);
            return true;
        }
        return false;
    }

    public synchronized String getPathByWatchDescriptor(Integer wd) {
        return pathByWatchDescriptor.get(wd);
    }

    public Queue<Event> read(final Memory p, final Queue<Event> events) throws InterruptedException {

        logger.debug("Issuing a read");
        int count = l.read(watchHandle, p, (int) p.getSize());
        if (count == -1) {
            logger
                    .error("Inotify Read failed, probably due to insufficient free space in the buffer");
            // l.perror( "Error reading events" );
            return null;
        }

        int consumed = 0;
        while (consumed < (int) count) {
            if (logger.isDebugEnabled()) {
                logger.debug("Trying at " + consumed);
            }
            Event e = new Event();
            pl.set(p, consumed, e, this);
            consumed += e.len;
            events.add(e);
            if (logger.isDebugEnabled()) {
                logger.debug("Have " + e.wd + ", " + e.mask + ", " + e.cookie + ", " + e.path + " "
                        + e.watchParent + " remaning data " + (count - consumed));
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Returning " + events.size() + " events ");
        }
        return events;
    }

    @Override
    public void run() {

        try {
            Memory p = new Memory(64 * 1024);
            logger.info("WatchHandle is " + watchHandle + " size is " + p.getSize());
            while (watchHandle != -1) {
                if (read(p, eventQueue) == null) {
                    logger.error("Unable to read from the watch handle. Aborting");
                    return;
                }
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted", e);
        }
    }

    public static void main(String[] args) throws Exception {
        InotifyEvent inotifyEvent = new InotifyEvent();

        TreeMap<Integer, String> watches = new TreeMap<Integer, String>();
        if (args.length == 0) {
            Integer tmpWd = inotifyEvent.register("/tmp", IN_ALL_EVENTS);
            watches.put(tmpWd, "/tmp");
        } else {
            for (String arg : args) {
                Integer wd = inotifyEvent.register(arg, IN_ALL_EVENTS);
                watches.put(wd, arg);
            }
        }

        LinkedBlockingQueue<Event> queue = new LinkedBlockingQueue<Event>();
        inotifyEvent.init();
        inotifyEvent.setEventQueue(queue);
        Thread running = new Thread(inotifyEvent);

        /** So if the main is interrupted, the jvm will exit. */
        running.setDaemon(true);
        running.start();

        Event e;
        while (running.isAlive() && !running.isInterrupted()) {
            e = queue.take();
            System.out.println(e);
        }
    }

}
