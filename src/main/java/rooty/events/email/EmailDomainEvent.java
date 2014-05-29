package rooty.events.email;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rooty.RootyMessage;

@NoArgsConstructor @AllArgsConstructor
public class EmailDomainEvent extends RootyMessage {

    @Getter @Setter private String name;
    public EmailDomainEvent withName(String n) { name = n; return this; }

}
