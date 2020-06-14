package com.gmail.br45entei.swt;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public enum Response {
	YES(), NO(), YES_TO_ALL(), NO_TO_ALL(), OK(), CANCEL(), DONE(), ABORT(), RETRY(), IGNORE(), CONNECT(), RECONNECT(), DISCONNECT(), UPDATE(), CLOSE(), NO_RESPONSE(), NO_PERMS(), DIALOG_SLEEP();
	
	public final String	text;
	
	private Response() {
		this.text = this.name().substring(0, 1).toUpperCase() + this.name().substring(1).toLowerCase();
	}
	
}
