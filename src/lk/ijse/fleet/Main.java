package lk.ijse.fleet;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    private static final String LINE = "========================================================";

    private FleetManager fleetManager;
    private UserManager userManager;
    private Scanner scanner;
    private SystemUser loggedInUser; // null until login succeeds

    public static void main(String[] args) {
        new Main().run();
    }

    public Main() {
        fleetManager = new FleetManager();
        userManager = new UserManager();
        scanner = new Scanner(System.in);

        // Lambda implementation of the custom functional interface
        fleetManager.setRentalEventListener(agreement ->
                System.out.println("[EVENT] New rental created: " + agreement.getAgreementId())
        );

        // Background workers as daemon threads
        Thread statusThread = new Thread(new VehicleStatusMonitor(fleetManager));
        statusThread.setDaemon(true);
        statusThread.start();

        Thread overdueThread = new Thread(new OverdueReminderWorker(fleetManager));
        overdueThread.setDaemon(true);
        overdueThread.start();
    }

    public void run() {
        while (true) {
            showAuthMenu();
            if (loggedInUser == null) {
                return;
            }
            showMainMenu();
        }
    }

    // ---------------------------------------------------------------- input helpers

    private String readLine(String prompt) {
        System.out.print(prompt);
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed. Exiting.");
            System.exit(0);
            return "";
        }
    }

    private String readNonEmpty(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (!s.isEmpty()) {
                return s;
            }
            System.out.println("Value cannot be empty.");
        }
    }

    private int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter a whole number.");
            }
        }
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            int n = readInt(prompt);
            if (n > 0) {
                return n;
            }
            System.out.println("Value must be greater than 0.");
        }
    }

    private double readPositiveDouble(String prompt) {
        while (true) {
            try {
                double d = Double.parseDouble(readLine(prompt));
                if (d > 0) {
                    return d;
                }
                System.out.println("Value must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    private FuelType readFuelType() {
        while (true) {
            System.out.println("Fuel type: [1] PETROL  [2] DIESEL  [3] ELECTRIC  [4] HYBRID");
            int c = readInt("Enter fuel choice > ");
            switch (c) {
                case 1: return FuelType.PETROL;
                case 2: return FuelType.DIESEL;
                case 3: return FuelType.ELECTRIC;
                case 4: return FuelType.HYBRID;
                default: System.out.println("Invalid fuel choice.");
            }
        }
    }

    // ---------------------------------------------------------------- auth menu

    public void showAuthMenu() {
        while (loggedInUser == null) {
            System.out.println();
            System.out.println(LINE);
            System.out.println("  FLEET RENTAL MANAGEMENT SYSTEM (FRMS)");
            System.out.println(LINE);
            System.out.println("  [1] Login");
            System.out.println("  [2] Register New User");
            System.out.println("  [0] Exit");
            String choice = readLine("  Enter choice > ");
            System.out.println(LINE);

            switch (choice) {
                case "1": {
                    String username = readLine("Username: ");
                    String password = readLine("Password: ");
                    SystemUser user = userManager.login(username, password);
                    if (user != null) {
                        loggedInUser = user;
                    } else {
                        System.out.println("Invalid username or password");
                    }
                    break;
                }
                case "2": {
                    String username = readNonEmpty("Username: ");
                    String password = readNonEmpty("Password: ");
                    String fullName = readNonEmpty("Full name: ");
                    userManager.registerUser(username, password, fullName);
                    break;
                }
                case "0":
                    System.out.println("Goodbye!");
                    return; // loggedInUser stays null -> run() ends
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // ---------------------------------------------------------------- main menu

    public void showMainMenu() {
        System.out.println();
        System.out.println("Welcome, " + loggedInUser.getFullName() + "! Loading fleet dashboard...");

        while (true) {
            System.out.println();
            System.out.println(LINE);
            System.out.println("  FRMS MAIN MENU | Logged in as: " + loggedInUser.getFullName());
            System.out.println(LINE);
            System.out.println("  [1] Add Vehicle");
            System.out.println("  [2] Add Customer");
            System.out.println("  [3] View Available Vehicles");
            System.out.println("  [4] Create Rental Agreement");
            System.out.println("  [5] Return Vehicle");
            System.out.println("  [6] View Revenue Report");
            System.out.println("  [7] View Vehicles by Fuel Type");
            System.out.println("  [8] View Combined Fleet & Customer Report");
            System.out.println("  [9] View All Customers");
            System.out.println("  [10] View All Rental Agreements");
            System.out.println("  [0] Logout");
            String choice = readLine("  Enter choice > ");
            System.out.println(LINE);

            try {
                switch (choice) {
                    case "1": addVehicle(); break;
                    case "2": addCustomer(); break;
                    case "3": viewAvailableVehicles(); break;
                    case "4": createRental(); break;
                    case "5": returnVehicle(); break;
                    case "6": viewRevenueReport(); break;
                    case "7": viewVehiclesByFuelType(); break;
                    case "8": fleetManager.printAllReportables(); break;
                    case "9": viewAllCustomers(); break;
                    case "10": viewAllAgreements(); break;
                    case "0":
                        System.out.println("Logged out. Goodbye, " + loggedInUser.getFullName() + "!");
                        loggedInUser = null;
                        return;
                    default:
                        System.out.println("Invalid choice. Please enter a number from the menu.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid numeric input. Please try again.");
            }
        }
    }

    private void addVehicle() {
        System.out.println("Vehicle type: [1] Car  [2] Van  [3] Truck");
        int type = readInt("Enter type > ");
        if (type < 1 || type > 3) {
            System.out.println("Invalid vehicle type.");
            return;
        }
        String reg = readNonEmpty("Registration number: ");
        String brand = readNonEmpty("Brand: ");
        String model = readNonEmpty("Model: ");
        double rate = readPositiveDouble("Daily rate: ");
        FuelType fuel = readFuelType();

        Vehicle v;
        if (type == 1) {
            v = new Car(reg, brand, model, rate, fuel);
        } else if (type == 2) {
            int seats = readPositiveInt("Passenger capacity: ");
            v = new Van(reg, brand, model, rate, fuel, seats);
        } else {
            double tons = readPositiveDouble("Cargo capacity (tons): ");
            v = new Truck(reg, brand, model, rate, fuel, tons);
        }
        fleetManager.addVehicle(v);
        System.out.println("Vehicle added: " + v.getVehicleId());
    }

    private void addCustomer() {
        System.out.println("Customer type: [1] Individual  [2] Corporate");
        int type = readInt("Enter type > ");
        if (type != 1 && type != 2) {
            System.out.println("Invalid customer type.");
            return;
        }
        String name = readNonEmpty("Full name: ");
        String contact = readNonEmpty("Contact number: ");
        Customer c;
        if (type == 1) {
            int points = readInt("Loyalty points: ");
            if (points < 0) {
                System.out.println("Loyalty points cannot be negative.");
                return;
            }
            c = new IndividualCustomer(name, contact, points);
        } else {
            double limit = readPositiveDouble("Credit limit: ");
            c = new CorporateCustomer(name, contact, limit);
        }
        fleetManager.addCustomer(c);
        System.out.println("Customer added: " + c.getCustomerId());
    }

    private void viewAvailableVehicles() {
        List<Vehicle> list = fleetManager.getAvailableVehicles();
        if (list.isEmpty()) {
            System.out.println("No available vehicles.");
            return;
        }
        for (Vehicle v : list) {
            System.out.println(v);
        }
    }

    private void createRental() {
        String vehicleId = readNonEmpty("Vehicle ID (e.g. VEH-1001): ");
        String customerId = readNonEmpty("Customer ID (e.g. CUS-1001): ");
        int days = readPositiveInt("Rental days: ");
        try {
            RentalAgreement agreement = fleetManager.createRentalAgreement(vehicleId, customerId, days);
            if (agreement != null) {
                System.out.println(agreement);
            }
        } catch (VehicleNotAvailableException | InsufficientCreditException e) {
            System.out.println("Rental failed: " + e.getMessage());
        }
    }

    private void returnVehicle() {
        String id = readNonEmpty("Agreement ID (e.g. AGR-1001): ");
        fleetManager.returnVehicle(id);
    }

    private void viewRevenueReport() {
        System.out.println("================================================================");
        System.out.println("FLEET REVENUE REPORT");
        System.out.println("================================================================");
        System.out.println("Total Revenue: " + String.format("%,.2f", fleetManager.generateRevenueReport()));
        System.out.println("----------------------------------------------------------------");
    }

    private void viewVehiclesByFuelType() {
        FuelType type = readFuelType();
        List<Vehicle> list = fleetManager.getVehiclesByFuelType(type);
        if (list.isEmpty()) {
            System.out.println("No vehicles with fuel type " + type + ".");
            return;
        }
        for (Vehicle v : list) {
            System.out.println(v);
        }
    }

    private void viewAllCustomers() {
        List<Customer> list = fleetManager.getAllCustomers();
        if (list.isEmpty()) {
            System.out.println("No customers registered.");
            return;
        }
        for (Customer c : list) {
            System.out.println(c);
        }
    }

    private void viewAllAgreements() {
        List<RentalAgreement> list = fleetManager.getAllAgreements();
        if (list.isEmpty()) {
            System.out.println("No rental agreements yet.");
            return;
        }
        for (RentalAgreement a : list) {
            System.out.println(a);
        }
    }
}
