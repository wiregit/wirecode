package org.limewire.ui.swing.animate;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

/**
 * Abstract Animator class that will handle starting and stepping through an
 * animation, then finally stopping. The animation tries to trigger at least 30
 * frames a second, to Keep the animation smooth.
 * 
 * By trigger, we mean a step event is broadcast to all listeners which actually
 * do the work of animating the component.
 */
abstract class AbstractAnimator<T> implements Animator<T> {

    private int TARGET_STEPS_PER_SECOND = 30;

    private final Timer timer;

    private int stepNumber = 0;

    private final int totalNumberOfSteps;

    private final EventListenerList<AnimatorEvent<T>> eventListenerList;

    private final T component;

    public AbstractAnimator(final T component, int totalTime) {
        this.component = component;
        totalNumberOfSteps = (int) Math.ceil((((double) totalTime) / 1000)
                * TARGET_STEPS_PER_SECOND);// trying to get 30 frames a second
        this.eventListenerList = new EventListenerList<AnimatorEvent<T>>();
        this.timer = new Timer(0, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ++stepNumber;
                eventListenerList.broadcast(new AnimatorEvent<T>(AbstractAnimator.this,
                        AnimatorEvent.Type.STEP));
                if (stepNumber == totalNumberOfSteps) {
                    stop();
                }
            }
        });

        timer.setDelay(totalTime / totalNumberOfSteps);// trying to get 30
        // frames a second
    }

    @Override
    public void setInitialDelay(int initialDelay) {
        timer.setInitialDelay(initialDelay);
    }

    @Override
    public void start() {
        timer.start();
        eventListenerList.broadcast(new AnimatorEvent<T>(this, AnimatorEvent.Type.STARTED));
    }

    @Override
    public void stop() {
        eventListenerList.broadcast(new AnimatorEvent<T>(this, AnimatorEvent.Type.STOPPED));
        timer.stop();
    }

    /**
     * Total number of steps this animator will run.
     */
    public int getTotalNumberOfSteps() {
        return totalNumberOfSteps;
    }

    @Override
    public void addListener(EventListener<AnimatorEvent<T>> listener) {
        eventListenerList.addListener(listener);

    }

    @Override
    public boolean removeListener(EventListener<AnimatorEvent<T>> listener) {
        return eventListenerList.removeListener(listener);
    }

    @Override
    public int getCurrentStepNumber() {
        return stepNumber;
    }

    @Override
    public T getComponent() {
        return component;
    }
}
