package lk.ijse.fleet;

public class RentalAgreement {
    private static int counter = 1001;

    private String agreementId;
    private Vehicle vehicle;     // composition (has-a)
    private Customer customer;   // composition (has-a)
    private int rentalDays;
    private double totalCost;
    private int daysElapsed = 0;

    public RentalAgreement(Vehicle vehicle, Customer customer, int rentalDays) {
        this.vehicle = vehicle;
        this.customer = customer;
        this.rentalDays = rentalDays;
        double subtotal = vehicle.calculateRentalCost(rentalDays);
        this.totalCost = customer.applyDiscount(subtotal);
        this.agreementId = "AGR-" + (counter++);
        this.daysElapsed = 0;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Customer getCustomer() {
        return customer;
    }

    public int getRentalDays() {
        return rentalDays;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public int getDaysElapsed() {
        return daysElapsed;
    }

    public void setDaysElapsed(int daysElapsed) {
        this.daysElapsed = daysElapsed;
    }

    @Override
    public String toString() {
        String line = "================================================================";
        String customerType = (customer instanceof CorporateCustomer) ? "Corporate" : "Individual";
        return line + "\n"
                + "RENTAL AGREEMENT RECEIPT\n"
                + line + "\n"
                + "Agreement ID : " + agreementId + "\n"
                + "Vehicle      : " + vehicle.getVehicleId() + " - " + vehicle.getBrand() + " "
                + vehicle.getModel() + " (" + vehicle.getClass().getSimpleName() + ")\n"
                + "Customer     : " + customer.getCustomerId() + " - " + customer.getFullName()
                + " (" + customerType + ")\n"
                + "Rental Days  : " + rentalDays + "\n"
                + "Total Cost   : " + String.format("%,.2f", totalCost) + "\n"
                + line;
    }
}
