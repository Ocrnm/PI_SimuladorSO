package models;

/**
 * Clase que representa un recurso del sistema que puede ser asignado a un proceso
 */
public class Resource {
    private static int nextId = 1;
    private final int id;
    private final String name;
    private boolean available;

    public Resource(String name) {
        this.id = nextId++;
        this.name = name;
        this.available = true;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return name;
    }
}