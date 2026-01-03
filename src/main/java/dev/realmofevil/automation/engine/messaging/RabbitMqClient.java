package dev.realmofevil.automation.engine.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.reporting.StepReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Engine wrapper for RabbitMQ interactions.
 * Handles JSON serialization and Connection lifecycle.
 */
public class RabbitMqClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMqClient.class);

    private final Connection connection;
    private final Channel channel;
    private final ObjectMapper mapper;

    public RabbitMqClient(OperatorConfig.RabbitConfig config, ObjectMapper mapper) {
        this.mapper = mapper;
        if (config == null) {
            this.connection = null;
            this.channel = null;
            return;
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.host());
        if (config.port() > 0)
            factory.setPort(config.port());
        if (config.virtualHost() != null)
            factory.setVirtualHost(config.virtualHost());

        factory.setUsername(config.username().plainText());
        factory.setPassword(config.password().plainText());
        factory.setAutomaticRecoveryEnabled(true);

        try {
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            LOG.info("Connected to RabbitMQ at {}", config.host());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to connect to RabbitMQ", e);
        }
    }

    public void publish(String exchange, String routingKey, Object message) {
        if (channel == null) {
            throw new IllegalStateException("RabbitMQ is not configured for this operator.");
        }

        try {
            byte[] body = mapper.writeValueAsBytes(message);
            channel.basicPublish(exchange, routingKey, null, body);

            StepReporter.info("Published RabbitMQ Message to '" + exchange + "' : '" + routingKey + "'");
            StepReporter.attachJson("RabbitMQ Payload", new String(body));

        } catch (IOException e) {
            throw new RuntimeException("Failed to publish message to RabbitMQ", e);
        }
    }

    @Override
    public void close() {
        try {
            if (channel != null && channel.isOpen())
                channel.close();
            if (connection != null && connection.isOpen())
                connection.close();
        } catch (Exception e) {
            LOG.warn("Error closing RabbitMQ connection", e);
        }
    }

    public boolean isConfigured() {
        return channel != null;
    }
}