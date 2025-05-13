package models;

import java.util.*;

public class PCB {
    private static int nextPid = 1;
    public final int pid;
    public ProcessState state;
    public int priority;
    public int requiredMemory;
    public List<Resource> assignedResources;
    public SchedulingData schedulingData;

    public PCB(int priority, int requiredMemory) {
        this.pid = nextPid++;
        this.priority = priority;
        this.requiredMemory = requiredMemory;
        this.state = ProcessState.READY;
        this.assignedResources = new ArrayList<>();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PID: ").append(pid);
        sb.append(" | Estado: ").append(state);
        sb.append(" | Memoria: ").append(requiredMemory).append("MB");
        
        // Solo incluir prioridad para Cola Multinivel
        if (schedulingData != null && schedulingData.queueLevel != null) {
            sb.append(" | Prioridad: ").append(priority);
            sb.append(" | Cola: ");
            switch (schedulingData.queueLevel) {
                case 2: sb.append("Alta"); break;
                case 1: sb.append("Media"); break;
                case 0: sb.append("Baja"); break;
                default: sb.append("N/A"); break;
            }
        }
        
        // Incluir información de Round Robin
        if (schedulingData != null && schedulingData.burstTime != null) {
            sb.append(" | Burst Time: ").append(schedulingData.burstTime);
            sb.append(" | Remaining: ").append(schedulingData.remainingTime);
            sb.append(" | Quantum: ").append(schedulingData.quantum);
        }
        
        return sb.toString();
    }
}
