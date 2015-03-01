package rooty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.UUID;

import static org.cobbzilla.util.string.StringUtil.empty;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
@Accessors(chain=true) @ToString(of={"uuid", "errorCount", "lastError", "success", "finished", "results"})
public abstract class RootyMessage {

    private static final int MIN_SALT_LENGTH = 20;

    @Getter @Setter private String uuid;
    public boolean hasUuid() { return !empty(uuid); }
    public String initUuid () { this.uuid = UUID.randomUUID().toString(); return this.uuid; }

    @Getter @Setter private long ctime = System.currentTimeMillis();
    @Getter @Setter private String salt;
    @Getter @Setter private String hash;
    @Getter @Setter private int errorCount = 0;
    @Getter @Setter private String lastError;
    @Getter @Setter private boolean success = false;
    @Getter @Setter private boolean finished = false;
    @Getter private String results = null;
    public RootyMessage setResults (String r) { results = (r == null) ? null : r.trim(); return this; }

    public void setError (String message) {
        lastError = message;
        errorCount++;
    }

    @JsonIgnore public boolean getBooleanResult () { return Boolean.parseBoolean(results); }

    @JsonIgnore public boolean isValid () {
        return !empty(uuid) && (salt != null && salt.length() > MIN_SALT_LENGTH) && !empty(hash);
    }

    @JsonIgnore public long getAge () { return System.currentTimeMillis() - ctime; }
    public boolean isOlderThan (long age) { return System.currentTimeMillis() - ctime > age; }

    @JsonIgnore public boolean hasError() { return !empty(lastError); }
}
