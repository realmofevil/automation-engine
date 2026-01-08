package dev.realmofevil.automation.engine.messaging;

/**
 * Port for asynchronous messaging (Hexagonal Architecture).
 */
public interface MessagingPort extends AutoCloseable {
    void publish(String exchange, String routingKey, Object message);
    boolean isConfigured();
    
    @Override
    void close();
}