package rooty.handlers;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.cobbzilla.util.io.FileUtil;
import rooty.RootyHandlerBase;
import rooty.RootyMessage;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.abs;

@Slf4j
public class TouchFileHandler extends RootyHandlerBase {

    private static final AtomicInteger messageCount = new AtomicInteger(0);
    public static int getMessageCount () { return messageCount.get(); }

    private static final AtomicInteger successCount = new AtomicInteger(0);
    public static int getSuccessCount () { return successCount.get(); }

    public static void resetStats() {
        messageCount.set(0);
        successCount.set(0);
    }

    @Getter @Setter private String suffix;

    public String getFileSuffix() { return empty(suffix) ? "" :  "." + suffix; }

    @Override public boolean accepts(RootyMessage message) { return message instanceof TouchMessage; }

    @Override public boolean process(RootyMessage message) {

        log.info("process("+suffix+"): received message: "+message);
        messageCount.incrementAndGet();

        final TouchMessage m = (TouchMessage) message;
        final File file = new File(m.getFile() + getFileSuffix());
        FileUtil.touch(file);

        if (file.exists()) successCount.incrementAndGet();
        log.info("process("+suffix+"): successfully touched file: "+abs(file));
        return true;
    }

}
