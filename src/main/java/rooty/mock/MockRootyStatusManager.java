package rooty.mock;

import rooty.RootyMessage;
import rooty.RootyStatusManager;

import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.string.StringUtil.empty;

public class MockRootyStatusManager extends RootyStatusManager {

    public MockRootyStatusManager() { super(null); }

    private Map<String, RootyMessage> map = new HashMap<>();

    @Override public void update(String queueName, RootyMessage message, boolean authoritative) {
        if (!empty(queueName)) map.put(statusKey(queueName, message.getUuid()), message);
        map.put(statusKey(AUTHORITATIVE, message.getUuid()), message);
    }

    @Override public RootyMessage getStatus(String queueName, String uuid) {
        RootyMessage message = null;
        if (!empty(queueName)) message = map.get(statusKey(queueName, uuid));
        return (message != null) ? message : map.get(statusKey(AUTHORITATIVE, uuid));
    }

}
