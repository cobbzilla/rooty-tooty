package rooty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.mq.MqClient;
import org.cobbzilla.util.mq.MqProducer;
import org.cobbzilla.util.security.CryptoUtil;
import org.cobbzilla.util.security.ShaUtil;
import org.cobbzilla.util.string.Base64;
import org.cobbzilla.util.string.StringUtil;

@Accessors(chain=true) @Slf4j
public abstract class RootyHandlerBase implements RootyHandler {

    @Getter @Setter protected MqClient mqClient;
    @Getter @Setter protected String queueName;

    @Getter(value=AccessLevel.PROTECTED, lazy=true) private final MqProducer mqProducer = initMqProducer();
    private MqProducer initMqProducer() { return mqClient.getProducer(queueName); }

    @Getter @Setter private RootyStatusManager statusManager;

    public void write (RootyMessage message, String secret) {

        if (secret == null || secret.trim().length() < 10) {
            throw new IllegalArgumentException("secret must be at least 10 chars long");
        }

        if (!message.hasUuid()) message.initUuid();

        final String salt = RandomStringUtils.randomAlphanumeric(30);
        message.setSalt(salt);
        message.setHash(ShaUtil.sha256_hex(salt + secret));

        // generate JSON to a String
        final String json;
        try {
            json = JsonUtil.FULL_MAPPER.writeValueAsString(message);
        } catch (Exception e) {
            throw new IllegalStateException("Error translating message to JSON: "+e, e);
        }

        try {
            getMqProducer().send(Base64.encodeBytes(CryptoUtil.encrypt(json.getBytes(StringUtil.UTF8cs), secret)));
        } catch (Exception e) {
            throw new IllegalStateException("Error writing to message queue: "+e, e);
        }
    }

}
