package com.energyapp.model;


import java.time.LocalTime;

public class ScheduledDevice extends Device{

    private LocalTime schedule;

    public ScheduledDevice(String deviceName, DeviceType deviceType) {
        super(deviceName, deviceType);
    }

    public LocalTime getSchedule() {
        return schedule;
    }

    public void setSchedule(LocalTime schedule) {
        this.schedule = schedule;
    }


}

