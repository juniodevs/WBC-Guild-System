package com.guild.core.events;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class EventBus {
    
    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Consumer<?>>> listeners = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger(EventBus.class.getName());
    
    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        logger.info("Registrando ouvinte de evento: " + eventType.getSimpleName());
    }
    
    @SuppressWarnings("unchecked")
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        CopyOnWriteArrayList<Consumer<?>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
            logger.info("Cancelando registro de ouvinte de evento: " + eventType.getSimpleName());
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        CopyOnWriteArrayList<Consumer<?>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<?> listener : eventListeners) {
                try {
                    ((Consumer<T>) listener).accept(event);
                } catch (Exception e) {
                    logger.severe("Falha na execução do listener de evento: " + e.getMessage());
                }
            }
        }
    }
    
    public <T> void publishAsync(T event) {
        new Thread(() -> publish(event)).start();
    }
    
    public void clear() {
        listeners.clear();
        logger.info("Limpar todos os listeners de evento");
    }
    
    public int getListenerCount(Class<?> eventType) {
        CopyOnWriteArrayList<Consumer<?>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }
    
    public int getTotalListenerCount() {
        return listeners.values().stream().mapToInt(CopyOnWriteArrayList::size).sum();
    }
}
