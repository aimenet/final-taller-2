package commons;

public class Tarea {
	private Integer code;
	private Object payload;
	private String name;
	
	// Constructores
	public Tarea(String name) {
		this.code = null;
		this.name = name;
		this.payload = null;
	}
	
	public Tarea(String name, Object payload) {
		this.code = null;
		this.name = name;
		this.payload = payload;
	}
	
	public Tarea(Integer code, String name, Object payload) {
		this.code = code;
		this.name = name;
		this.payload = payload;
	}

	
	// Getters y Setters
	public Integer getCode() {return code;}
	public Object getPayload() {return payload;}
	public String getName() {return name;}
	
	public void setCode(Integer code) {this.code = code;}
	public void setPayload(Object payload) {this.payload = payload;}
	public void setName(String name) {this.name = name;}
}
