package rooty;

public class RootySender extends RootyHandlerBase {

    @Override public boolean accepts(RootyMessage message) { return false; }

    @Override public void process(RootyMessage message) {
        throw new IllegalStateException("RootySender is only for sending messages, not receiving");
    }

    private String secret;
    public RootySender withSecret(String secret) { this.secret = secret; return this; }

    public void write(RootyMessage message) { super.write(message, secret); }

}
