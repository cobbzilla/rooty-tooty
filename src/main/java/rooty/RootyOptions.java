package rooty;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Argument;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * command-line options for the RootyMain app. The only one right now is to point to the configuration.
 */
public class RootyOptions {

    public static final String USAGE_CONFIG = "Global configuration, determines which processors handle which commands";
    @Argument(required=true, usage=USAGE_CONFIG)
    @Getter @Setter private File configuration;

    public InputStream getConfigurationStream() throws FileNotFoundException {
        return new FileInputStream(configuration);
    }
}
