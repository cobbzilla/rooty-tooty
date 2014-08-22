package rooty.events.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import rooty.RootyMessage;

@Accessors(chain=true)
@NoArgsConstructor @AllArgsConstructor
public class AccountEvent extends RootyMessage {

    @Getter @Setter private String name;
    @Getter @Setter private boolean admin;

    // reserved accounts are treated specially
    @JsonIgnore public boolean isReservedAccount() {
        // add more here; or put this logic elsewhere, where a list can be consulted at runtime.
        return name != null && (name.equals("root") || name.equals("postmaster"));
    }
}
