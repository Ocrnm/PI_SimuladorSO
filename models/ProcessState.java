package models;

/**
 * Enumeración que representa los posibles estados de un proceso
 */
public enum ProcessState {
    NEW,        // Proceso recién creado
    READY,      // Proceso listo para ejecutar
    RUNNING,    // Proceso en ejecución
    BLOCKED,    // Proceso bloqueado esperando un recurso o evento
    SUSPENDED,  // Proceso suspendido temporalmente
    TERMINATED  // Proceso finalizado
}