package com.gp2mv3.linkedin;

public class DialogError extends Throwable {

	private static final long serialVersionUID = -991704825747001028L;
	
	private int mErrorCode;
	
	public DialogError(String message, int errorCode) {
		super(message);
		mErrorCode = errorCode;
	}

	public int getErrorCode() {
		return mErrorCode;
	}
}
