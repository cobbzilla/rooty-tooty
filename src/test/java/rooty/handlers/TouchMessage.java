package rooty.handlers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rooty.RootyMessage;

@NoArgsConstructor @AllArgsConstructor
public class TouchMessage extends RootyMessage {

    @Getter @Setter private String file;

}
