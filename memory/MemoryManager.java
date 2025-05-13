package memory;

import core.Logger;
import models.PCB;
import java.util.*;

public class MemoryManager {
    public static final int PAGE_SIZE = 256; // Tamaño de página en KB
    public static final int FRAME_COUNT = 16; // Número de marcos en memoria física
    
    private final Frame[] frames;
    private final Map<Integer, List<Page>> processPages; // PID -> Lista de páginas
    
    public MemoryManager() {
        frames = new Frame[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            frames[i] = new Frame(i);
        }
        processPages = new HashMap<>();
        Logger.log("Sistema de memoria inicializado: " + FRAME_COUNT + " marcos de " + PAGE_SIZE + "KB");
    }
    
    public boolean allocateMemory(PCB process) {
        int memoryInKB = process.requiredMemory * 1024; // Convertir MB a KB
        int requiredPages = (int) Math.ceil((double) memoryInKB / PAGE_SIZE);
        
        // Verificar si hay suficientes marcos libres
        int freeFrames = countFreeFrames();
        if (freeFrames < requiredPages) {
            Logger.log("No hay suficientes marcos libres para proceso " + process.pid + 
                      " (requiere " + requiredPages + " páginas, hay " + freeFrames + " marcos libres)");
            return false;
        }
        
        // Crear y asignar páginas
        List<Page> pages = new ArrayList<>();
        int assignedPages = 0;
        
        for (int i = 0; i < FRAME_COUNT && assignedPages < requiredPages; i++) {
            if (frames[i].isFree()) {
                Page page = new Page(assignedPages, process.pid);
                pages.add(page);
                frames[i].assignPage(page);
                assignedPages++;
            }
        }
        
        processPages.put(process.pid, pages);
        Logger.log("Memoria asignada al proceso " + process.pid + ": " + 
                  requiredPages + " páginas en " + requiredPages + " marcos");
        
        return true;
    }
    
    public void releaseMemory(PCB process) {
        List<Page> pages = processPages.get(process.pid);
        if (pages != null) {
            for (Page page : pages) {
                // Buscar el marco que contiene esta página
                for (Frame frame : frames) {
                    if (frame.getPage() != null && 
                        frame.getPage().getProcessId() == process.pid &&
                        frame.getPage().getPageNumber() == page.getPageNumber()) {
                        frame.release();
                        break;
                    }
                }
            }
            processPages.remove(process.pid);
            Logger.log("Memoria liberada para proceso " + process.pid + ": " + 
                      pages.size() + " páginas");
        }
    }
    
    public int countFreeFrames() {
        int count = 0;
        for (Frame frame : frames) {
            if (frame.isFree()) {
                count++;
            }
        }
        return count;
    }
    
    public List<MemoryAllocation> getMemoryMap() {
        List<MemoryAllocation> allocations = new ArrayList<>();
        for (Frame frame : frames) {
            Page page = frame.getPage();
            if (page != null) {
                allocations.add(new MemoryAllocation(
                    frame.getFrameNumber(),
                    page.getProcessId(),
                    page.getPageNumber()
                ));
            } else {
                allocations.add(new MemoryAllocation(
                    frame.getFrameNumber(),
                    -1,  // No hay proceso
                    -1   // No hay página
                ));
            }
        }
        return allocations;
    }
    
    public static class MemoryAllocation {
        private final int frameNumber;
        private final int processId;
        private final int pageNumber;
        
        public MemoryAllocation(int frameNumber, int processId, int pageNumber) {
            this.frameNumber = frameNumber;
            this.processId = processId;
            this.pageNumber = pageNumber;
        }
        
        public int getFrameNumber() { return frameNumber; }
        public int getProcessId() { return processId; }
        public int getPageNumber() { return pageNumber; }
    }
    
    public int getTotalMemory() {
        return FRAME_COUNT * PAGE_SIZE / 1024; // Convertir a MB
    }
    
    public int getUsedMemory() {
        int usedFrames = FRAME_COUNT - countFreeFrames();
        return usedFrames * PAGE_SIZE / 1024; // Convertir a MB
    }
    
    public int getFreeMemory() {
        return countFreeFrames() * PAGE_SIZE / 1024; // Convertir a MB
    }
}