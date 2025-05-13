package core.scheduling;

import models.PCB;

import java.util.List;

public interface Scheduler {
    void addProcess(PCB process);
    PCB getNextProcess(); // retorna el proceso que debe ejecutarse
    void removeProcess(PCB process);
    void onProcessFinished(PCB process);
    String getName();
}