package rooty;

import lombok.Cleanup;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.cobbzilla.util.json.JsonUtil;
import org.kohsuke.args4j.CmdLineParser;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class RootySenderMain {

    @Getter private RootyOptions options = new RootyOptions();

    public static void main (String[] args) throws Exception {
        RootySenderMain main = new RootySenderMain();
        main.send(args, System.in);
    }

    public void send(String[] args, InputStream in) throws Exception {

        final CmdLineParser parser = new CmdLineParser(getOptions());
        parser.parseArgument(args);

        // load global configuration
        final Yaml yaml = new Yaml();
        final RootyConfiguration configuration;
        try {
            @Cleanup final InputStream configStream = options.getConfigurationStream();
            configuration = yaml.loadAs(configStream, RootyConfiguration.class);
        } catch (Exception e) {
            throw new IllegalStateException("Error loading configuration: " + e, e);
        }

        final RootyMessage message = JsonUtil.fromJson(IOUtils.toString(in), RootyMessage.class);
        new RootySender()
                .setSecret(configuration.getSecret())
                .setMqClient(configuration.getMqClient())
                .setQueueName(configuration.getQueueName())
                .setStatusManager(configuration.getStatusManager())
                .write(message, configuration.getSecret());
    }
}
