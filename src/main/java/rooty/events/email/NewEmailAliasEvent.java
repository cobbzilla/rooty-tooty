package rooty.events.email;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class NewEmailAliasEvent extends EmailAliasEvent {

    @Getter @Setter
    public List<String> recipients = new ArrayList<>();
    public NewEmailAliasEvent withRecipients (List<String> r) { recipients = r; return this; }
    public NewEmailAliasEvent withRecipient (String r) { recipients.add(r); return this; }

}
