package core;
import java.util.*;
import models.*;

public class ProcessManager {
    private List<PCB> processList = new ArrayList<>();

    public PCB createProcess(int priority, int memory) {
        PCB process = new PCB(priority, memory);
        processList.add(process);
        // Movemos el mensaje de log a después de verificar recursos en ConsoleInterface
        return process;
    }

    public void suspendProcess(int pid) {
        getProcess(pid).state = ProcessState.SUSPENDED;
        Logger.log("Proceso " + pid + " suspendido");
    }

    public void resumeProcess(int pid) {
        getProcess(pid).state = ProcessState.READY;
        Logger.log("Proceso " + pid + " continúa su ejecución");
    }

    public void terminateProcess(int pid, String reason) {
        PCB process = getProcess(pid);
        process.state = ProcessState.TERMINATED;
        Logger.log("Proceso " + pid + " terminado | Razón: " + reason);
    }

    public List<PCB> getActiveProcesses() {
        return processList.stream().filter(p -> p.state != ProcessState.TERMINATED).toList();
    }

    public boolean isProcessActive(int pid) {
        return processList.stream().anyMatch(p -> p.pid == pid && p.state != ProcessState.TERMINATED);
    }

    public PCB getProcess(int pid) {
        return processList.stream().filter(p -> p.pid == pid).findFirst().orElseThrow();
    }
}
