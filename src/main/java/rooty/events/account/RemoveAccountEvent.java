package rooty.events.account;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class RemoveAccountEvent extends AccountEvent {

    public RemoveAccountEvent (String name, boolean admin) { super(name, admin); }

}
