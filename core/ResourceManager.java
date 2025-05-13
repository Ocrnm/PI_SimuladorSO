package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import memory.MemoryManager;
import models.*;
import sync.Semaphore;

public class ResourceManager {
    private final MemoryManager memoryManager;
    private final Map<String, Semaphore> semaphores;
    private final Map<String, Resource> ioDevices;
    
    public ResourceManager() {
        this.memoryManager = new MemoryManager();
        this.semaphores = new HashMap<>();
        this.ioDevices = new HashMap<>();
        
        // Inicializar semáforos básicos
        semaphores.put("IO", new Semaphore("IO", 1)); // Semáforo para operaciones E/S
        semaphores.put("CPU", new Semaphore("CPU", 1)); // Semáforo para la CPU
        
        // Inicializar dispositivos E/S
        ioDevices.put("disk", new Resource("Disco"));
        ioDevices.put("printer", new Resource("Impresora"));
        ioDevices.put("network", new Resource("Red"));
        
        Logger.log("ResourceManager inicializado con " + 
                  memoryManager.getTotalMemory() + "MB de memoria y " + 
                  semaphores.size() + " semáforos");
    }

    public boolean requestResources(PCB process) {
        // Intentar asignar memoria
        if (!memoryManager.allocateMemory(process)) {
            return false;
        }
        
        // Asignar recursos
        Resource cpuInstance = new Resource("CPU-" + process.pid);
        process.assignedResources.add(cpuInstance);
        
        Logger.log("Recursos asignados al Proceso " + process.pid + 
                 " (Memoria: " + process.requiredMemory + "MB)");
        return true;
    }

    public void releaseResources(PCB process) {
        // Liberar memoria
        memoryManager.releaseMemory(process);
        
        // Liberar recursos
        process.assignedResources.clear();
        
        Logger.log("Recursos liberados por el Proceso " + process.pid);
    }

    public boolean acquireSemaphore(PCB process, String semaphoreName) {
        Semaphore semaphore = semaphores.get(semaphoreName);
        if (semaphore != null) {
            return semaphore.wait(process);
        }
        return false;
    }
    
    public void releaseSemaphore(String semaphoreName) {
        Semaphore semaphore = semaphores.get(semaphoreName);
        if (semaphore != null) {
            PCB released = semaphore.signal();
            if (released != null) {
                released.state = ProcessState.READY;
            }
        }
    }
    
    public List<Semaphore> getSemaphores() {
        return new ArrayList<>(semaphores.values());
    }
    
    public Semaphore createSemaphore(String name, int initialValue) {
        Semaphore semaphore = new Semaphore(name, initialValue);
        semaphores.put(name, semaphore);
        return semaphore;
    }
    
    public boolean requestIODevice(PCB process, String deviceName) {
        Resource device = ioDevices.get(deviceName);
        if (device != null && device.isAvailable()) {
            device.setAvailable(false);
            process.assignedResources.add(device);
            return true;
        }
        return false;
    }
    
    public void releaseIODevice(PCB process, String deviceName) {
        Resource device = ioDevices.get(deviceName);
        if (device != null) {
            device.setAvailable(true);
            process.assignedResources.remove(device);
        }
    }

    public int getAvailableMemory() {
        return memoryManager.getFreeMemory();
    }
    
    public int getTotalMemory() {
        return memoryManager.getTotalMemory();
    }
    
    public MemoryManager getMemoryManager() {
        return memoryManager;
    }
}


