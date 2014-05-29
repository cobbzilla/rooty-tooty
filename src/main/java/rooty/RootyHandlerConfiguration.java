package rooty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
public class RootyHandlerConfiguration {

    @Getter @Setter private String handler;
    public RootyHandlerConfiguration withHandler (String h) { handler = h; return this; }

    @Getter @Setter private Map<String, Object> params = new HashMap<>();
    public RootyHandlerConfiguration withParams (Map<String, Object> p) { params = p; return this; }
    public RootyHandlerConfiguration withParam (String name, Object value) { params.put(name, value); return this; }

}
