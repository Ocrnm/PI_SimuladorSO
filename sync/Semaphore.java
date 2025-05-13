package sync;

import core.Logger;
import models.PCB;

import java.util.ArrayList;
import java.util.List;

public class Semaphore {
    private final String name;
    private int value;
    private final List<PCB> waitingProcesses;

    public Semaphore(String name, int initialValue) {
        this.name = name;
        this.value = initialValue;
        this.waitingProcesses = new ArrayList<>();
        Logger.log("Semáforo '" + name + "' creado con valor inicial " + initialValue);
    }

    public synchronized boolean wait(PCB process) {
        this.value--;
        if (this.value < 0) {
            // Bloquear proceso
            waitingProcesses.add(process);
            Logger.log("Proceso " + process.pid + " bloqueado en semáforo '" + name + "'");
            return false;
        }
        return true;
    }

    public synchronized PCB signal() {
        this.value++;
        if (!waitingProcesses.isEmpty() && this.value <= 0) {
            PCB process = waitingProcesses.remove(0);
            Logger.log("Proceso " + process.pid + " desbloqueado de semáforo '" + name + "'");
            return process;
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public List<PCB> getWaitingProcesses() {
        return new ArrayList<>(waitingProcesses);
    }
    
    @Override
    public String toString() {
        return "Semáforo '" + name + "': valor=" + value + ", procesos en espera=" + waitingProcesses.size();
    }
}