package ui;

import core.*;
import core.scheduling.*;
import java.util.*;
import models.*;

public class ConsoleInterface {
    private final Scanner scanner = new Scanner(System.in);
    private final ProcessManager pm = new ProcessManager();
    private final ResourceManager rm = new ResourceManager();
    private final Scheduler scheduler;

    public ConsoleInterface(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void start() {
        while (true) {
            System.out.println("\n=== MENÚ ===");
            System.out.println("1. Crear proceso");
            System.out.println("2. Suspender proceso");
            System.out.println("3. Reanudar proceso");
            System.out.println("4. Terminar proceso");
            System.out.println("5. Listar procesos");
            System.out.println("6. Mostrar memoria disponible");
            System.out.println("7. Salir");
            System.out.print("Opción: ");

            int op = scanner.nextInt();
            scanner.nextLine(); // Limpiar buffer

            switch (op) {
                case 1 -> crearProceso();
                case 2 -> {
                    listarProcesos();
                    int pid = promptPid();
                    if (pid != -1) pm.suspendProcess(pid);
                }
                case 3 -> {
                    listarProcesos();
                    int pid = promptPid();
                    if (pid != -1) pm.resumeProcess(pid);
                }
                case 4 -> {
                    listarProcesos();
                    int pid = promptPid();
                    if (pid != -1) {
                        PCB process = pm.getProcess(pid);
                        rm.releaseResources(process); // Liberar recursos antes de terminar
                        pm.terminateProcess(pid, "Usuario");
                    }
                }
                case 5 -> listarProcesos();
                case 6 -> mostrarMemoria();
                case 7 -> System.exit(0);
                default -> System.out.println("Opción inválida");
            }
        }
    }

    private void mostrarMemoria() {
        int totalMemory = rm.getTotalMemory();
        int usedMemory = totalMemory - rm.getAvailableMemory();
        int availableMemory = rm.getAvailableMemory();
        
        System.out.println("=== INFORMACIÓN DE MEMORIA ===");
        System.out.println("Memoria total: " + totalMemory + " MB");
        System.out.println("Memoria en uso: " + usedMemory + " MB (" + 
                          (usedMemory * 100 / totalMemory) + "%)");
        System.out.println("Memoria disponible: " + availableMemory + " MB (" + 
                          (availableMemory * 100 / totalMemory) + "%)");
    }

    private void crearProceso() {
        int pri = 0;
        
        // Verificar memoria disponible antes de solicitar datos
        System.out.println("Memoria disponible: " + rm.getAvailableMemory() + " MB");
        
        // Solicitar memoria antes que otros parámetros
        System.out.print("Memoria requerida (MB): ");
        int mem = scanner.nextInt();
        scanner.nextLine(); // Limpiar buffer
        
        // Validar que haya suficiente memoria antes de continuar
        if (mem > rm.getAvailableMemory()) {
            System.out.println("Error: No hay suficiente memoria disponible.");
            System.out.println("Proceso no creado.");
            return;
        }
        
        // Solo pedimos prioridad si usamos Cola Multinivel
        if (scheduler instanceof MultilevelQueueScheduler) {
            System.out.print("Prioridad (0-9): ");
            pri = scanner.nextInt();
            scanner.nextLine(); // Limpiar buffer
        } else {
            // Para Round Robin no mostramos mensaje de prioridad
            pri = 0; // Valor por defecto
        }
        
        PCB p = pm.createProcess(pri, mem);
        p.schedulingData = new SchedulingData();

        // Configuración específica para Round Robin
        if (scheduler instanceof RoundRobinScheduler) {
            System.out.print("Burst Time: ");
            p.schedulingData.burstTime = scanner.nextInt();
            p.schedulingData.remainingTime = p.schedulingData.burstTime;
            scanner.nextLine(); // Limpiar buffer
            
            // Obtener el quantum del scheduler
            RoundRobinScheduler rrScheduler = (RoundRobinScheduler) scheduler;
            p.schedulingData.quantum = rrScheduler.getQuantum();
        }
        
        // Configuración para Cola Multinivel
        if (scheduler instanceof MultilevelQueueScheduler) {
            // Asignar nivel de cola basado en prioridad
            if (pri >= 7) {
                p.schedulingData.queueLevel = 2; // Alta prioridad
            } else if (pri >= 4) {
                p.schedulingData.queueLevel = 1; // Media prioridad
            } else {
                p.schedulingData.queueLevel = 0; // Baja prioridad
            }
        }

        // Intentar asignar recursos y luego agregar al planificador
        if (!rm.requestResources(p)) {
            System.out.println("Error: No se pudieron asignar recursos.");
            pm.terminateProcess(p.pid, "Fallo en asignación de recursos");
        } else {
            scheduler.addProcess(p);
            // Solo mostramos el mensaje de éxito si se asignaron los recursos
            Logger.log("Se creó proceso: " + p);
            System.out.println("Proceso " + p.pid + " creado exitosamente.");
        }
    }

    private int promptPid() {
        System.out.print("Ingrese PID del proceso: ");
        int pid = scanner.nextInt();
        scanner.nextLine(); // Limpiar buffer

        if (!pm.isProcessActive(pid)) {
            System.out.println("⚠  PID no encontrado. Intente con uno válido.");
            return -1;
        }
        return pid;
    }

    private void listarProcesos() {
        List<PCB> procesos = pm.getActiveProcesses();
        if (procesos.isEmpty()) {
            System.out.println("No hay procesos activos.");
        } else {
            System.out.println("=== Procesos activos ===");
            
            if (scheduler instanceof RoundRobinScheduler) {
                System.out.printf("%-5s %-10s %-10s %-10s %s%n",
                    "PID", "Estado", "Memoria", "Burst Time", "Remaining Time");
            } else {
                System.out.printf("%-5s %-10s %-10s %-10s %s%n", 
                    "PID", "Estado", "Prioridad", "Memoria", "Cola");
            }
            
            System.out.println("-".repeat(70));
            
            for (PCB p : procesos) {
                if (scheduler instanceof RoundRobinScheduler) {
                    String burstTime = p.schedulingData != null && p.schedulingData.burstTime != null ? 
                        p.schedulingData.burstTime.toString() : "N/A";
                    String remainingTime = p.schedulingData != null && p.schedulingData.remainingTime != null ? 
                        p.schedulingData.remainingTime.toString() : "N/A";
                    System.out.printf("%-5d %-10s %-10d %-10s %s%n", 
                        p.pid, p.state, p.requiredMemory, burstTime, remainingTime);
                } else {
                    // Para Cola Multinivel
                    String queueLevel = p.schedulingData != null && p.schedulingData.queueLevel != null ? 
                        (p.schedulingData.queueLevel == 2 ? "Alta" : 
                         p.schedulingData.queueLevel == 1 ? "Media" : "Baja") : "N/A";
                    System.out.printf("%-5d %-10s %-10d %-10d %s%n", 
                        p.pid, p.state, p.priority, p.requiredMemory, queueLevel);
                }
            }
        }
    }
}


