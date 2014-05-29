package rooty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.string.StringUtil;

import java.util.UUID;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class RootyMessage {

    private static final int MIN_SALT_LENGTH = 20;

    @Getter @Setter private String uuid;
    public boolean hasUuid() { return !StringUtil.empty(uuid); }
    public void initUuid () { this.uuid = UUID.randomUUID().toString(); }

    @Getter @Setter private String salt;
    @Getter @Setter private String hash;
    @Getter @Setter private int errorCount = 0;
    @Getter @Setter private String lastError;

    public void setError (String message) {
        lastError = message;
        errorCount++;
    }

    @JsonIgnore public boolean isValid () {
        return !StringUtil.empty(uuid) && (salt != null && salt.length() > MIN_SALT_LENGTH) && !StringUtil.empty(hash);
    }

}
