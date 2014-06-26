package rooty;

import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.mq.MqClient;
import org.cobbzilla.util.mq.MqConsumer;
import org.cobbzilla.util.security.CryptoUtil;
import org.cobbzilla.util.security.ShaUtil;
import org.cobbzilla.util.string.Base64;
import org.kohsuke.args4j.CmdLineParser;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.string.StringUtil.UTF8cs;

@Slf4j
public class RootyMain implements MqConsumer {

    @Getter private RootyOptions options = new RootyOptions();

    @Getter @Setter private RootyConfiguration configuration;
    protected MqClient getMqClient() { return configuration.getMqClient(); }

    private final AtomicBoolean listening = new AtomicBoolean(false);
    public boolean isListening() { return listening.get(); }

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    public void shutdown () throws IOException {
        if (getMqClient() != null) {
            try {
                getMqClient().shutdown();
            } catch (Exception e) {
                log.error("Error shutting down mqClient: "+e, e);
                throw e;
            }
        }
        shutdown.set(true);
        listening.set(false);
        synchronized (this) { this.notify(); }
    }

    // Runs in the foreground.
    public static void main (String[] args) throws Exception {
        final RootyMain main = new RootyMain();
        main.run(args);
        main.waitForShutdown();
    }

    public boolean waitForStartup (long timeout) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (!isListening() && start + timeout < System.currentTimeMillis()) {
            synchronized (this) { wait(100); }
        }
        return isListening();
    }

    private void waitForShutdown() throws InterruptedException {
        while (isListening()) {
            synchronized (this) { wait(60000); }
        }
    }

    // Runs in the background
    public void run (String[] args) throws Exception {
        final CmdLineParser parser = new CmdLineParser(getOptions());
        parser.parseArgument(args);

        // load global configuration
        final Yaml yaml = new Yaml();
        try {
            @Cleanup final InputStream configStream = options.getConfigurationStream();
            configuration = yaml.loadAs(configStream, RootyConfiguration.class);
        } catch (Exception e) {
            throw new IllegalStateException("Error loading configuration: " + e, e);
        }

        run();
    }

    // Call from code, runs in background
    public void run (RootyConfiguration configuration) {
        this.configuration = configuration;
        run();
    }

    public void run () {
        final String queueName = configuration.getQueueName();
        getMqClient().registerConsumer(this, queueName, queueName + "_error");
        listening.set(true);
    }

    @Override
    public void onMessage(Object o) throws Exception {

        if (shutdown.get()) {
            final String msg = "onMessage: shutting down, bailing on message: " + o;
            log.warn(msg);
            throw new IllegalStateException(msg);
        }

        // decrypt/unmarshall the message
        final byte[] messageBytes = o.toString().getBytes(UTF8cs);
        final String secret = configuration.getSecret();
        final String json = new String(CryptoUtil.decrypt(Base64.decode(messageBytes), secret), UTF8cs);
        final RootyMessage message = fromJson(json, RootyMessage.class);

        try {
            // verify hash
            if (!ShaUtil.sha256_hex(message.getSalt()+ secret).equals(message.getHash())) {
                throw new IllegalArgumentException("Invalid hash for message: " + message.getUuid());
            }

            // which handlers will take this message?
            final List<RootyHandler> handlers = configuration.getHandlers(message);
            if (handlers.isEmpty()) {
                log.warn("No handler found for message "+message.getUuid()+" with type=" + message.getClass().getName());
                return;
            }

            // Process the message
            for (RootyHandler handler : handlers) {
                handler.process(message);
            }
            message.setSuccess(true);

        } catch (Exception e) {
            message.setError(e.getMessage());
            throw e;

        } finally {
            // write finalized result to memcached
            configuration.getStatusManager().update(message.setFinished(true));
        }
    }
}