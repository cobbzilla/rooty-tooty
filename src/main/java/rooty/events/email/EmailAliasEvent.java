package rooty.events.email;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import rooty.RootyMessage;

@Accessors(chain=true)
@NoArgsConstructor @AllArgsConstructor
public class EmailAliasEvent extends RootyMessage {

    @Getter @Setter private String name;

}
