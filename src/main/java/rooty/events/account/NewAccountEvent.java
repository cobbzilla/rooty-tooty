package rooty.events.account;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NewAccountEvent extends AccountEvent {

    public NewAccountEvent (String name, boolean admin) { super(name, admin); }

}
