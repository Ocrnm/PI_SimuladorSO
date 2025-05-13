package models;

public class SchedulingData {
    public Integer burstTime;       // usado por SJF o RR
    public Integer remainingTime;   // RR
    public Integer queueLevel;      // Multilevel Queue
    public Long arrivalTime;        // Para FCFS o SJF
    public Integer quantum;         // RR (si se requiere por proceso)

    public SchedulingData() {}

@Override
public String toString() {
    return "[BT=" + burstTime + ", Rem=" + remainingTime +
           ", QL=" + queueLevel + ", Arr=" + arrivalTime +"]";
}
}