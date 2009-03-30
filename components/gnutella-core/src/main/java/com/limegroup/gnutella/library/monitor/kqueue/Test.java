package com.limegroup.gnutella.library.monitor.kqueue;

import java.io.IOException;

import com.sun.jna.FromNativeContext;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Union;
import com.sun.jna.ptr.PointerByReference;

public class Test {
    public static void main(String[] args) throws IOException, InterruptedException {
        String path = "/Users/pvertenten/test1";
        int num_of_event_slots = 1;
        int num_of_event_fds = 1;
        kevent[] events_to_monitor = new kevent[num_of_event_fds];
        kevent[] events_data = new kevent[num_of_event_slots];
        
        Pointer user_data;
        
        timespec timeout = new timespec(0, 50000000);
        
        int kq = CLibrary.INSTANCE.kqueue();
        
        if(kq < 0) {
            throw new IOException("Failure initializing kqueue.");
        }
        
        int event_fd = CLibrary.INSTANCE.open(path, CLibrary.O_EVTONLY);

        if(event_fd < 0) {
            throw new IOException("Error opening file for monitoring.");
        }
        //U u = new U(path);
        user_data = Pointer.NULL;
        
        int vnode_events = KQueueEventMask.ALL_EVENTS.getMask();
        //EV_SET
        events_to_monitor[0] = new kevent();
        events_to_monitor[0].ident = event_fd;
        events_to_monitor[0].filter = CLibrary.EVFILT_VNODE;
        events_to_monitor[0].flags = CLibrary.EV_ADD | CLibrary.EV_CLEAR;
        events_to_monitor[0].fflags = vnode_events;
        events_to_monitor[0].data = 0;
        events_to_monitor[0].udata = null;
        
        events_to_monitor[0].write();
        
       
        int num_files = 1;
        events_data[0] = new kevent();
        events_data[0].write(); 
       
        
        int test_count = CLibrary.INSTANCE.kevent(kq, events_to_monitor, 1, events_data, 1, null);
        if(test_count < 0) {
            System.out.println(CLibrary.INSTANCE.strerror(Native.getLastError()));
            throw new IOException("Failure registering events.");
        }
        
        
        while(true) {
            
            
            
            int event_count = CLibrary.INSTANCE.kevent(kq, null, 0, events_data, num_files, Pointer.NULL);
            
           
            
            if(event_count < 0) {
                System.out.println(CLibrary.INSTANCE.strerror(Native.getLastError()));
                throw new IOException("Error reading events");
            }
            
            kevent resultEvent = events_data[0];
            resultEvent.read();
            
            if(resultEvent.flags == CLibrary.EV_ERROR) {
                System.out.println(CLibrary.INSTANCE.strerror(Native.getLastError()));
                throw new IOException("Error reading events");
            }
            System.out.println(resultEvent);
            
            if (KQueueEventMask.NOTE_DELETE.isSet(resultEvent.fflags)) {
                System.out.println("delete");
            }
            if (KQueueEventMask.NOTE_RENAME.isSet(resultEvent.fflags)) {
                System.out.println("renamed");
            }
            if (KQueueEventMask.NOTE_EXTEND.isSet(resultEvent.fflags)
                    || KQueueEventMask.NOTE_WRITE.isSet(resultEvent.fflags)) {
                System.out.println("file size changed");
            }
            if (KQueueEventMask.NOTE_ATTRIB.isSet(resultEvent.fflags)) {
                System.out.println("attributes changed");
            }
            
            System.out.println("udata: " + (resultEvent.udata == Pointer.NULL));
            
            Thread.sleep(5000);
        }
        
    }
    
    public static class KEventArray extends PointerType {
        public KEventArray(kevent[] events) {
           
        }
        
        @Override
        public Object fromNative(Object arg0, FromNativeContext arg1) {
            // TODO Auto-generated method stub
            return super.fromNative(arg0, arg1);
        }
    }
    public static class U extends PointerType {
        public U(String str) {
            getPointer().setString(0, str);
        }
    }
    class UData extends Union {
        Pointer u_ptr;
        int u_int;
    }
}
