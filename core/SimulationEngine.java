package core;

import java.util.*;
import models.*;
import core.scheduling.*;
import memory.*;

public class SimulationEngine {
    private final Scheduler scheduler;
    private final ProcessManager processManager;
    private final ResourceManager resourceManager;
    
    private PCB runningProcess = null;
    private int currentTime = 0;
    private boolean paused = false;
    
    // Colas de procesos en diferentes estados
    private final List<PCB> newProcesses = new ArrayList<>();
    private final List<PCB> readyProcesses = new ArrayList<>();
    private final List<PCB> blockedProcesses = new ArrayList<>();
    private final List<PCB> suspendedProcesses = new ArrayList<>();
    
    public SimulationEngine(Scheduler scheduler, ProcessManager processManager, ResourceManager resourceManager) {
        this.scheduler = scheduler;
        this.processManager = processManager;
        this.resourceManager = resourceManager;
    }
    
    public void addProcess(PCB process) {
        process.state = ProcessState.NEW;
        newProcesses.add(process);
        Logger.log("Proceso " + process.pid + " creado (NEW)");
    }
    
    public boolean tick() {
        if (paused) return false;
        
        currentTime++;
        
        // 1. Mover procesos de NEW a READY
        moveNewToReady();
        
        // 2. Manejar proceso en RUNNING
        handleRunningProcess();
        
        // 3. Manejar procesos bloqueados (E/S)
        handleBlockedProcesses();
        
        // 4. Seleccionar nuevo proceso a ejecutar si es necesario
        if (runningProcess == null) {
            selectNextProcess();
        }
        
        Logger.log("Simulación: Tick " + currentTime);
        return true;
    }
    
    private void moveNewToReady() {
        List<PCB> readyToMove = new ArrayList<>();
        for (PCB process : newProcesses) {
            // Asignar recursos y cambiar a estado READY
            if (resourceManager.requestResources(process)) {
                process.state = ProcessState.READY;
                readyToMove.add(process);
                Logger.log("Proceso " + process.pid + " pasa a READY");
                
                // Agregar proceso al planificador
                scheduler.addProcess(process);
                readyProcesses.add(process);
            }
        }
        newProcesses.removeAll(readyToMove);
    }
    
    private void handleRunningProcess() {
        if (runningProcess != null) {
            // Decrementar el tiempo restante
            if (runningProcess.schedulingData != null && 
                runningProcess.schedulingData.remainingTime != null) {
                
                runningProcess.schedulingData.remainingTime--;
                
                // Verificar si se completó
                if (runningProcess.schedulingData.remainingTime <= 0) {
                    Logger.log("Proceso " + runningProcess.pid + " completó su ejecución");
                    completeProcess(runningProcess);
                    runningProcess = null;
                }
                // Si usa Round Robin, verificar quantum
                else if (scheduler instanceof RoundRobinScheduler &&
                        runningProcess.schedulingData.quantum != null) {
                    
                    // Decrementar contador del quantum
                    int remainingQuantum = runningProcess.schedulingData.quantum - 1;
                    runningProcess.schedulingData.quantum = remainingQuantum;
                    
                    // Si se acabó el quantum pero aún tiene tiempo de CPU
                    if (remainingQuantum <= 0) {
                        Logger.log("Proceso " + runningProcess.pid + " agotó su quantum");
                        // Resetear quantum y poner al final de la cola
                        RoundRobinScheduler rr = (RoundRobinScheduler) scheduler;
                        runningProcess.schedulingData.quantum = rr.getQuantum();
                        runningProcess.state = ProcessState.READY;
                        
                        readyProcesses.add(runningProcess);
                        scheduler.addProcess(runningProcess);
                        runningProcess = null;
                    }
                }
                
                // Verificar si hay una ráfaga de E/S pendiente
                if (runningProcess != null && runningProcess.hasMoreIOBursts()) {
                    PCB.IOBurst ioBurst = runningProcess.getCurrentIOBurst();
                    // Proceso bloqueado para E/S
                    Logger.log("Proceso " + runningProcess.pid + " bloqueado para E/S: " + 
                             ioBurst.getDeviceType() + ", duración=" + ioBurst.getDuration());
                    
                    if (resourceManager.requestIODevice(runningProcess, ioBurst.getDeviceType())) {
                        runningProcess.state = ProcessState.BLOCKED;
                        blockedProcesses.add(runningProcess);
                        runningProcess = null;
                    }
                }
            }
        }
    }
    
    private void handleBlockedProcesses() {
        List<PCB> processesToUnblock = new ArrayList<>();
        
        for (PCB process : blockedProcesses) {
            PCB.IOBurst ioBurst = process.getCurrentIOBurst();
            if (ioBurst != null) {
                ioBurst.decrementTime();
                
                if (ioBurst.isComplete()) {
                    // La operación de E/S ha terminado
                    Logger.log("Proceso " + process.pid + " completó E/S: " + 
                             ioBurst.getDeviceType());
                    
                    // Liberar dispositivo E/S
                    resourceManager.releaseIODevice(process, ioBurst.getDeviceType());
                    
                    // Marcar operación como completada y mover a ready
                    process.completeCurrentIOBurst();
                    process.state = ProcessState.READY;
                    
                    // Agregar a la cola de listos
                    readyProcesses.add(process);
                    scheduler.addProcess(process);
                    
                    processesToUnblock.add(process);
                }
            }
        }
        
        blockedProcesses.removeAll(processesToUnblock);
    }
    
    private void selectNextProcess() {
        PCB nextProcess = scheduler.getNextProcess();
        if (nextProcess != null) {
            readyProcesses.remove(nextProcess);
            nextProcess.state = ProcessState.RUNNING;
            runningProcess = nextProcess;
            Logger.log("Proceso " + nextProcess.pid + " pasa a RUNNING");
        }
    }
    
    private void completeProcess(PCB process) {
        scheduler.onProcessFinished(process);
        process.state = ProcessState.TERMINATED;
        
        // Liberar recursos
        resourceManager.releaseResources(process);
        
        // Eliminar de todas las listas activas (por si acaso)
        newProcesses.remove(process);
        readyProcesses.remove(process);
        blockedProcesses.remove(process);
    }
    
    public void togglePause() {
        this.paused = !this.paused;
        Logger.log("Simulación " + (paused ? "pausada" : "reanudada"));
    }
    
    public List<PCB> getNewProcesses() {
        return new ArrayList<>(newProcesses);
    }
    
    public List<PCB> getReadyProcesses() {
        return new ArrayList<>(readyProcesses);
    }
    
    public List<PCB> getBlockedProcesses() {
        return new ArrayList<>(blockedProcesses);
    }
    
    public PCB getRunningProcess() {
        return runningProcess;
    }
    
    public int getCurrentTime() {
        return currentTime;
    }
    
    public boolean isPaused() {
        return paused;
    }
    
    public void suspendProcess(PCB process) {
        if (process == runningProcess) {
            runningProcess = null;
        } else {
            readyProcesses.remove(process);
            blockedProcesses.remove(process);
        }
        
        process.state = ProcessState.SUSPENDED;
        suspendedProcesses.add(process);
        Logger.log("Proceso " + process.pid + " suspendido");
    }
    
    public void resumeProcess(PCB process) {
        if (process.state == ProcessState.SUSPENDED) {
            process.state = ProcessState.READY;
            suspendedProcesses.remove(process);
            readyProcesses.add(process);
            scheduler.addProcess(process);
            Logger.log("Proceso " + process.pid + " reanudado (READY)");
        } else {
            Logger.log("Error: No se puede reanudar el proceso " + process.pid + 
                     " porque no está suspendido. Estado actual: " + process.state);
        }
    }
}