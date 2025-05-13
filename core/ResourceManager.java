package core;

import java.util.HashMap;
import java.util.Map;
import models.*;

public class ResourceManager {
    private final int totalMemory = 4096;
    private int usedMemory = 0;
    private final Map<Integer, Integer> processoMemoryMap = new HashMap<>();

    public ResourceManager() {
        // Constructor vacío
    }

    public boolean requestResources(PCB process) {
        // Verificar la memoria disponible
        if (usedMemory + process.requiredMemory > totalMemory) {
            Logger.log("No hay memoria suficiente para Proceso " + process.pid);
            return false;
        }

        // Asignar memoria
        usedMemory += process.requiredMemory;
        processoMemoryMap.put(process.pid, process.requiredMemory);
        
        // Registramos la asignación
        Resource memory = new Resource("Memoria-" + process.requiredMemory + "MB");
        process.assignedResources.add(memory);
        
        Logger.log("Recursos asignados al Proceso " + process.pid + 
                  " (Memoria: " + process.requiredMemory + "MB)");
        return true;
    }

    public void releaseResources(PCB process) {
        // Verificar que el proceso tenga recursos asignados
        Integer memoryAssigned = processoMemoryMap.get(process.pid);
        if (memoryAssigned != null) {
            usedMemory -= memoryAssigned;
            processoMemoryMap.remove(process.pid);
            Logger.log("Recursos liberados por el Proceso " + process.pid + 
                      " (Memoria recuperada: " + memoryAssigned + "MB)");
        }
        
        // Limpiar la lista de recursos asignados al proceso
        process.assignedResources.clear();
    }

    public int getAvailableMemory() {
        return totalMemory - usedMemory;
    }
    
    public int getTotalMemory() {
        return totalMemory;
    }
    
    public int getMemoryUsedByProcess(int pid) {
        return processoMemoryMap.getOrDefault(pid, 0);
    }
}


