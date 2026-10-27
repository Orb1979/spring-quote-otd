package org.example.quoteotd.controller;

import org.example.quoteotd.exception.NoQuotesException;
import org.example.quoteotd.exception.ViewerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ViewerNotFoundException.class)
	ProblemDetail handleViewerNotFound(ViewerNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Viewer not found");
		return problem;
	}

	@ExceptionHandler(NoQuotesException.class)
	ProblemDetail handleNoQuotes(NoQuotesException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("No quotes available");
		return problem;
	}
}
