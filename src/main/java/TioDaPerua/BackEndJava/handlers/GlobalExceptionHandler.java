package TioDaPerua.BackEndJava.handlers;

import TioDaPerua.BackEndJava.DTOs.ErrorResponseDTO;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.ServiceUnavailableException;
import TioDaPerua.BackEndJava.exceptions.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return createResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return createResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return createResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return createResponse(HttpStatus.FORBIDDEN, "Forbidden", "Acesso não autorizado para este recurso", request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflict(ConflictException ex, HttpServletRequest request) {
        return createResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponseDTO> handleServiceUnavailable(
            ServiceUnavailableException ex, HttpServletRequest request) {
        return createResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return createResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + validationDescription(error))
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.joining("; "));
        if (message.isBlank()) {
            message = "A requisição contém campos inválidos";
        }
        return createResponse(HttpStatus.BAD_REQUEST, "Bad Request", message, request);
    }

    private String validationDescription(org.springframework.validation.FieldError error) {
        return switch (error.getCode() == null ? "" : error.getCode()) {
            case "NotBlank", "NotEmpty", "NotNull" -> "é obrigatório";
            case "Email" -> "deve ter formato de e-mail válido";
            case "Positive", "PositiveOrZero", "Min", "Max" -> "está fora do limite permitido";
            case "PastOrPresent" -> "não pode ser uma data futura";
            case "Size" -> "tem tamanho fora do limite permitido";
            case "SenhaValida" -> error.getDefaultMessage();
            default -> "é inválido";
        };
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnreadableRequest(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return createResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                "O corpo da requisição está ausente ou em formato inválido", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneral(Exception ex, HttpServletRequest request) {
        return createResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Ocorreu um erro inesperado", request);
    }

    private ResponseEntity<ErrorResponseDTO> createResponse(HttpStatus status, String error, String message, HttpServletRequest request) {
        ErrorResponseDTO body = new ErrorResponseDTO(
            LocalDateTime.now(),
            status.value(),
            error,
            message,
            request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
