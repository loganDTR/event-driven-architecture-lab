package it.zengfx.order.adapter.in.web.error;

import it.zengfx.order.adapter.out.kafka.OrderEventPublicationException;
import it.zengfx.order.application.exception.EventContractViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidRequestDataException.class)
    public ProblemDetail handleInvalidRequestData(
            InvalidRequestDataException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );

        problem.setTitle("Invalid request data");
        problem.setType(URI.create(
                "https://eventlab.local/problems/invalid-request-data"
        ));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_REQUEST_DATA");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ){
        List<ValidationError> errors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toValidationError)
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "La richiesta contiene uno o più campi non validi."
        );

        problem.setTitle("Request validation failed");
        problem.setType(URI.create(
                "https://eventlab.local/problems/validation-error"
        ));
        problem.setProperty("code", "REQUEST_VALIDATION_FAILED");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errors", errors);

        return handleExceptionInternal(
                exception,
                problem,
                headers,
                status,
                request
        );
    }

    @ExceptionHandler(OrderEventPublicationException.class)
    public ProblemDetail handleEventPublicationFailure(
            OrderEventPublicationException exception,
            HttpServletRequest request
    ) {
        logger.error(
                "Event publication failed: method={}, path={}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Il servizio non è temporaneamente in grado di pubblicare l'evento."
        );

        problem.setTitle("Event publication unavailable");
        problem.setType(URI.create(
                "https://eventlab.local/problems/event-publication-unavailable"
        ));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "EVENT_PUBLICATION_UNAVAILABLE");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedError(
            Exception exception,
            HttpServletRequest request
    ) {
        logger.error(
                "Unexpected error: method={}, path={}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Si è verificato un errore interno inatteso."
        );

        problem.setTitle("Internal server error");
        problem.setType(URI.create(
                "https://eventlab.local/problems/internal-error"
        ));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INTERNAL_ERROR");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    private ValidationError toValidationError(FieldError error) {
        return new ValidationError(
                error.getField(),
                error.getRejectedValue(),
                error.getDefaultMessage()
        );
    }

    public record ValidationError(
            String field,
            Object rejectedValue,
            String message
    ) {
    }

    @ExceptionHandler(EventContractViolationException.class)
    public ProblemDetail handleEventContractViolation(
            EventContractViolationException exception,
            HttpServletRequest request
    ) {
        logger.error(
                "Event contract violation: method={}, path={}, violations={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.violations(),
                exception
        );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "L'evento generato non rispetta il contratto previsto."
        );

        problem.setTitle("Event contract violation");
        problem.setType(URI.create(
                "https://eventlab.local/problems/event-contract-violation"
        ));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "EVENT_CONTRACT_VIOLATION");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }
}
