import core.scheduling.*;
import java.util.Scanner;
import ui.ConsoleInterface;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("=== SIMULADOR DE SISTEMA OPERATIVO ===");
        System.out.println("Seleccione algoritmo de planificación:");
        System.out.println("1. Round Robin");
        System.out.println("2. Cola Multinivel");
        System.out.print("Opción: ");
        int opt = sc.nextInt();

        Scheduler scheduler;
        
        switch (opt) {
            case 1:
                System.out.print("Ingrese el quantum para Round Robin: ");
                int quantum = sc.nextInt();
                scheduler = new RoundRobinScheduler(quantum);
                System.out.println("Round Robin configurado con quantum = " + quantum);
                break;
            case 2:
                scheduler = new MultilevelQueueScheduler();
                System.out.println("Cola Multinivel seleccionada");
                break;
            default:
                throw new IllegalArgumentException("Algoritmo no válido");
        }
        
        System.out.println("\nIniciando simulador...");
        System.out.println("Use la opción 'Avanzar simulación' para controlar el progreso");
        System.out.println("Los procesos pasarán por estados: CREACIÓN → NEW → READY → RUNNING");

        ConsoleInterface ui = new ConsoleInterface(scheduler);
        ui.start();
    }
}