package com.energyapp.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.LocalDate;
import java.time.LocalTime;

public abstract class ScheduledDevice extends Device {

    private final ObjectProperty<LocalTime> schedule = new SimpleObjectProperty<>();
    private final BooleanProperty skipNextRun = new SimpleBooleanProperty(false);
    private final ObjectProperty<LocalDate> lastScheduledRunDate = new SimpleObjectProperty<>();

    protected ScheduledDevice(String deviceName, DeviceType deviceType) {
        super(deviceName, deviceType);
    }

    public LocalTime getSchedule() {
        return schedule.get();
    }

    public void setSchedule(LocalTime schedule) {
        this.schedule.set(schedule);
    }

    public ObjectProperty<LocalTime> scheduleProperty() {
        return schedule;
    }

    public boolean isSkipNextRun() {
        return skipNextRun.get();
    }

    public void setSkipNextRun(boolean value) {
        skipNextRun.set(value);
    }

    public BooleanProperty skipNextRunProperty() {
        return skipNextRun;
    }

    public LocalDate getLastScheduledRunDate() {
        return lastScheduledRunDate.get();
    }

    public void setLastScheduledRunDate(LocalDate value) {
        lastScheduledRunDate.set(value);
    }

    public ObjectProperty<LocalDate> lastScheduledRunDateProperty() {
        return lastScheduledRunDate;
    }

}

