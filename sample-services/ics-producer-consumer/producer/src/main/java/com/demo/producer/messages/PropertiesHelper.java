package com.demo.producer.messages;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.demo.producer.producer.SimpleProducer;

@Component
public class PropertiesHelper {
    private static final Logger log = LoggerFactory.getLogger(PropertiesHelper.class);
    private static String kafkaServers = null;

    public static Properties getProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream input = SimpleProducer.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                log.error("Failed to load configuration file 'config.properties'");
                throw new IOException("Configuration file 'config.properties' not found");
            }
            props.load(input);
            setBootstrapServers(props);
        } catch (IOException e) {
            log.error("Error reading configuration file: ", e);
            throw e;
        }
        return props;
    }

    private static void setBootstrapServers(Properties props) {
        if (kafkaServers != null && !kafkaServers.isEmpty()) {
            props.setProperty("bootstrap.servers", kafkaServers);
            log.info("Using actively bootstrap servers: {}", kafkaServers);
        } else {
            String kafkaServersEnv = System.getenv("KAFKA_SERVERS");
            if (kafkaServersEnv != null && !kafkaServersEnv.isEmpty()) {
                kafkaServers = kafkaServersEnv;
                props.setProperty("bootstrap.servers", kafkaServers);
                log.info("Using environment variable KAFKA_SERVERS: {}", kafkaServers);
            } else {
                log.info("Environment variable KAFKA_SERVERS not found, defaulting to config file");
            }
        }
    }

    public static void setKafkaServers(String servers) {
        kafkaServers = servers;
    }
}
