package org.example.quoteotd.exception;

import java.util.UUID;

public class ViewerNotFoundException extends RuntimeException {

	public ViewerNotFoundException(UUID viewerId) {
		super("Viewer %s does not exist".formatted(viewerId));
	}
}
