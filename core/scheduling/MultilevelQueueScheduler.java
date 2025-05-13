package core.scheduling;

import java.util.*;
import models.PCB;
import models.ProcessState;
import models.SchedulingData;

public class MultilevelQueueScheduler implements Scheduler {
    // Colas para los tres niveles fijos de prioridad
    private List<PCB> altaPrioridad = new ArrayList<>();    // Nivel 2
    private List<PCB> mediaPrioridad = new ArrayList<>();   // Nivel 1
    private List<PCB> bajaPrioridad = new ArrayList<>();    // Nivel 0

    // Mapa para almacenar los datos de programación de cada proceso
    private Map<Integer, SchedulingData> schedulingDataMap = new HashMap<>();

    // Tiempo actual del sistema
    private long tiempoActual = 0;
    
    // Quantum para cada nivel de prioridad
    private final int quantumAlta = 2;
    private final int quantumMedia = 4;
    private final int quantumBaja = 6;

    @Override
    public void addProcess(PCB process) {
        // Obtener o crear datos de planificación
        SchedulingData data = schedulingDataMap.get(process.pid);
        if (data == null) {
            data = new SchedulingData();
            data.arrivalTime = tiempoActual;

            // Determinar el nivel de cola basado en la prioridad del proceso
            // Simplificamos a solo 3 niveles: 0=baja, 1=media, 2=alta
            if (process.priority >= 7) {
                data.queueLevel = 2; // Alta prioridad
                data.quantum = quantumAlta;
            } else if (process.priority >= 4) {
                data.queueLevel = 1; // Media prioridad
                data.quantum = quantumMedia;
            } else {
                data.queueLevel = 0; // Baja prioridad
                data.quantum = quantumBaja;
            }
            
            // Importante: asignar burst time y remaining time para garantizar finalización
            if (data.burstTime == null) {
                // Si no tiene burst time, asignar uno basado en su prioridad
                data.burstTime = 5 + (2 - (data.queueLevel != null ? data.queueLevel : 0)) * 3;
            }
            
            data.remainingTime = data.burstTime;
            schedulingDataMap.put(process.pid, data);
        }

        // Agregar el proceso a la cola correspondiente
        switch (data.queueLevel) {
            case 2:
                altaPrioridad.add(process);
                break;
            case 1:
                mediaPrioridad.add(process);
                break;
            case 0:
            default:
                bajaPrioridad.add(process);
        }

        // Actualizar el estado del proceso
        process.state = ProcessState.READY;
    }

    @Override
    public PCB getNextProcess() {
        // Seleccionar el siguiente proceso según la política de prioridad multinivel
        PCB nextProcess = null;

        if (!altaPrioridad.isEmpty()) {
            nextProcess = altaPrioridad.remove(0);
        } else if (!mediaPrioridad.isEmpty()) {
            nextProcess = mediaPrioridad.remove(0);
        } else if (!bajaPrioridad.isEmpty()) {
            nextProcess = bajaPrioridad.remove(0);
        }

        if (nextProcess != null) {
            nextProcess.state = ProcessState.RUNNING;
            
            // Incrementar el tiempo actual del sistema
            tiempoActual++;
        }

        return nextProcess;
    }

    @Override
    public void removeProcess(PCB process) {
        // Remover el proceso de todas las colas
        altaPrioridad.remove(process);
        mediaPrioridad.remove(process);
        bajaPrioridad.remove(process);

        // Eliminar también sus datos de planificación
        schedulingDataMap.remove(process.pid);
    }

    @Override
    public void onProcessFinished(PCB process) {
        // Marcar el proceso como terminado
        process.state = ProcessState.TERMINATED;
    }

    @Override
    public String getName() {
        return "Multilevel Queue Scheduler (3 niveles: 0=Baja, 1=Media, 2=Alta)";
    }
    
    // Este método será llamado por el SimulationEngine en cada tick para actualizar tiempos
    public void updateProcessTimes(PCB runningProcess) {
        if (runningProcess != null && runningProcess.schedulingData != null) {
            SchedulingData data = runningProcess.schedulingData;
            
            // Decrementar tiempo restante
            if (data.remainingTime != null && data.remainingTime > 0) {
                data.remainingTime--;
            }
            
            // Decrementar quantum si está definido
            if (data.quantum != null && data.quantum > 0) {
                data.quantum--;
            }
        }
    }
    
    // Verificar si el proceso debe continuar ejecutándose o regresar a la cola
    public boolean shouldPreempt(PCB process) {
        if (process == null || process.schedulingData == null) {
            return false;
        }
        
        SchedulingData data = process.schedulingData;
        
        // Preemptar si se acabó el quantum pero aún tiene tiempo de CPU
        if (data.quantum != null && data.quantum <= 0 && data.remainingTime != null && data.remainingTime > 0) {
            // Restaurar quantum según nivel
            switch (data.queueLevel) {
                case 2:
                    data.quantum = quantumAlta;
                    break;
                case 1:
                    data.quantum = quantumMedia;
                    break;
                case 0:
                default:
                    data.quantum = quantumBaja;
            }
            return true;
        }
        
        return false;
    }
    
    // Verificar si el proceso ha terminado su tiempo de ejecución
    public boolean isProcessComplete(PCB process) {
        if (process == null || process.schedulingData == null) {
            return false;
        }
        
        return process.schedulingData.remainingTime != null && process.schedulingData.remainingTime <= 0;
    }
}