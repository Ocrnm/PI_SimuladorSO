package core;
import java.util.*;
import models.*;

public class ProcessManager {
    private List<PCB> processList = new ArrayList<>();

    public PCB createProcess(int priority, int memory) {
        PCB process = new PCB(priority, memory);
        processList.add(process);
        return process;
    }

    public void suspendProcess(int pid) {
        PCB process = getProcess(pid);
        if (process != null) {
            process.state = ProcessState.SUSPENDED;
            Logger.log("Proceso " + pid + " suspendido");
            System.out.println("Proceso " + pid + " suspendido exitosamente.");
        }
    }

    public void resumeProcess(int pid) {
        PCB process = getProcess(pid);
        if (process != null) {
            process.state = ProcessState.READY;
            Logger.log("Proceso " + pid + " continúa su ejecución");
            System.out.println("Proceso " + pid + " reanudado exitosamente.");
        }
    }

    public void terminateProcess(int pid, String reason) {
        PCB process = getProcess(pid);
        if (process != null) {
            process.state = ProcessState.TERMINATED;
            Logger.log("Proceso " + pid + " terminado | Razón: " + reason);
        }
    }

    public List<PCB> getActiveProcesses() {
        return processList.stream()
            .filter(p -> p.state != ProcessState.TERMINATED)
            .toList();
    }

    public boolean isProcessActive(int pid) {
        return processList.stream()
            .anyMatch(p -> p.pid == pid && p.state != ProcessState.TERMINATED);
    }

    public PCB getProcess(int pid) {
        return processList.stream()
            .filter(p -> p.pid == pid)
            .findFirst()
            .orElse(null);
    }
    
    public void cleanupTerminatedProcesses() {
        // Solo para gestión manual de memoria, si fuera necesario
        processList.removeIf(p -> p.state == ProcessState.TERMINATED);
    }
}
