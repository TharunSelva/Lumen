package com.energyapp.model;


import java.time.LocalDateTime;

public class ScheduledDevice extends Device{

    private LocalDateTime schedule;

    public ScheduledDevice(String deviceName, DeviceType deviceType) {
        super(deviceName, deviceType);
    }

    public LocalDateTime getSchedule() {
        return schedule;
    }

    public void setSchedule(LocalDateTime schedule) {
        this.schedule = schedule;
    }


}

