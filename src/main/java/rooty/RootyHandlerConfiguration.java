package rooty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Accessors(chain=true) @ToString
public class RootyHandlerConfiguration {

    @Getter @Setter private String handler;

    @Getter @Setter private Map<String, Object> params = new HashMap<>();
    public RootyHandlerConfiguration addParam (String name, Object value) { params.put(name, value); return this; }
}
