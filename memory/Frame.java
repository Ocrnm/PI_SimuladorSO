package memory;

/**
 * Representa un marco de memoria física
 */
public class Frame {
    private final int frameNumber;
    private Page page;
    
    public Frame(int frameNumber) {
        this.frameNumber = frameNumber;
        this.page = null;
    }
    
    public int getFrameNumber() {
        return frameNumber;
    }
    
    public Page getPage() {
        return page;
    }
    
    public void assignPage(Page page) {
        this.page = page;
    }
    
    public void release() {
        if (page != null) {
            page.setInMemory(false);
            page = null;
        }
    }
    
    public boolean isFree() {
        return page == null;
    }
    
    @Override
    public String toString() {
        return "Marco" + frameNumber + ": " + (page != null ? page.toString() : "libre");
    }
}