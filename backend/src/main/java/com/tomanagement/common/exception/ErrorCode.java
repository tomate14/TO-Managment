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
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Probá de nuevo más tarde."),
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
    DIAGNOSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "El diagnóstico no existe."),
    SURGERY_DATA_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY,
            "Con cirugía, ingresá la fecha de cirugía, la de retiro de puntos y el tipo de cirugía."),
    STITCHES_BEFORE_SURGERY(HttpStatus.UNPROCESSABLE_ENTITY, "El retiro de puntos no puede ser anterior a la cirugía."),
    IMMOBILIZATION_DATE_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "Sin cirugía, ingresá la fecha de inmovilización."),
    SURGERY_TYPE_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "Ese tipo de cirugía está inactivo: elegí uno activo."),
    FOLLOW_UP_OUT_OF_RANGE(HttpStatus.UNPROCESSABLE_ENTITY, "Hay valores fuera del rango permitido."),
    SCAR_TYPE_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "El tratamiento es quirúrgico: elegí el tipo de cicatriz."),
    SCAR_TYPE_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_ENTITY, "El tratamiento es conservador: no lleva tipo de cicatriz."),
    SCAR_TYPE_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "Ese tipo de cicatriz está inactivo: elegí uno activo."),
    SEGMENT_REPEATED(HttpStatus.UNPROCESSABLE_ENTITY, "Cada segmento se evalúa una sola vez por seguimiento."),
    TEMPLATE_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY,
            "La plantilla tiene que estar activa y ser del segmento evaluado."),
    ANSWER_QUESTION_INVALID(HttpStatus.UNPROCESSABLE_ENTITY,
            "Hay respuestas a preguntas que no corresponden: solo se responden las preguntas visibles de la plantilla."),
    ANSWER_VALUE_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "Hay respuestas con un valor que no es válido para su pregunta."),
    APPOINTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "El turno no existe."),
    APPOINTMENT_OUTSIDE_HOURS(HttpStatus.UNPROCESSABLE_ENTITY,
            "Los turnos son de lunes a sábado, de 08:00 a 20:00, empezando en punto o y media."),
    APPOINTMENT_IN_PAST(HttpStatus.UNPROCESSABLE_ENTITY, "No se pueden dar turnos en el pasado."),
    APPOINTMENT_DURATION_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "La duración tiene que ser de 30, 45 o 60 minutos."),
    APPOINTMENT_RANGE_TOO_LONG(HttpStatus.UNPROCESSABLE_ENTITY, "El rango de fechas no puede superar los 42 días."),
    THERAPIST_BUSY(HttpStatus.CONFLICT, "El terapeuta ya tiene un turno en ese horario."),
    PATIENT_BUSY(HttpStatus.CONFLICT, "El paciente ya tiene un turno en ese horario."),
    DIAGNOSIS_NOT_OF_PATIENT(HttpStatus.UNPROCESSABLE_ENTITY, "El diagnóstico no es de ese paciente."),
    THERAPIST_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "Ese terapeuta está inactivo: elegí uno activo."),
    ESTABLISHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "El establecimiento no existe."),
    ESTABLISHMENT_NAME_TAKEN(HttpStatus.CONFLICT, "Ya hay un establecimiento con ese nombre."),
    ESTABLISHMENT_INACTIVE(HttpStatus.FORBIDDEN, "El establecimiento está inactivo."),
    ESTABLISHMENT_PENDING_APPROVAL(HttpStatus.FORBIDDEN, "Tu cuenta está pendiente de aprobación."),
    ESTABLISHMENT_REQUIRED(HttpStatus.FORBIDDEN, "Elegí un establecimiento para continuar."),
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
