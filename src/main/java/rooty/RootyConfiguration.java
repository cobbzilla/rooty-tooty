package rooty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.mq.MqClient;
import org.cobbzilla.util.mq.MqClientFactory;
import org.cobbzilla.util.mq.kestrel.KestrelClient;
import org.cobbzilla.util.reflect.ReflectionUtil;

import java.util.*;

/**
 * The rooty configuration file is stored somewhere as a YAML document.
 * The YAML gets mapped into this object.
 */
@Slf4j
public class RootyConfiguration {

    @Getter @Setter private String secret;
    public RootyConfiguration withSecret(String s) { secret = s; return this; }

    @Getter @Setter private String queueName;
    public RootyConfiguration withQueueName(String q) { queueName = q; return this; }

    @Getter @Setter private Map<String, RootyHandlerConfiguration> handlers;

    public RootyConfiguration addHandler (RootyHandlerConfiguration handlerConfig) {
        if (handlers == null) handlers = new HashMap<>();
        handlers.put(handlerConfig.getHandler(), handlerConfig);
        return this;
    }

    @Getter(lazy=true) private final MqClient  mqClient = initMqClient();

    private MqClient initMqClient() {
        // Setup configuration
        final Properties kestrelProperties = new Properties();

        // Max # of connections to kestrel. default is 1
        kestrelProperties.setProperty("kestrelConnectionPoolSize", "2");

        // Comma-separated list of hostname:port of all kestrel servers to use. required.
        kestrelProperties.setProperty("kestrelHosts", "127.0.0.1:22133");

        // How often should the client drop its connection to kestrel and reconnect. default is 5 minutes
        // If you are using multiple kestrel servers, this will ensure that no kestrel server sits idle with no clients
        kestrelProperties.setProperty("kestrelReconnectIntervalInMinutes", "5");

        // Create a client
        final MqClientFactory clientFactory = new MqClientFactory();
        return clientFactory.createClient(KestrelClient.class, kestrelProperties);
    }

    @Getter(lazy=true) private final RootySender sender = initSender();

    private RootySender initSender() {
        return (RootySender) new RootySender()
                .withSecret(secret)
                .withMqClient(getMqClient())
                .withQueueName(getQueueName());
    }

    public RootyHandler getHandler(Class clazz) {
        for (RootyHandler handler : getHandlerMap().values()) {
            if (handler.getClass().isAssignableFrom(clazz)) return handler;
        }
        return null;
    }

    public List<RootyHandler> getHandlers(RootyMessage message) {
        List<RootyHandler> accepted = new ArrayList<>();
        for (RootyHandler handler : getHandlerMap().values()) {
            if (handler.accepts(message)) accepted.add(handler);
        }
        return accepted;
    }

    @Getter(value= AccessLevel.PRIVATE, lazy=true) private final Map<String, RootyHandler> handlerMap = initHandlerMap();
    private Map<String, RootyHandler> initHandlerMap() {
        final Map<String, RootyHandler> map = new HashMap<>();
        for (Map.Entry<String, RootyHandlerConfiguration> entry : getHandlers().entrySet()) {
            final RootyHandlerConfiguration handlerConfig = entry.getValue();
            try {
                final String name = entry.getKey();
                final RootyHandler handler = (RootyHandler) Class.forName(name).newInstance();
                handler.setMqClient(getMqClient());
                handler.setQueueName(getQueueName());
                if (handlerConfig != null && handlerConfig.getParams() != null) {
                    ReflectionUtil.copyFromMap(handler, handlerConfig.getParams());
                }
                map.put(name, handler);

            } catch (Exception e) {
                log.error("Error creating handler ("+handlerConfig+"): "+e, e);
            }
        }
        return map;
    }
}