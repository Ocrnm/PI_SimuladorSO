package core.scheduling;

import java.util.*;
import models.PCB;
import models.ProcessState;
import models.SchedulingData;

public class RoundRobinScheduler implements Scheduler {
    // Cola de procesos listos
    private Queue<PCB> readyQueue = new LinkedList<>();

    // Mapeo para mantener los datos de planificación de cada proceso
    private Map<Integer, SchedulingData> schedulingDataMap = new HashMap<>();

    // Quantum por defecto
    private int quantum = 4;

    // Tiempo actual del sistema
    private long currentTime = 0;

    // Proceso actualmente en ejecución
    private PCB runningProcess = null;

    // Lista de procesos completados (para estadísticas)
    private List<ProcesoRR> completedProcesses = new ArrayList<>();

    /**
     * Constructor con quantum personalizado
     */
    public RoundRobinScheduler(int quantum) {
        this.quantum = quantum;
    }

    /**
     * Constructor por defecto (quantum=4)
     */
    public RoundRobinScheduler() {
        this(4);
    }

    /**
     * Obtener el quantum configurado
     */
    public int getQuantum() {
        return this.quantum;
    }

    @Override
    public void addProcess(PCB process) {
        // Crear datos de planificación si no existen
        SchedulingData data = schedulingDataMap.get(process.pid);
        if (data == null) {
            data = new SchedulingData();
            data.arrivalTime = currentTime;
            data.burstTime = 5; // Valor predeterminado
            data.remainingTime = data.burstTime;
            data.quantum = this.quantum;
            schedulingDataMap.put(process.pid, data);
        }

        // Cambiar estado del proceso y agregarlo a la cola de listos
        process.state = ProcessState.READY;
        readyQueue.add(process);
    }

    @Override
    public PCB getNextProcess() {
        // Si hay un proceso en ejecución, verificar si ha consumido su quantum
        if (runningProcess != null) {
            SchedulingData data = schedulingDataMap.get(runningProcess.pid);

            // Si aún tiene tiempo de CPU restante, volver a la cola de listos
            if (data != null && data.remainingTime > 0) {
                runningProcess.state = ProcessState.READY;
                readyQueue.add(runningProcess);
            } else {
                // El proceso ha terminado
                onProcessFinished(runningProcess);
            }

            runningProcess = null;
        }

        // Obtener el siguiente proceso
        if (!readyQueue.isEmpty()) {
            runningProcess = readyQueue.poll();
            runningProcess.state = ProcessState.RUNNING;
            return runningProcess;
        }

        return null; // No hay procesos disponibles
    }

    @Override
    public void removeProcess(PCB process) {
        // Implementar la eliminación de un proceso
        if (process == runningProcess) {
            runningProcess = null;
        } else {
            // Crear una nueva cola sin el proceso eliminado
            Queue<PCB> newQueue = new LinkedList<>();
            for (PCB p : readyQueue) {
                if (p.pid != process.pid) {
                    newQueue.add(p);
                }
            }
            readyQueue = newQueue;
        }

        // Eliminar los datos de planificación asociados
        schedulingDataMap.remove(process.pid);
    }

    @Override
    public void onProcessFinished(PCB process) {
        // Marcar el proceso como terminado
        process.state = ProcessState.TERMINATED;

        // Actualizar estadísticas
        SchedulingData data = schedulingDataMap.get(process.pid);
        if (data != null) {
            ProcesoRR procesoRR = new ProcesoRR(
                    process,
                    data.arrivalTime != null ? data.arrivalTime.intValue() : 0,
                    data.burstTime != null ? data.burstTime : 0
            );
            procesoRR.tiempoFin = (int) currentTime;
            completedProcesses.add(procesoRR);
        }
    }

    @Override
    public String getName() {
        return "Round Robin (Quantum=" + quantum + ")";
    }

    /**
     * Método para la simulación manual con datos ingresados por el usuario
     */
    public void schedule(List<PCB> readyQueue) {
        Scanner scanner = new Scanner(System.in);
        List<ProcesoRR> procesos = new ArrayList<>();

        System.out.println("Ingrese los datos de planificación para Round Robin:");
        for (PCB pcb : readyQueue) {
            System.out.println("Proceso: " + pcb.pid);

            System.out.print("Tiempo de llegada: ");
            int llegada = Integer.parseInt(scanner.nextLine());

            System.out.print("Ciclos de ejecución (burst time): ");
            int burst = Integer.parseInt(scanner.nextLine());

            procesos.add(new ProcesoRR(pcb, llegada, burst));

            // Actualizar datos de planificación
            SchedulingData data = new SchedulingData();
            data.arrivalTime = (long) llegada;
            data.burstTime = burst;
            data.remainingTime = burst;
            schedulingDataMap.put(pcb.pid, data);
        }

        System.out.print("Ingrese el quantum: ");
        int quantum = Integer.parseInt(scanner.nextLine());
        this.quantum = quantum;

        // Ordenar procesos por tiempo de llegada
        procesos.sort(Comparator.comparingInt(p -> p.llegada));

        Queue<ProcesoRR> cola = new LinkedList<>();
        int tiempo = 0;
        int idx = 0;

        System.out.println("\n=== Simulación Round Robin (Quantum=" + quantum + ") ===");

        while (idx < procesos.size() || !cola.isEmpty()) {
            // Agregar procesos que hayan llegado hasta el tiempo actual
            while (idx < procesos.size() && procesos.get(idx).llegada <= tiempo) {
                cola.add(procesos.get(idx));
                System.out.println("[t=" + tiempo + "] Llega " + procesos.get(idx).pcb.pid);
                idx++;
            }

            // Si la cola está vacía, avanzar el tiempo hasta la próxima llegada
            if (cola.isEmpty()) {
                tiempo = procesos.get(idx).llegada;
                continue;
            }

            // Obtener el siguiente proceso y ejecutarlo
            ProcesoRR actual = cola.poll();
            int ejecucion = Math.min(quantum, actual.restante);
            actual.restante -= ejecucion;

            System.out.println("\n[t=" + tiempo + "] Ejecutando " + actual.pcb.pid +
                    " por " + ejecucion + "u (Restante: " + actual.restante + ")");

            tiempo += ejecucion;

            // Verificar nuevas llegadas durante la ejecución
            while (idx < procesos.size() && procesos.get(idx).llegada <= tiempo) {
                cola.add(procesos.get(idx));
                System.out.println("[t=" + tiempo + "] Llega " + procesos.get(idx).pcb.pid);
                idx++;
            }

            // Decidir si el proceso va de vuelta a la cola o ha terminado
            if (actual.restante > 0) {
                cola.add(actual);
                System.out.println("[t=" + tiempo + "] " + actual.pcb.pid + " vuelve a la cola");
            } else {
                actual.tiempoFin = tiempo;
                System.out.println("[t=" + tiempo + "] " + actual.pcb.pid + " ha terminado");
            }
        }

        mostrarResultados(procesos);
    }

    private void mostrarResultados(List<ProcesoRR> procesos) {
        System.out.println(" " + "-".repeat(74));
        System.out.println("| PID     | Tiempo Llegada | Ciclos Ejecución | Tiempo Espera | Tiempo Retorno |");
        System.out.println(" " + "-".repeat(74));

        double esperaProm = 0;
        double retornoProm = 0;

        for (ProcesoRR p : procesos) {
            int retorno = p.tiempoFin - p.llegada;
            int espera = retorno - p.burst;

            System.out.printf("| %7d | %14d | %16d | %13d | %13d |%n",
                    p.pcb.pid, p.llegada, p.burst, espera, retorno);

            esperaProm += espera;
            retornoProm += retorno;
        }

        System.out.println(" " + "-".repeat(74));
        System.out.printf("| Promedios%46s | %13.2f | %13.2f |%n", "",
                esperaProm / procesos.size(), retornoProm / procesos.size());
    }

    private static class ProcesoRR {
        PCB pcb;
        int llegada;
        int burst;
        int restante;
        int tiempoFin;

        public ProcesoRR(PCB pcb, int llegada, int burst) {
            this.pcb = pcb;
            this.llegada = llegada;
            this.burst = burst;
            this.restante = burst;
        }
    }
}