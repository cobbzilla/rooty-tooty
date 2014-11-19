package rooty.mock;

import rooty.RootyMessage;
import rooty.RootyStatusManager;

import java.util.HashMap;
import java.util.Map;

public class MockRootyStatusManager extends RootyStatusManager {

    public MockRootyStatusManager() { super(null, null); }

    private Map<String, RootyMessage> map = new HashMap<>();

    @Override public void update(String queueName, RootyMessage message) { map.put(statusKey(queueName, message.getUuid()), message); }

    @Override public RootyMessage getStatus(String queueName, String uuid) { return map.get(statusKey(queueName, uuid)); }

}
