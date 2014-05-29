package rooty.events.email;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NewEmailDomainEvent extends EmailDomainEvent {

    public NewEmailDomainEvent (String name) { super(name); }

}
