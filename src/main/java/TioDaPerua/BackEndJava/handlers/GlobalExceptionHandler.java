package TioDaPerua.BackEndJava.handlers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import TioDaPerua.BackEndJava.DTOs.ErrorResponseDTO;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;

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

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflict(ConflictException ex, HttpServletRequest request) {
        return createResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return createResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneral(Exception ex, HttpServletRequest request) {
        return createResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Ocorreu um erro inesperado", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationError(MethodArgumentNotValidException ex) {
        // 1. Criamos um mapa vazio que vai guardar: "nomeDoCampo" -> "mensagemDeErro"
        Map<String, String> erros = new HashMap<>();

        // 2. Pedimos ao Spring a lista de todos os erros de campo que aconteceram
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();

        // 3. Iteramos por cada erro encontrado e adicionamos no mapa
        for (FieldError error : fieldErrors) {
            erros.put(error.getField(), error.getDefaultMessage());
        }

        // 4. Retornamos o mapa completo com status 400
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erros);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        return createResponse(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            "O corpo da requisição contém um formato JSON inválido ou tipos incompátiveis.",
            request
        );
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
