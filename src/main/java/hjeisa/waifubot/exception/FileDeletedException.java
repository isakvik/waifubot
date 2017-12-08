package hjeisa.waifubot.exception;

// used for when an api call returns a post with no associated file URL, signifying post has been deleted
public class FileDeletedException extends Exception {
    public FileDeletedException() {
        super();
    }
}
