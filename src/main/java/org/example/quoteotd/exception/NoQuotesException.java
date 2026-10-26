package org.example.quoteotd.exception;

public class NoQuotesException extends RuntimeException {

	public NoQuotesException() {
		super("No quotes available to show");
	}
}
