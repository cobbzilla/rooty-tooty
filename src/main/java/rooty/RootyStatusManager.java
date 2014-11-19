package rooty;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.MemcachedClient;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.security.CryptoUtil;

import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.string.StringUtil.UTF8cs;

@AllArgsConstructor @Slf4j
public class RootyStatusManager {

    private MemcachedClient memcached;
    private String secret;

    public static String statusKey(String queueName, String uuid) { return uuid +"_"+queueName; }

    public void update(String queueName, RootyMessage message) {
        try {
            final byte[] encrypted = CryptoUtil.encrypt(toJson(message).getBytes(UTF8cs), secret);
            memcached.set(statusKey(queueName, message.getUuid()), (int) TimeUnit.MINUTES.toSeconds(5), encrypted);
            memcached.set(statusKey("", message.getUuid()), (int) TimeUnit.MINUTES.toSeconds(5), encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Error updating status: "+e);
        }
    }

    public RootyMessage getStatus (String queueName, String uuid) {
        try {
            byte[] encrypted = memcached.get(statusKey(queueName, uuid));
            if (encrypted == null) encrypted = memcached.get(statusKey("", uuid));
            return encrypted == null ? null : JsonUtil.fromJson(new String(CryptoUtil.decrypt(encrypted, secret), UTF8cs), RootyMessage.class);

        } catch (Exception e) {
            throw new IllegalStateException("getStatus error: "+e, e);
        }
    }

}
