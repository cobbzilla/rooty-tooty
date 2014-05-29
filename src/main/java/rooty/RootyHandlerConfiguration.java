package rooty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
public class RootyHandlerConfiguration {

    @Getter @Setter private String handler;
    @Getter @Setter private Map<String, Object> params = new HashMap<>();

}
