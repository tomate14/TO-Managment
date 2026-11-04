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
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "El usuario no existe."),
    USER_EMAIL_TAKEN(HttpStatus.CONFLICT, "Ya hay un usuario con este email."),
    SELF_DEACTIVATION_FORBIDDEN(HttpStatus.UNPROCESSABLE_ENTITY, "No podés desactivarte."),
    SELF_ROLE_CHANGE_FORBIDDEN(HttpStatus.UNPROCESSABLE_ENTITY, "No podés cambiar tu propio rol."),
    CURRENT_PASSWORD_INCORRECT(HttpStatus.UNPROCESSABLE_ENTITY, "La contraseña actual no es correcta."),
    CATALOG_NOT_FOUND(HttpStatus.NOT_FOUND, "El catálogo no existe."),
    CATALOG_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "El elemento del catálogo no existe."),
    CATALOG_ITEM_DUPLICATE(HttpStatus.CONFLICT, "Ya existe con ese nombre (sin importar mayúsculas)."),
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "La plantilla no existe."),
    TEMPLATE_DUPLICATE(HttpStatus.CONFLICT, "Ya hay una plantilla con ese nombre en el segmento."),
    SEGMENT_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "El segmento está inactivo: elegí uno activo."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "La pregunta no existe."),
    QUESTION_RANGE_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "El mínimo no puede ser mayor que el máximo."),
    QUESTION_OPTIONS_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "Las preguntas de opción necesitan al menos 2 opciones."),
    QUESTION_OPTIONS_DUPLICATE(HttpStatus.UNPROCESSABLE_ENTITY, "Las opciones no se pueden repetir."),
    CONDITION_PARENT_INVALID(HttpStatus.UNPROCESSABLE_ENTITY,
            "La pregunta padre tiene que ser una pregunta activa anterior de la misma plantilla."),
    CONDITION_OPERATOR_INCOMPATIBLE(HttpStatus.UNPROCESSABLE_ENTITY,
            "«Mayor que» y «Menor que» solo sirven si la pregunta padre es numérica. Elegí «Igual a» o «Distinto de»."),
    CONDITION_VALUE_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "El valor esperado no es una respuesta válida de la pregunta padre."),
    QUESTION_LOCKED(HttpStatus.UNPROCESSABLE_ENTITY, "La pregunta tiene respuestas: solo se puede desactivar."),
    QUESTION_HAS_DEPENDENTS(HttpStatus.UNPROCESSABLE_ENTITY,
            "Otras preguntas dependen de esta: no se puede eliminar ni cambiar su tipo u opciones."),
    PATIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "El paciente no existe."),
    PATIENT_DNI_DUPLICATE(HttpStatus.CONFLICT, "Ya existe un paciente con este DNI."),
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
