package hjeisa.waifubot.exception;

// used if API result contains a tag not allowed by Discord's Terms of Service for unsafe results
public class ForbiddenTagException extends Throwable {
    public ForbiddenTagException() {
        super();
    }
}
