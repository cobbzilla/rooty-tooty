package rooty.events.email;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class RemoveEmailAliasEvent extends EmailAliasEvent {

    public RemoveEmailAliasEvent (String name) { super(name); }

}
