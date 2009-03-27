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
    public static void main(String[] args) throws IOException {
        String path = "/Users/pvertenten/test1";
        int num_of_event_slots = 1;
        int num_of_event_fds = 1;
        kevent[] events_to_monitor = new kevent[num_of_event_fds];
        kevent[] events_data = new kevent[num_of_event_slots];
        
        Pointer user_data;
        
        timespec timeout = new timespec(1, 1);
        
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
        
        Memory memory = new Memory(Pointer.SIZE);
        memory.write(0, new Pointer[] {events_to_monitor[0].getPointer()}, 0, 1);
        
        Memory memory2 = new Memory(Pointer.SIZE);
        memory.write(0, new Pointer[] {events_data[0].getPointer()}, 0, 1);
        
        int test_count = CLibrary.INSTANCE.kevent(kq, events_to_monitor[0].getPointer(), num_of_event_slots, events_data[0].getPointer(), 1, Pointer.NULL);
        System.out.println(CLibrary.INSTANCE.strerror(Native.getLastError()));
        
        
        while(true) {
            
            
            
            int event_count = CLibrary.INSTANCE.kevent(kq, events_to_monitor[0].getPointer(), 1, events_data[0].getPointer(), num_files, Pointer.NULL);
            
            System.out.println(CLibrary.INSTANCE.strerror(Native.getLastError()));
            
            if(event_count < 0) {
                throw new IOException("Error reading events");
            }
            events_data[0].read();
            
            System.out.println(events_data[0]);
        
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
