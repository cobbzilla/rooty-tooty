package rooty.events.email;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Accessors(chain=true)
public class NewEmailAliasEvent extends EmailAliasEvent {

    @Getter @Setter public List<String> recipients = new ArrayList<>();
    public NewEmailAliasEvent addRecipient (String r) { recipients.add(r); return this; }

}
