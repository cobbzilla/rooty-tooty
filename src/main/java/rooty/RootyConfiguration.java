package rooty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;
import org.cobbzilla.util.mq.MqClient;
import org.cobbzilla.util.mq.MqClientFactory;
import org.cobbzilla.util.mq.kestrel.KestrelClient;
import org.cobbzilla.util.reflect.ReflectionUtil;

import java.io.IOException;
import java.util.*;

/**
 * The rooty configuration file is stored somewhere as a YAML document.
 * The YAML gets mapped into this object.
 */
@Slf4j
public class RootyConfiguration {

    @Getter @Setter private String secret;
    @Getter @Setter private String queueName;
    @Getter @Setter private String memcachedHost = "127.0.0.1";
    @Getter @Setter private int memcachedPort = 11211;

    @Getter @Setter private Map<String, RootyHandlerConfiguration> handlers;

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
                .setSecret(secret)
                .setMqClient(getMqClient())
                .setQueueName(getQueueName());
    }

    @Getter(value=AccessLevel.PROTECTED, lazy=true) private final MemcachedClient memcached = initMemcached();
    private MemcachedClient initMemcached() {
        try {
            return new XMemcachedClient(getMemcachedHost(), getMemcachedPort());
        } catch (IOException e) {
            throw new IllegalStateException("Error connecting to memcached: "+e, e);
        }
    }

    @Setter private RootyStatusManager statusManager;
    public RootyStatusManager getStatusManager() {
        if (statusManager == null) statusManager = new RootyStatusManager(getMemcached(), getSecret());
        return statusManager;
    }

    public <T> T getHandler(Class<T> clazz) { return (T) getRootyHandler(clazz, getHandlerMap()); }

    public static RootyHandler getRootyHandler(Class clazz, Map<String, RootyHandler> map) {
        for (RootyHandler handler : map.values()) {
            if (clazz.isAssignableFrom(handler.getClass())) return handler;
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

    @Getter(value=AccessLevel.PROTECTED, lazy=true) private final Map<String, RootyHandler> handlerMap = initHandlerMap();
    private Map<String, RootyHandler> initHandlerMap() {
        final Map<String, RootyHandler> map = new HashMap<>();
        for (Map.Entry<String, RootyHandlerConfiguration> entry : getHandlers().entrySet()) {
            final RootyHandlerConfiguration handlerConfig = entry.getValue();
            try {
                final String name = entry.getKey();
                addHandler(map, name, handlerConfig);

            } catch (Exception e) {
                log.error("Error creating handler ("+handlerConfig+"): "+e, e);
            }
        }
        return map;
    }

    private void addHandler(Map<String, RootyHandler> map, String handlerClass, RootyHandlerConfiguration config) throws Exception {
        final RootyHandler handler = (RootyHandler) Class.forName(handlerClass).newInstance();
        if (config != null && config.getParams() != null) {
            ReflectionUtil.copyFromMap(handler, config.getParams());
        }
        addHandler(map, handlerClass, handler);
    }

    private void addHandler(Map<String, RootyHandler> map, String handlerClass, RootyHandler handler) {
        handler.setMqClient(getMqClient());
        handler.setQueueName(getQueueName());
        handler.setStatusManager(getStatusManager());
        map.put(handlerClass, handler);
    }

    public void addHandler (String handlerClass, RootyHandlerConfiguration config) throws Exception {
        addHandler(getHandlerMap(), handlerClass, config);
    }

    public void addHandler (RootyHandler handler) throws Exception {
        addHandler(getHandlerMap(), handler.getClass().getName(), handler);
    }
}