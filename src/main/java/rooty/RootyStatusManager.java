package rooty;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.cache.memcached.MemcachedService;

import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@AllArgsConstructor @Slf4j
public class RootyStatusManager {

    protected static final String AUTHORITATIVE = "--authoritative--";

    private MemcachedService memcached;

    public static String statusKey(String queueName, String uuid) { return uuid +"_"+queueName; }

    public void update(String queueName, RootyMessage message, boolean authoritative) {
        try {
            memcached.set(statusKey(queueName, message.getUuid()), toJson(message), (int) TimeUnit.MINUTES.toSeconds(5));
            if (authoritative) {
                memcached.set(statusKey(AUTHORITATIVE, message.getUuid()), toJson(message), (int) TimeUnit.MINUTES.toSeconds(5));
            }
        } catch (Exception e) {
            die("Error updating status: "+e);
        }
    }

    public RootyMessage getStatus (String uuid) { return getStatus(AUTHORITATIVE, uuid); }

    public RootyMessage getStatus (String queueName, String uuid) {
        try {
            String message = memcached.get(statusKey(queueName, uuid));
            if (message == null && !empty(queueName)) message = memcached.get(statusKey(AUTHORITATIVE, uuid));
            return message == null ? null : fromJson(message, RootyMessage.class);

        } catch (Exception e) {
            return die("getStatus error: " + e, e);
        }
    }

}
