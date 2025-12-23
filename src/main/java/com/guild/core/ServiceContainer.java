package com.guild.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class ServiceContainer {
    
    private final Map<Class<?>, Object> services = new HashMap<>();
    private final Map<Class<?>, ServiceLifecycle> lifecycles = new HashMap<>();
    private final Logger logger = Logger.getLogger(ServiceContainer.class.getName());
    
    public <T> void register(Class<T> serviceClass, T service) {
        services.put(serviceClass, service);
        logger.info("Registrando serviço: " + serviceClass.getSimpleName());
    }
    
    public <T> void register(Class<T> serviceClass, T service, ServiceLifecycle lifecycle) {
        services.put(serviceClass, service);
        lifecycles.put(serviceClass, lifecycle);
        logger.info("Registrando serviço: " + serviceClass.getSimpleName() + " (com ciclo de vida)");
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> serviceClass) {
        T service = (T) services.get(serviceClass);
        if (service == null) {
            throw new ServiceNotFoundException("Serviço não encontrado: " + serviceClass.getName());
        }
        return service;
    }
    
    public boolean has(Class<?> serviceClass) {
        return services.containsKey(serviceClass);
    }
    
    public CompletableFuture<Void> startAll() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Iniciando todos os serviços...");
            for (Map.Entry<Class<?>, ServiceLifecycle> entry : lifecycles.entrySet()) {
                try {
                    entry.getValue().start();
                    logger.info("Serviço iniciado com sucesso: " + entry.getKey().getSimpleName());
                } catch (Exception e) {
                    logger.severe("Falha ao iniciar serviço: " + entry.getKey().getSimpleName() + " - " + e.getMessage());
                }
            }
        });
    }
    
    public CompletableFuture<Void> stopAll() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Parando todos os serviços...");
            for (Map.Entry<Class<?>, ServiceLifecycle> entry : lifecycles.entrySet()) {
                try {
                    entry.getValue().stop();
                    logger.info("Serviço parado com sucesso: " + entry.getKey().getSimpleName());
                } catch (Exception e) {
                    logger.severe("Falha ao parar serviço: " + entry.getKey().getSimpleName() + " - " + e.getMessage());
                }
            }
        });
    }
    
    public void shutdown() {
        try {
            stopAll().get();
            services.clear();
            lifecycles.clear();
            logger.info("Container de serviços fechado");
        } catch (Exception e) {
            logger.severe("Erro ao fechar container de serviços: " + e.getMessage());
        }
    }
    
    public interface ServiceLifecycle {
        void start() throws Exception;
        void stop() throws Exception;
    }
    
    public static class ServiceNotFoundException extends RuntimeException {
        public ServiceNotFoundException(String message) {
            super(message);
        }
    }
}
