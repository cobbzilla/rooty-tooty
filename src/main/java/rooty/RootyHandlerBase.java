package rooty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.mq.MqClient;
import org.cobbzilla.util.mq.MqProducer;
import org.cobbzilla.util.security.ShaUtil;

@Slf4j
public abstract class RootyHandlerBase implements RootyHandler {

    @Getter @Setter protected MqClient mqClient;
    public RootyHandlerBase withMqClient(MqClient m) { mqClient = m; return this; }

    @Getter @Setter protected String queueName;
    public RootyHandlerBase withQueueName(String n) { queueName = n; return this; }

    @Getter(value=AccessLevel.PROTECTED, lazy=true) private final MqProducer mqProducer = initMqProducer();
    private MqProducer initMqProducer() { return mqClient.getProducer(queueName); }

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
            getMqProducer().send(json);
        } catch (Exception e) {
            throw new IllegalStateException("Error writing to message queue: "+e, e);
        }
    }

}
