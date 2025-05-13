package ui;

import core.*;
import core.scheduling.*;
import java.util.*;
import memory.*;
import models.*;
import sync.Semaphore;

public class ConsoleInterface {
    private final Scanner scanner = new Scanner(System.in);
    private final ProcessManager pm = new ProcessManager();
    private final ResourceManager rm = new ResourceManager();
    private final Scheduler scheduler;
    private final SimulationEngine simulationEngine;

    public ConsoleInterface(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.simulationEngine = new SimulationEngine(scheduler, pm, rm);
    }

    public void start() {
        while (true) {
            // Verificar si hay procesos activos
            List<PCB> procesosActivos = pm.getActiveProcesses();
            boolean hayProcesos = !procesosActivos.isEmpty();

            System.out.println("\n" + "=".repeat(80));
            System.out.println("=== SIMULADOR DE SISTEMA OPERATIVO ===");
            System.out.println("Algoritmo: " + scheduler.getName());
            System.out.println("Tiempo actual: " + simulationEngine.getCurrentTime());
            System.out.println("Estado: " + (simulationEngine.isPaused() ? "PAUSADO" : "EN EJECUCIÓN"));
            System.out.println("=".repeat(80));
            
            // Mostrar el proceso en ejecución
            PCB enEjecucion = simulationEngine.getRunningProcess();
            if (enEjecucion != null) {
                System.out.println("\n=== PROCESO EN EJECUCIÓN ===");
                System.out.println(enEjecucion);
            }
            
            // Mostrar listas de procesos
            displayProcessLists();
            
            // Mostrar información de memoria
            displayMemory();
            
            System.out.println("\n=== MENÚ ===");
            System.out.println("1. Crear proceso");
            
            // Mostrar opciones adicionales solo si hay procesos
            if (hayProcesos) {
                System.out.println("2. Suspender proceso");
                System.out.println("3. Reanudar proceso");
                System.out.println("4. Terminar proceso");
                System.out.println("5. Listar procesos");
                System.out.println("6. Información de memoria");
                System.out.println("7. Información de semáforos");
                System.out.println("8. Avanzar simulación");
                System.out.println("9. Pausar/Reanudar simulación");
            }
            
            int baseOption = hayProcesos ? 10 : 2;
            System.out.println(baseOption + ". Salir");
            
            System.out.print("Opción: ");
            int op = scanner.nextInt();
            scanner.nextLine(); // Limpiar buffer

            if (hayProcesos) {
                switch (op) {
                    case 1 -> crearProceso();
                    case 2 -> {
                        listarProcesos();
                        int pid = promptPid();
                        if (pid != -1) {
                            PCB process = pm.getProcess(pid);
                            simulationEngine.suspendProcess(process);
                        }
                    }
                    case 3 -> {
                        listarProcesos();
                        int pid = promptPid();
                        if (pid != -1) {
                            PCB process = pm.getProcess(pid);
                            if (process.state == ProcessState.SUSPENDED) {
                                simulationEngine.resumeProcess(process);
                            } else {
                                System.out.println("Error: El proceso " + pid + 
                                                " no está suspendido. Estado actual: " + process.state);
                            }
                        }
                    }
                    case 4 -> {
                        listarProcesos();
                        int pid = promptPid();
                        if (pid != -1) {
                            terminarProceso(pid);
                        }
                    }
                    case 5 -> listarProcesos();
                    case 6 -> mostrarMemoria();
                    case 7 -> mostrarSemaforos();
                    case 8 -> avanzarSimulacion();
                    case 9 -> simulationEngine.togglePause();
                    case 10 -> System.exit(0);
                    default -> System.out.println("Opción inválida");
                }
            } else {
                // Menú simplificado cuando no hay procesos
                switch (op) {
                    case 1 -> crearProceso();
                    case 2 -> System.exit(0);
                    default -> System.out.println("Opción inválida");
                }
            }
        }
    }
    
    private void displayProcessLists() {
        List<PCB> newProcesses = simulationEngine.getNewProcesses();
        List<PCB> readyProcesses = simulationEngine.getReadyProcesses();
        List<PCB> blockedProcesses = simulationEngine.getBlockedProcesses();
        
        System.out.println("\n=== LISTAS DE PROCESOS ===");
        
        System.out.println("Procesos nuevos (" + newProcesses.size() + "):");
        for (PCB p : newProcesses) {
            System.out.println("  - " + p);
        }
        
        System.out.println("Procesos listos (" + readyProcesses.size() + "):");
        for (PCB p : readyProcesses) {
            System.out.println("  - " + p);
        }
        
        System.out.println("Procesos bloqueados (" + blockedProcesses.size() + "):");
        for (PCB p : blockedProcesses) {
            System.out.println("  - " + p);
        }
    }
    
    private void displayMemory() {
        MemoryManager memManager = rm.getMemoryManager();
        System.out.println("\n=== ESTADO DE LA MEMORIA ===");
        System.out.println("Total: " + memManager.getTotalMemory() + " MB | " +
                         "Usada: " + memManager.getUsedMemory() + " MB | " +
                         "Libre: " + memManager.getFreeMemory() + " MB");
        
        System.out.println("\nMapeo de páginas y marcos:");
        List<MemoryManager.MemoryAllocation> allocations = memManager.getMemoryMap();
        System.out.println("-".repeat(40));
        System.out.printf("%-8s %-8s %s%n", "Marco", "PID", "Página");
        System.out.println("-".repeat(40));
        
        for (MemoryManager.MemoryAllocation alloc : allocations) {
            System.out.printf("%-8d %-8s %s%n", 
                alloc.getFrameNumber(),
                alloc.getProcessId() != -1 ? alloc.getProcessId() : "libre",
                alloc.getPageNumber() != -1 ? alloc.getPageNumber() : "-"
            );
        }
        System.out.println("-".repeat(40));
    }
    
    private void avanzarSimulacion() {
        System.out.print("Pulsa Enter para avanzar un tick en la simulación...");
        scanner.nextLine();
        simulationEngine.tick();
    }
    
    private void mostrarSemaforos() {
        System.out.println("\n=== SEMÁFOROS DEL SISTEMA ===");
        List<Semaphore> semaforos = rm.getSemaphores();
        
        if (semaforos.isEmpty()) {
            System.out.println("No hay semáforos definidos.");
            return;
        }
        
        for (Semaphore sem : semaforos) {
            System.out.println(sem);
            List<PCB> waiting = sem.getWaitingProcesses();
            if (!waiting.isEmpty()) {
                System.out.println("  Procesos en espera:");
                for (PCB p : waiting) {
                    System.out.println("    - " + p.pid);
                }
            }
        }
    }
    
    // Método para terminar un proceso adecuadamente
    private void terminarProceso(int pid) {
        PCB process = pm.getProcess(pid);
        if (process != null) {
            // Primero quitamos del scheduler
            scheduler.removeProcess(process);
            
            // Luego liberamos recursos
            rm.releaseResources(process);
            
            // Finalmente lo marcamos como terminado
            pm.terminateProcess(pid, "Usuario");
            
            System.out.println("Proceso " + pid + " terminado y recursos liberados.");
            System.out.println("Memoria liberada: " + process.requiredMemory + " MB");
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
            System.out.println("Prioridad: 0=Baja, 1=Media, 2=Alta");
            System.out.print("Ingrese nivel de prioridad (0-2): ");
            pri = scanner.nextInt();
            scanner.nextLine(); // Limpiar buffer
            
            // Validar la prioridad
            if (pri < 0 || pri > 2) {
                System.out.println("Prioridad inválida. Se asignará prioridad 0 (Baja)");
                pri = 0;
            }
            
            // Convertir la prioridad simplificada a escala 0-9 para compatibilidad
            pri = pri == 0 ? 1 : (pri == 1 ? 5 : 9);
        } else {
            // Para Round Robin usamos prioridad por defecto
            pri = 0;
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
            if (pri >= 7) { // Prioridad alta (9)
                p.schedulingData.queueLevel = 2; // Alta prioridad
            } else if (pri >= 4) { // Prioridad media (5)
                p.schedulingData.queueLevel = 1; // Media prioridad
            } else { // Prioridad baja (1)
                p.schedulingData.queueLevel = 0; // Baja prioridad
            }
        }

        // Preguntar si quiere añadir ráfagas de E/S
        System.out.print("¿Desea añadir ráfagas de E/S al proceso? (s/n): ");
        String respuesta = scanner.nextLine().trim().toLowerCase();
        
        if (respuesta.equals("s")) {
            System.out.print("¿Cuántas ráfagas de E/S desea añadir?: ");
            int numRafagas = scanner.nextInt();
            scanner.nextLine(); // Limpiar buffer
            
            for (int i = 0; i < numRafagas; i++) {
                System.out.println("\nRáfaga de E/S #" + (i+1));
                
                System.out.println("Dispositivo: 1=Disco, 2=Impresora, 3=Red");
                System.out.print("Seleccione dispositivo: ");
                int deviceOpt = scanner.nextInt();
                scanner.nextLine(); // Limpiar buffer
                
                String deviceType;
                switch (deviceOpt) {
                    case 1 -> deviceType = "disk";
                    case 2 -> deviceType = "printer";
                    case 3 -> deviceType = "network";
                    default -> {
                        System.out.println("Opción inválida. Se usará disco.");
                        deviceType = "disk";
                    }
                }
                
                System.out.print("Duración de la ráfaga (ticks): ");
                int duration = scanner.nextInt();
                scanner.nextLine(); // Limpiar buffer
                
                p.addIOBurst(new PCB.IOBurst(deviceType, duration));
                System.out.println("Ráfaga de E/S añadida: " + deviceType + ", duración=" + duration);
            }
        }
        
        // Agregar proceso al simulador (que se encarga de ponerlo en estado NEW)
        simulationEngine.addProcess(p);
        System.out.println("Proceso " + p.pid + " creado exitosamente (NEW).");
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
                System.out.printf("%-5s %-10s %-10s %-10s %-15s %s%n",
                    "PID", "Estado", "Memoria", "Burst Time", "Remaining Time", "E/S");
            } else {
                System.out.printf("%-5s %-10s %-10s %-10s %-10s %s%n", 
                    "PID", "Estado", "Memoria", "Prioridad", "Cola", "E/S");
            }
            
            System.out.println("-".repeat(80));
            
            for (PCB p : procesos) {
                String ioInfo = p.ioBursts.isEmpty() ? "No" : 
                               p.currentIOIndex + "/" + p.ioBursts.size();
                
                if (scheduler instanceof RoundRobinScheduler) {
                    String burstTime = p.schedulingData != null && p.schedulingData.burstTime != null ? 
                        p.schedulingData.burstTime.toString() : "N/A";
                    String remainingTime = p.schedulingData != null && p.schedulingData.remainingTime != null ? 
                        p.schedulingData.remainingTime.toString() : "N/A";
                    System.out.printf("%-5d %-10s %-10d %-10s %-15s %s%n", 
                        p.pid, p.state, p.requiredMemory, burstTime, remainingTime, ioInfo);
                } else {
                    // Para Cola Multinivel
                    String prioridad;
                    if (p.priority >= 7) {
                        prioridad = "Alta (9)";
                    } else if (p.priority >= 4) {
                        prioridad = "Media (5)";
                    } else {
                        prioridad = "Baja (1)";
                    }
                    
                    String queueLevel = p.schedulingData != null && p.schedulingData.queueLevel != null ? 
                        (p.schedulingData.queueLevel == 2 ? "Alta" : 
                         p.schedulingData.queueLevel == 1 ? "Media" : "Baja") : "N/A";
                    
                    System.out.printf("%-5d %-10s %-10d %-10s %-10s %s%n", 
                        p.pid, p.state, p.requiredMemory, prioridad, queueLevel, ioInfo);
                }
            }
        }
    }
}


