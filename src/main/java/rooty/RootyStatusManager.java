package rooty;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.MemcachedClient;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.security.CryptoUtil;

import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.string.StringUtil.UTF8cs;
import static org.cobbzilla.util.string.StringUtil.empty;

@AllArgsConstructor @Slf4j
public class RootyStatusManager {

    protected static final String AUTHORITATIVE = "--authoritative--";

    private MemcachedClient memcached;
    private String secret;

    public static String statusKey(String queueName, String uuid) { return uuid +"_"+queueName; }

    public void update(String queueName, RootyMessage message, boolean authoritative) {
        try {
            final byte[] encrypted = CryptoUtil.encrypt(toJson(message).getBytes(UTF8cs), secret);
            memcached.set(statusKey(queueName, message.getUuid()), (int) TimeUnit.MINUTES.toSeconds(5), encrypted);
            if (authoritative) {
                memcached.set(statusKey(AUTHORITATIVE, message.getUuid()), (int) TimeUnit.MINUTES.toSeconds(5), encrypted);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error updating status: "+e);
        }
    }

    public RootyMessage getStatus (String uuid) { return getStatus(AUTHORITATIVE, uuid); }

    public RootyMessage getStatus (String queueName, String uuid) {
        try {
            byte[] encrypted = memcached.get(statusKey(queueName, uuid));
            if (encrypted == null && !empty(queueName)) encrypted = memcached.get(statusKey(AUTHORITATIVE, uuid));
            return encrypted == null ? null : JsonUtil.fromJson(new String(CryptoUtil.decrypt(encrypted, secret), UTF8cs), RootyMessage.class);

        } catch (Exception e) {
            throw new IllegalStateException("getStatus error: "+e, e);
        }
    }

}
