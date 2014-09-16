package rooty.mock;

import rooty.RootyMessage;
import rooty.RootyStatusManager;

import java.util.HashMap;
import java.util.Map;

public class MockRootyStatusManager extends RootyStatusManager {

    public MockRootyStatusManager() { super(null, null); }

    private Map<String, RootyMessage> map = new HashMap<>();

    @Override public void update(RootyMessage message) { map.put(message.getUuid(), message); }

    @Override public RootyMessage getStatus(String uuid) { return map.get(uuid); }
}
