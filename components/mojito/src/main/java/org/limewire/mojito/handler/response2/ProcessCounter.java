package org.limewire.mojito.handler.response2;

class ProcessCounter {

    private final int maxProcesses;
    
    private int process = 0;
    
    public ProcessCounter(int maxProcesses) {
        if (maxProcesses < 0) {
            throw new IllegalArgumentException(
                    "maxProcesses=" + maxProcesses);
        }
        
        this.maxProcesses = maxProcesses;
    }
    
    public boolean hasNext() {
        return process < maxProcesses;
    }
    
    public boolean increment() {
        return increment(false);
    }
    
    public boolean increment(boolean force) {
        if (process < maxProcesses || force) {
            ++process;
            return true;
        }
        return false;
    }
    
    public void decrement() {
        if (0 < process) {
            --process;
        }
    }
    
    public int get() {
        return process;
    }
}
