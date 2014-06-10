package rooty.events.email;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rooty.RootyMessage;

@NoArgsConstructor @AllArgsConstructor
public class EmailAliasEvent extends RootyMessage {

    @Getter @Setter
    private String name;
    public EmailAliasEvent withName(String n) { name = n; return this; }

}
