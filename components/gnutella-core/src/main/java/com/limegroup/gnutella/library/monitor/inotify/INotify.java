package com.limegroup.gnutella.library.monitor.inotify;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * Adapted from Jason Venner's code at http://markmail.org/message/y7h3koshjl34bbrn
 */
/** Hook to JNA to enable clibrary access for the Inotify specific bits. */
interface INotify extends Library {

    int inotify_init();

    int inotify_add_watch(int fd, String pathname, int mask);

    int inotify_rm_watch(int fd, int wd);

    int read(int fd, Pointer buf, int size);

    int close(int fd);
}