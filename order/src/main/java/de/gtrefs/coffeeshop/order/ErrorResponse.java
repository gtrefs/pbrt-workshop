package de.gtrefs.coffeeshop.order;

import java.util.*;

public class ErrorResponse {
	public ErrorResponse() {
	}

	public ErrorResponse(String message, List<String> details) {
		this.message = message;
		this.details = details;
	}

	private String message;
	private List<String> details;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<String> getDetails() {
		return details;
	}

	public void setDetails(List<String> details) {
		this.details = details;
	}

	@Override
	public String toString() {
		return "ErrorResponse{" +
				"message='" + message + '\'' +
				", details=" + details +
				'}';
	}
}
