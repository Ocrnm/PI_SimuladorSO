package memory;

/**
 * Representa una página en la memoria virtual de un proceso
 */
public class Page {
    private final int pageNumber;
    private final int processId;
    private boolean isInMemory;
    
    public Page(int pageNumber, int processId) {
        this.pageNumber = pageNumber;
        this.processId = processId;
        this.isInMemory = true;
    }
    
    public int getPageNumber() {
        return pageNumber;
    }
    
    public int getProcessId() {
        return processId;
    }
    
    public boolean isInMemory() {
        return isInMemory;
    }
    
    public void setInMemory(boolean inMemory) {
        isInMemory = inMemory;
    }
    
    @Override
    public String toString() {
        return "P" + processId + ":Pág" + pageNumber;
    }
}