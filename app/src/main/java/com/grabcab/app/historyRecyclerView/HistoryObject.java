package com.grabcab.app.historyRecyclerView;

/**
 * Created by manel on 10/10/2017.
 */

public class HistoryObject {
    private String rideId;
    private String time;

    public HistoryObject(String rideId, String time) {
        this.rideId = rideId;
        this.time = time;
    }

    private String customerPaid;

    public String getCustomerPaid() {
        return customerPaid;
    }

    public void setCustomerPaid(String customerPaid) {
        this.customerPaid = customerPaid;
    }

    public HistoryObject(String rideId, String time, String customerPaid){
        this.rideId = rideId;
        this.time = time;
        this.customerPaid = customerPaid;
    }

    public String getRideId(){return rideId;}
    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getTime(){return time;}
    public void setTime(String time) {
        this.time = time;
    }
}
