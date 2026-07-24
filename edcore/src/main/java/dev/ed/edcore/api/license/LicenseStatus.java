package dev.ed.edcore.api.license;

/**
 * Состояние онлайн-лицензии EDCore.
 */
public enum LicenseStatus {
    /** Идентификаторы ещё не сгенерированы или сервер не зарегистрирован. */
    UNREGISTERED,
    /** Сервер в БД, ключ в config пуст или не привязан к server_id. */
    WAITING_KEY,
    /** POST /license/verify успешен. */
    VALID,
    /** Ключ указан, проверка не прошла. */
    INVALID
}
