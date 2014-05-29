package rooty.events.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rooty.RootyMessage;

@NoArgsConstructor @AllArgsConstructor
public class AccountEvent extends RootyMessage {

    @Getter @Setter private String name;
    public AccountEvent withName(String n) { name = n; return this; }

    @Getter @Setter private boolean admin;
    public AccountEvent withAdmin(boolean b) { admin = b; return this; }

    // reserved accounts are treated specially
    @JsonIgnore public boolean isReservedAccount() {
        // add more here; or put this logic elsewhere, where a list can be consulted at runtime.
        return name != null && (name.equals("root") || name.equals("postmaster"));
    }
}
