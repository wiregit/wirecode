package com.limegroup.gnutella.stubs;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ScheduledExecutorServiceStub implements ScheduledExecutorService {

    public ScheduledFuture<?> schedule(Runnable command, long delay,
            TimeUnit unit) {
        // TODO Auto-generated method stub
        return null;
    }

    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay,
            TimeUnit unit) {
        // TODO Auto-generated method stub
        return null;
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
            long initialDelay, long period, TimeUnit unit) {
        // TODO Auto-generated method stub
        return null;
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
            long initialDelay, long delay, TimeUnit unit) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }

    public <T> List<Future<T>> invokeAll(Collection<Callable<T>> tasks)
            throws InterruptedException {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> List<Future<T>> invokeAll(Collection<Callable<T>> tasks,
            long timeout, TimeUnit unit) throws InterruptedException {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> T invokeAny(Collection<Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> T invokeAny(Collection<Callable<T>> tasks, long timeout,
            TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isShutdown() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isTerminated() {
        // TODO Auto-generated method stub
        return false;
    }

    public void shutdown() {
        // TODO Auto-generated method stub

    }

    public List<Runnable> shutdownNow() {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> Future<T> submit(Callable<T> task) {
        // TODO Auto-generated method stub
        return null;
    }

    public Future<?> submit(Runnable task) {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> Future<T> submit(Runnable task, T result) {
        // TODO Auto-generated method stub
        return null;
    }

    public void execute(Runnable command) {
        // TODO Auto-generated method stub

    }

}
