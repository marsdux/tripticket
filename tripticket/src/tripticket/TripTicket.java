package tripticket;

public class TripTicket {
    private int id;
    private String ticketNo;
    private String driverName;
    private String vehicleNo;
    private String vehicleType;
    private String plateNumber;
    private String department;
    private String destination;
    private String purpose;
    private String departureDate;
    private String returnDate;
    private double startOdometer;
    private double endOdometer;
    private String requestedBy;
    private String approvedBy;
    private String status;
    private String remarks;
    private String createdAt;

    public TripTicket() {}

    public TripTicket(String ticketNo, String driverName, String vehicleNo,
                      String destination, String purpose, String departureDate,
                      String returnDate, String requestedBy, String approvedBy,
                      String status, String remarks) {
        this.ticketNo = ticketNo;
        this.driverName = driverName;
        this.vehicleNo = vehicleNo;
        this.destination = destination;
        this.purpose = purpose;
        this.departureDate = departureDate;
        this.returnDate = returnDate;
        this.requestedBy = requestedBy;
        this.approvedBy = approvedBy;
        this.status = status;
        this.remarks = remarks;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getVehicleNo() { return vehicleNo; }
    public void setVehicleNo(String vehicleNo) { this.vehicleNo = vehicleNo; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getDepartureDate() { return departureDate; }
    public void setDepartureDate(String departureDate) { this.departureDate = departureDate; }

    public String getReturnDate() { return returnDate; }
    public void setReturnDate(String returnDate) { this.returnDate = returnDate; }

    public double getStartOdometer() { return startOdometer; }
    public void setStartOdometer(double startOdometer) { this.startOdometer = startOdometer; }

    public double getEndOdometer() { return endOdometer; }
    public void setEndOdometer(double endOdometer) { this.endOdometer = endOdometer; }

    public double getDistanceTraveled() { return endOdometer - startOdometer; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
