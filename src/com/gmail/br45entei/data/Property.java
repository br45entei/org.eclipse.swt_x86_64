package com.gmail.br45entei.data;

@SuppressWarnings("javadoc")
public class Property<T> implements Runnable {
	private final String name;
	private volatile T value;
	private volatile boolean isLocked = false;
	private volatile Runnable code = null;
	
	private String description = "";
	
	public Property() {
		this("");
	}
	
	public Property(String name) {
		this.name = name;
	}
	
	public Property(String name, T value) {
		this(name);
		this.value = value;
	}
	
	public Property(T value) {
		this("");
		this.value = value;
	}
	
	@Override
	public void run() {
		if(this.code != null) {
			this.code.run();
		}
	}
	
	public final Property<T> setRunnable(Runnable code) {
		this.code = code;
		return this;
	}
	
	public final String getName() {
		return this.name;
	}
	
	public final T getValue() {
		return this.value;
	}
	
	public final Property<T> setValue(T value) {
		if(!this.isLocked) {
			this.value = value;
		}
		return this;
	}
	
	public final Property<T> lockValue() {
		this.isLocked = true;
		return this;
	}
	
	public final String getDescription() {
		return this.description;
	}
	
	public final Property<T> setDescription(String description) {
		this.description = description;
		return this;
	}
	
}
