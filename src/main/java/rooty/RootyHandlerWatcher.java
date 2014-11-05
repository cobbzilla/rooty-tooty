package rooty;

import org.cobbzilla.util.io.FilesystemWatcher;

import java.io.File;
import java.nio.file.Path;

public class RootyHandlerWatcher extends FilesystemWatcher {

    private final RootyConfiguration configuration;

    public RootyHandlerWatcher(Path path, RootyConfiguration configuration) {
        super(path);
        this.configuration = configuration;
    }

    @Override protected void onFileCreated (File path) { configuration.addHandlerFromFile(path); }
    @Override protected void onFileModified(File path) { configuration.addHandlerFromFile(path); }
    @Override protected void onFileDeleted(File path) { configuration.removeHandlerFromFile(path); }

}
