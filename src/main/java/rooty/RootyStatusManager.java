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

    public void update(RootyMessage message) {
        try {
            final byte[] encrypted = CryptoUtil.encrypt(toJson(message).getBytes(UTF8cs), secret);
            memcached.set(message.getUuid(), (int) TimeUnit.MINUTES.toSeconds(5), encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Error updating status: "+e);
        }
    }

    public RootyMessage getStatus (String uuid) {
        try {
            final byte[] encrypted = memcached.get(uuid);
            return encrypted == null ? null : JsonUtil.fromJson(new String(CryptoUtil.decrypt(encrypted, secret), UTF8cs), RootyMessage.class);

        } catch (Exception e) {
            throw new IllegalStateException("getStatus error: "+e, e);
        }
    }

}
