package core;

import java.util.ArrayList;
import java.util.List;
import models.*;

public class ResourceManager {
    private final Resource cpu = new Resource("CPU");
    private final int totalMemory = 4096;
    private int usedMemory = 0;
    private final List<Resource> availableResources = new ArrayList<>();

    public ResourceManager() {
        // Inicializar recursos disponibles
        availableResources.add(cpu);
    }

    public boolean requestResources(PCB process) {
        // Verificar la memoria disponible
        if (usedMemory + process.requiredMemory > totalMemory) {
            Logger.log("No hay memoria suficiente para Proceso " + process.pid);
            return false;
        }

        // Asignar memoria
        usedMemory += process.requiredMemory;
        
        // Asignar una copia del CPU (simulando multitarea)
        Resource cpuInstance = new Resource("CPU-" + process.pid);
        process.assignedResources.add(cpuInstance);
        
        Logger.log("Recursos asignados al Proceso " + process.pid + 
                  " (Memoria: " + process.requiredMemory + "MB)");
        return true;
    }

    public void releaseResources(PCB process) {
        usedMemory -= process.requiredMemory;
        process.assignedResources.clear();
        Logger.log("Recursos liberados por el Proceso " + process.pid + 
                  " (Memoria recuperada: " + process.requiredMemory + "MB)");
    }

    public int getAvailableMemory() {
        return totalMemory - usedMemory;
    }
    
    public int getTotalMemory() {
        return totalMemory;
    }
}


