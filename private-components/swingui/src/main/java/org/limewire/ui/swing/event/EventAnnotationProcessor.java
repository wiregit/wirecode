package org.limewire.ui.swing.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import org.bushe.swing.event.EventService;
import org.bushe.swing.event.EventServiceExistsException;
import org.bushe.swing.event.EventServiceLocator;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.bushe.swing.event.annotation.EventTopicSubscriber;
import org.bushe.swing.event.annotation.EventTopicPatternSubscriber;
import org.bushe.swing.event.annotation.ProxyTopicSubscriber;
import org.bushe.swing.event.annotation.ReferenceStrength;

public class EventAnnotationProcessor {

    private EventAnnotationProcessor() {
    }

    public static void subscribe(Object subscriber) {
        if (subscriber == null) {
            return;
        }
        Class<?> cl = subscriber.getClass();
        Method[] methods = cl.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.isAnnotationPresent(RuntimeTopicEventSubscriber.class)) {
                RuntimeTopicEventSubscriber annotation = method.getAnnotation(RuntimeTopicEventSubscriber.class);
                if (annotation != null) {
                    process(annotation, subscriber, method);
                }
            }
        }
        org.bushe.swing.event.annotation.AnnotationProcessor.process(subscriber);
    }

    private static void process(final RuntimeTopicEventSubscriber annotation, final Object subscriber, final Method method) {
        EventTopicSubscriber eventTopicSubscriber = new EventTopicSubscriber() {
            @Override
            public Class<? extends EventService> autoCreateEventServiceClass() {
                return annotation.autoCreateEventServiceClass();
            }

            @Override
            public String eventServiceName() {
                return annotation.eventServiceName();
            }

            @Override
            public ReferenceStrength referenceStrength() {
                return ReferenceStrength.WEAK;
            }

            @Override
            public String topic() {
                String t = getTopic(annotation, subscriber, method);
                return t;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return annotation.annotationType();
            }
        };
        process(eventTopicSubscriber, subscriber, method);
    }

    private static String getTopic(RuntimeTopicEventSubscriber annotation, Object subscriber, Method method) {
        try {
            //TODO: Confirm that subscriber class has this method
            Method runtimeEvalMethod = subscriber.getClass().getMethod(annotation.methodName(), new Class[0]);
            return runtimeEvalMethod.invoke(subscriber, new Object[0]).toString();
        } catch (SecurityException e) {
            throw new RuntimeException("Could not retrieve method for subscription. Method: " + annotation.methodName(), e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not retrieve method for subscription. Method: " + annotation.methodName(), e);
        } catch (InvocationTargetException e) {
            e.getTargetException().printStackTrace();
            throw new RuntimeException("Could not invoke method for subscription. Method: " + annotation.methodName(), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not invoke method for subscription. Method: " + annotation.methodName(), e);
        }
    }
    
    private static void process(EventTopicSubscriber topicAnnotation, Object obj, Method method) {
        String topic = topicAnnotation.topic();
        if (topic == null) {
            throw new IllegalArgumentException("Topic cannot be null for EventTopicSubscriber annotation");
        }

        Class<? extends EventService> eventServiceClass = topicAnnotation.autoCreateEventServiceClass();
        String eventServiceName = topicAnnotation.eventServiceName();
        EventService eventService = getEventServiceFromAnnotation(eventServiceName, eventServiceClass);

        ProxyTopicSubscriber subscriber = new ProxyTopicSubscriber(obj, method, topicAnnotation.referenceStrength(), eventService, topic);

        eventService.subscribeStrongly(topic, subscriber);
    }

    private static EventService getEventServiceFromAnnotation(String eventServiceName, Class<? extends EventService> eventServiceClass) {
        EventService eventService = EventServiceLocator.getEventService(eventServiceName);
        if (eventService == null) {
            if (EventServiceLocator.SERVICE_NAME_EVENT_BUS.equals(eventServiceName)) {
                eventService = EventServiceLocator.getSwingEventService();
            } else {
                // The event service does not yet exist, create it
                try {
                    eventService = eventServiceClass.newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException("Could not instance of create EventService class " + eventServiceClass, e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Could not instance of create EventService class " + eventServiceClass, e);
                }
                try {
                    EventServiceLocator.setEventService(eventServiceName, eventService);
                } catch (EventServiceExistsException e) {
                    // ignore it, it's OK
                    eventService = EventServiceLocator.getEventService(eventServiceName);
                }
            }
        }
        return eventService;
    }

    public static void unsubscribe(Object subscriber) {
        if (subscriber == null) {
            return;
        }
        Class<?> cl = subscriber.getClass();
        Method[] methods = cl.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            RuntimeTopicEventSubscriber dynamicTopicSubscriber = method
                    .getAnnotation(RuntimeTopicEventSubscriber.class);
            if (dynamicTopicSubscriber != null) {
                EventService eventService = EventServiceLocator.getEventService(dynamicTopicSubscriber.eventServiceName());
                eventService.unsubscribe(getTopic(dynamicTopicSubscriber, subscriber, method), subscriber);
            }
           EventSubscriber classAnnotation = method.getAnnotation(EventSubscriber.class);
           if (classAnnotation != null) {
               EventService eventService = EventServiceLocator.getEventService(classAnnotation.eventServiceName());
               if (classAnnotation.exact()) {
                   eventService.unsubscribeExactly(classAnnotation.eventClass(), subscriber);
               } else {
                   eventService.unsubscribe(classAnnotation.eventClass(), subscriber);
               }
           }
           EventTopicSubscriber topicAnnotation = method.getAnnotation(EventTopicSubscriber.class);
           if (topicAnnotation != null) {
               EventService eventService = EventServiceLocator.getEventService(topicAnnotation.eventServiceName());
               eventService.unsubscribe(topicAnnotation.topic(), subscriber);
           }
           EventTopicPatternSubscriber topicPatternAnnotation = method.getAnnotation(EventTopicPatternSubscriber.class);
           if (topicPatternAnnotation != null) {
               EventService eventService = EventServiceLocator.getEventService(topicPatternAnnotation.eventServiceName());
               eventService.unsubscribe(Pattern.compile(topicPatternAnnotation.topicPattern()),
                       subscriber);
           }
       }
   }
}