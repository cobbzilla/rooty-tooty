package rooty;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class RootySender extends RootyHandlerBase {

    @Override public boolean accepts(RootyMessage message) { return false; }

    @Override public boolean process(RootyMessage message) {
        return die("RootySender is only for sending messages, not receiving");
    }

    @Setter private String secret;

    public void write(RootyMessage message) { super.write(message, secret); }

}
