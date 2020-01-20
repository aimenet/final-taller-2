package my_exceptions;

/**
 * 
 * @author rorro
 */
public class ManualInterruptException extends Exception {

	private static final long serialVersionUID = 0000000000000000001L;
	private final Integer code;

	public ManualInterruptException(Integer code) {
		super();
		this.code = code;
	}

	public ManualInterruptException(String message, Throwable cause, Integer code) {
		super(message, cause);
		this.code = code;
	}

	public ManualInterruptException(String message, Integer code) {
		super(message);
		this.code = code;
	}

	public ManualInterruptException(Throwable cause, Integer code) {
		super(cause);
		this.code = code;
	}
	
	public Integer getCode() {
		return this.code;
	}
}