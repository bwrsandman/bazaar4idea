package bazaar4idea.command;

public class BzrCommandException extends Exception {

  public BzrCommandException() {
  }

  public BzrCommandException(String message) {
    super(message);
  }

  public BzrCommandException(String message, Throwable cause) {
    super(message, cause);
  }

  public BzrCommandException(Throwable cause) {
    super(cause);
  }
}
