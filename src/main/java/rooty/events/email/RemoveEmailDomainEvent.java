package rooty.events.email;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class RemoveEmailDomainEvent extends EmailDomainEvent {

    public RemoveEmailDomainEvent (String name) { super(name); }

}
