package com.tomanagement.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Catalog of every error the API can return: its HTTP status and the user-facing message (Spanish UI).
 */
public enum ErrorCode {

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "El recurso solicitado no existe."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "El recurso ya existe."),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "La operación no cumple las reglas del negocio."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "La solicitud está mal formada."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El formato de la solicitud no está soportado."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "Tenés que iniciar sesión para continuar."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Tu sesión venció. Ingresá de nuevo para continuar."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "No tenés permisos para realizar esta acción."),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "La ruta solicitada no existe."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido para esta ruta."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intentá nuevamente.");

    private static final List<ErrorCode> CONTAINER_LEVEL_CODES = List.of(
            MALFORMED_REQUEST, AUTHENTICATION_REQUIRED, ACCESS_DENIED, ENDPOINT_NOT_FOUND,
            METHOD_NOT_ALLOWED, UNSUPPORTED_MEDIA_TYPE);

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    /**
     * Resolves the code for a servlet-container error status; unknown statuses map to INTERNAL_ERROR.
     */
    public static ErrorCode fromHttpStatus(int statusCode) {
        return CONTAINER_LEVEL_CODES.stream()
                .filter(code -> code.httpStatus.value() == statusCode)
                .findFirst()
                .orElse(INTERNAL_ERROR);
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
