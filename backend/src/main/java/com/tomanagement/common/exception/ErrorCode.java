package com.tomanagement.common.exception;

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
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "No tenés permisos para realizar esta acción."),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "La ruta solicitada no existe."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido para esta ruta."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intentá nuevamente.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
