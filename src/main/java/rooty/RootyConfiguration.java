package rooty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.mq.MqClient;
import org.cobbzilla.util.mq.MqClientFactory;
import org.cobbzilla.util.mq.kestrel.KestrelClient;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.cache.memcached.MemcachedService;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.path;

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
    @Getter @Setter private int maxRetries = 0;
    @Getter @Setter private String configDir;  // more RootyHandlers can be defined in this directory

    @Getter @Setter private Map<String, RootyHandlerConfiguration> handlers;

    @Getter(lazy=true) private final MqClient mqClient = initMqClient();

    // force initialization of handlers, will start handlerWatcher
    public void initHandlers () { getHandlerMap(); }

    private RootyHandlerWatcher handlerWatcher = null;

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

    @Getter(value=AccessLevel.PROTECTED, lazy=true) private final MemcachedService memcached = initMemcached();
    private MemcachedService initMemcached() {
        return new MemcachedService(getMemcachedHost(), getMemcachedPort(), getSecret());
    }

    @Setter private RootyStatusManager statusManager;
    public RootyStatusManager getStatusManager() {
        if (statusManager == null) statusManager = new RootyStatusManager(getMemcached());
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
        final List<RootyHandler> accepted = new ArrayList<>();
        for (RootyHandler handler : getHandlerMap().values()) {
            if (handler.accepts(message)) accepted.add(handler);
        }
        return accepted;
    }

    @Getter(value=AccessLevel.PROTECTED, lazy=true) private final Map<String, RootyHandler> handlerMap = initHandlerMap();
    private Map<String, RootyHandler> initHandlerMap() {
        final Map<String, RootyHandler> map = new ConcurrentHashMap<>();

        // Load pre-configured handlers
        for (Map.Entry<String, RootyHandlerConfiguration> entry : getHandlers().entrySet()) {
            final RootyHandlerConfiguration handlerConfig = entry.getValue();
            try {
                final String name = entry.getKey();
                addHandler(map, name, handlerConfig);

            } catch (Exception e) {
                log.error("initHandlerMap: Error creating handler ("+handlerConfig+"): "+e, e);
            }
        }

        // If configDir is defined, load handlers from there
        // Look files whose names are Java classes that implement RootyHandler
        if (configDir != null) {
            final File config = new File(configDir);
            if (!config.exists() || !config.isDirectory()) {
                log.warn("initHandlerMap: Not a directory: " + abs(config));
            } else {
                final File[] files = FileUtil.list(config);
                for (File f : files) {
                    addHandlerFromFile(map, f);
                }
            }

            handlerWatcher = new RootyHandlerWatcher(path(config), this);
            handlerWatcher.start();
        }
        return map;
    }

    protected void addHandlerFromFile(File f) { addHandlerFromFile(getHandlerMap(), f); }

    private void addHandlerFromFile(Map<String, RootyHandler> map, File f) {
        final String fname = f.getName();
        if (fname.endsWith("~") || fname.startsWith("#") || fname.startsWith(".")) {
            log.info("addHandlerFromFile: skipping file: "+fname);
            return;
        }

        final RootyHandlerConfiguration handlerConfig;
        try {
            handlerConfig = new Yaml().loadAs(FileUtil.toString(f), RootyHandlerConfiguration.class);
            addHandler(map, fname, handlerConfig.getHandler(), handlerConfig);
            log.info("addHandlerFromFile: successfully added handler "+fname+" of type: "+handlerConfig.getHandler());

        } catch (Exception e) {
            log.warn("addHandlerFromFile: Invalid handler config: " + fname + ": " + e);
        }
    }

    private void addHandler(Map<String, RootyHandler> map, String handlerClass, RootyHandlerConfiguration config) throws Exception {
        addHandler(map, handlerClass, handlerClass, config);
    }

    private void addHandler(Map<String, RootyHandler> map, String handlerName, String handlerClass, RootyHandlerConfiguration config) throws Exception {
        final RootyHandler handler = ReflectionUtil.instantiate(handlerClass);
        if (config != null && config.getParams() != null) {
            ReflectionUtil.copyFromMap(handler, config.getParams());
        }
        addHandler(map, handlerName, handler);
    }

    private void addHandler(Map<String, RootyHandler> map, String handlerName, RootyHandler handler) {
        handler.setMqClient(getMqClient());
        handler.setQueueName(getQueueName());
        handler.setSender(getSender());
        handler.setStatusManager(getStatusManager());
        map.put(handlerName, handler);
    }

    public void addHandler (String handlerClass, RootyHandlerConfiguration config) throws Exception {
        addHandler(getHandlerMap(), handlerClass, config);
    }

    public void addHandler (RootyHandler handler) throws Exception {
        addHandler(getHandlerMap(), handler.getClass().getName(), handler);
    }

    protected void removeHandlerFromFile(File f) {
        log.info("removeHandlerFromFile: removing handler "+f.getName());
        getHandlerMap().remove(f.getName());
    }

}