package core.scheduling;

import models.PCB;
import models.SchedulingData;
import models.ProcessState;
import models.Resource;

import java.util.*;

public class MultilevelQueueScheduler implements Scheduler {
    // Colas para los diferentes niveles de prioridad
    private List<PCB> altaPrioridad = new ArrayList<>();
    private List<PCB> mediaPrioridad = new ArrayList<>();
    private List<PCB> bajaPrioridad = new ArrayList<>();

    // Mapa para almacenar los datos de programación de cada proceso
    private Map<Integer, SchedulingData> schedulingDataMap = new HashMap<>();

    // Lista para mantener el seguimiento de los procesos completados
    private List<ProcesoWrapper> procesosCompletados = new ArrayList<>();

    // Tiempo actual del sistema
    private long tiempoActual = 0;

    @Override
    public void addProcess(PCB process) {
        // Obtener o crear datos de planificación
        SchedulingData data = schedulingDataMap.get(process.pid);
        if (data == null) {
            data = new SchedulingData();
            data.arrivalTime = tiempoActual;
            data.burstTime = 5; // Valor por defecto
            data.remainingTime = data.burstTime;

            // Determinar el nivel de cola basado en la prioridad del proceso
            if (process.priority >= 7) {
                data.queueLevel = 2; // Alta prioridad
            } else if (process.priority >= 4) {
                data.queueLevel = 1; // Media prioridad
            } else {
                data.queueLevel = 0; // Baja prioridad
            }

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
                bajaPrioridad.add(process);
                break;
            default:
                bajaPrioridad.add(process); // Por defecto va a la cola de baja prioridad
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
        }

        return nextProcess;
    }

    @Override
    public void removeProcess(PCB process) {
        // Remover el proceso de todas las colas
        altaPrioridad.remove(process);
        mediaPrioridad.remove(process);
        bajaPrioridad.remove(process);

        // Opcionalmente, podemos eliminar también sus datos de planificación
        schedulingDataMap.remove(process.pid);
    }

    @Override
    public void onProcessFinished(PCB process) {
        // Obtener los datos de planificación
        SchedulingData data = schedulingDataMap.get(process.pid);
        if (data == null) return;

        // Actualizar estadísticas del proceso finalizado
        process.state = ProcessState.TERMINATED;

        // Registrar el proceso completado para mostrar estadísticas
        ProcesoWrapper wrapper = new ProcesoWrapper(
                process,
                data.arrivalTime != null ? data.arrivalTime.intValue() : 0,
                data.burstTime != null ? data.burstTime.intValue() : 0,
                data.queueLevel != null ? data.queueLevel.intValue() : 0
        );

        // Calcular tiempos
        int tiempoInicio = (int) (tiempoActual - (data.burstTime != null ? data.burstTime.intValue() : 0));
        int tiempoFin = (int) tiempoActual;
        int tiempoEspera = tiempoInicio - (data.arrivalTime != null ? data.arrivalTime.intValue() : 0);

        wrapper.tiempoInicio = tiempoInicio;
        wrapper.tiempoFin = tiempoFin;
        wrapper.tiempoEspera = tiempoEspera;

        procesosCompletados.add(wrapper);
    }

    @Override
    public String getName() {
        return "Multilevel Queue Scheduler";
    }

    /**
     * Método para la simulación manual con datos ingresados por el usuario
     */
    public void schedule(List<PCB> readyQueue) {
        Scanner scanner = new Scanner(System.in);
        List<ProcesoWrapper> procesos = new ArrayList<>();

        System.out.println("Ingrese los datos de planificación para cada proceso:");
        for (PCB pcb : readyQueue) {
            System.out.println("Proceso: " + pcb.pid);

            System.out.print("Tiempo de llegada: ");
            int llegada = Integer.parseInt(scanner.nextLine());

            System.out.print("Ciclos de ejecución (burst time): ");
            int ciclos = Integer.parseInt(scanner.nextLine());

            System.out.print("Prioridad (0=baja, 1=media, 2=alta): ");
            int prioridad = Integer.parseInt(scanner.nextLine());

            ProcesoWrapper wrapper = new ProcesoWrapper(pcb, llegada, ciclos, prioridad);
            procesos.add(wrapper);

            // Actualizar la información de planificación en el mapa
            SchedulingData data = new SchedulingData();
            data.arrivalTime = (long) llegada;
            data.burstTime = ciclos;
            data.remainingTime = ciclos;
            data.queueLevel = prioridad;
            schedulingDataMap.put(pcb.pid, data);
        }

        List<ProcesoWrapper> alta = new ArrayList<>();
        List<ProcesoWrapper> media = new ArrayList<>();
        List<ProcesoWrapper> baja = new ArrayList<>();

        for (ProcesoWrapper p : procesos) {
            switch (p.prioridad) {
                case 2:
                    alta.add(p);
                    break;
                case 1:
                    media.add(p);
                    break;
                case 0:
                    baja.add(p);
                    break;
            }
        }

        tiempoActual = 0;
        procesosCompletados.clear();

        while (!alta.isEmpty() || !media.isEmpty() || !baja.isEmpty()) {
            ProcesoWrapper proceso;

            if (!alta.isEmpty()) {
                proceso = alta.remove(0);
            } else if (!media.isEmpty()) {
                proceso = media.remove(0);
            } else {
                proceso = baja.remove(0);
            }

            int tiempoInicio = Math.max((int) tiempoActual, proceso.llegada);
            int tiempoFin = tiempoInicio + proceso.ciclos;
            int espera = tiempoInicio - proceso.llegada;

            tiempoActual = tiempoFin;
            proceso.tiempoInicio = tiempoInicio;
            proceso.tiempoFin = tiempoFin;
            proceso.tiempoEspera = espera;
            procesosCompletados.add(proceso);
        }

        mostrarResultados(procesosCompletados);
    }

    private void mostrarResultados(List<ProcesoWrapper> procesos) {
        System.out.println(" " + "-".repeat(74));
        System.out.println("| PID     | Tiempo Llegada | Ciclos Ejecución | Tiempo Espera | Tiempo Retorno |");
        System.out.println(" " + "-".repeat(74));

        double esperaProm = 0;
        double retornoProm = 0;

        for (ProcesoWrapper p : procesos) {
            int retorno = p.tiempoFin - p.llegada;
            System.out.printf("| %7d | %14d | %16d | %13d | %13d |%n",
                    p.pcb.pid, p.llegada, p.ciclos, p.tiempoEspera, retorno);

            esperaProm += p.tiempoEspera;
            retornoProm += retorno;
        }

        System.out.println(" " + "-".repeat(74));
        System.out.printf("| Promedios%46s | %13.2f | %13.2f |%n", "",
                esperaProm / procesos.size(), retornoProm / procesos.size());
    }

    /**
     * Clase auxiliar para mantener los datos de planificación
     */
    private static class ProcesoWrapper {
        PCB pcb;
        int llegada;
        int ciclos;
        int prioridad;
        int tiempoInicio;
        int tiempoFin;
        int tiempoEspera;

        public ProcesoWrapper(PCB pcb, int llegada, int ciclos, int prioridad) {
            this.pcb = pcb;
            this.llegada = llegada;
            this.ciclos = ciclos;
            this.prioridad = prioridad;
        }
    }
}