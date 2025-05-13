package models;

import java.time.LocalDateTime;
import java.util.*;

public class PCB {
    private static int nextPid = 1;
    public final int pid;
    public ProcessState state;
    public int priority;
    public int requiredMemory; // en MB
    public List<Resource> assignedResources;
    public SchedulingData schedulingData;
    public LocalDateTime creationTime;
    public LocalDateTime startTime;
    public List<IOBurst> ioBursts;
    public int currentIOIndex;
    public List<PageAccess> pageAccesses;
    
    public PCB(int priority, int requiredMemory) {
        this.pid = nextPid++;
        this.priority = priority;
        this.requiredMemory = requiredMemory;
        this.state = ProcessState.NEW; // Inicializado en NEW, no en READY
        this.assignedResources = new ArrayList<>();
        this.creationTime = LocalDateTime.now();
        this.ioBursts = new ArrayList<>();
        this.currentIOIndex = 0;
        this.pageAccesses = new ArrayList<>();
    }
    
    public void addIOBurst(IOBurst ioBurst) {
        this.ioBursts.add(ioBurst);
    }
    
    public IOBurst getCurrentIOBurst() {
        if (currentIOIndex < ioBursts.size()) {
            return ioBursts.get(currentIOIndex);
        }
        return null;
    }
    
    public void completeCurrentIOBurst() {
        currentIOIndex++;
    }
    
    public boolean hasMoreIOBursts() {
        return currentIOIndex < ioBursts.size();
    }
    
    public void recordPageAccess(int pageNumber, boolean isWrite) {
        pageAccesses.add(new PageAccess(pageNumber, isWrite));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PID: ").append(pid);
        sb.append(" | Estado: ").append(state);
        sb.append(" | Memoria: ").append(requiredMemory).append("MB");
        
        // Información adicional según el algoritmo
        if (schedulingData != null) {
            if (schedulingData.queueLevel != null) {
                sb.append(" | Prioridad: ");
                if (priority >= 7) {
                    sb.append("2 (Alta)");
                } else if (priority >= 4) {
                    sb.append("1 (Media)");
                } else {
                    sb.append("0 (Baja)");
                }
                
                sb.append(" | Cola: ");
                switch (schedulingData.queueLevel) {
                    case 2: sb.append("Alta"); break;
                    case 1: sb.append("Media"); break;
                    case 0: sb.append("Baja"); break;
                    default: sb.append("N/A"); break;
                }
            }
            
            if (schedulingData.burstTime != null) {
                sb.append(" | Burst Time: ").append(schedulingData.burstTime);
                // Asegurar que el tiempo restante nunca sea negativo para mostrar
                int remainingTime = schedulingData.remainingTime != null ? 
                    Math.max(0, schedulingData.remainingTime) : 0;
                sb.append(" | Remaining: ").append(remainingTime);
                sb.append(" | Quantum: ").append(schedulingData.quantum);
            }
        }
        
        // Información de ráfagas E/S
        if (!ioBursts.isEmpty()) {
            sb.append(" | IO: ").append(currentIOIndex).append("/").append(ioBursts.size());
        }
        
        return sb.toString();
    }
    
    public static class PageAccess {
        private final int pageNumber;
        private final boolean isWrite;
        private final LocalDateTime timestamp;
        
        public PageAccess(int pageNumber, boolean isWrite) {
            this.pageNumber = pageNumber;
            this.isWrite = isWrite;
            this.timestamp = LocalDateTime.now();
        }
        
        public int getPageNumber() { return pageNumber; }
        public boolean isWrite() { return isWrite; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class IOBurst {
        private final String deviceType; // "disk", "printer", etc.
        private final int duration;
        private int remainingTime;
        
        public IOBurst(String deviceType, int duration) {
            this.deviceType = deviceType;
            this.duration = duration;
            this.remainingTime = duration;
        }
        
        public String getDeviceType() { return deviceType; }
        public int getDuration() { return duration; }
        public int getRemainingTime() { return remainingTime; }
        
        public void decrementTime() {
            if (remainingTime > 0) {
                remainingTime--;
            }
        }
        
        public boolean isComplete() {
            return remainingTime <= 0;
        }
    }
}
