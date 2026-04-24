package com.energyapp.service;

import com.energyapp.db.CanonicalDeviceMapper;
import com.energyapp.db.CanonicalDeviceRecord;
import com.energyapp.db.DeviceRepository;
import com.energyapp.model.Device;
import com.energyapp.model.DeviceRunMode;
import com.energyapp.model.DeviceStatus;
import com.energyapp.model.ScheduledDevice;
import com.energyapp.model.SmartPlug;
import com.energyapp.model.WashingMachine;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DeviceService {

    private static final String HOME_ID = "HOME_1";

    private final List<Device> allDevices;
    private final DeviceRepository deviceRepository;
    private final SimulationService simulationService;
    private final String homeId;

    public DeviceService(SimulationService service) {
        this(service, new DeviceRepository(), HOME_ID);
    }

    DeviceService(SimulationService simulationService, DeviceRepository deviceRepository, String homeId) {
        this.simulationService = simulationService;
        this.deviceRepository = deviceRepository;
        this.homeId = homeId;
        this.allDevices = new ArrayList<>(deviceRepository.fetchActiveByHome(homeId).stream()
                .map(CanonicalDeviceMapper::toDevice)
                .filter(Objects::nonNull)
                .toList());

        simulationService.getCurrentEnergyRecord().addListener((observable, oldRec, newRec) -> {
            if (newRec != null) {
                handleSimulationUpdate(newRec);
            }
        });
    }

    public List<Device> getAllDevices() {
        return this.allDevices;
    }

    public void addDevice(Device device) {
        ensureCanonicalIdentity(device);
        refreshDeviceStatus(device, currentReferenceTime());
        deviceRepository.insert(CanonicalDeviceMapper.newRecord(device, homeId, currentReferenceTime()));
        this.allDevices.add(device);
    }

    public void saveDevice(Device device) {
        enforceNonInterruptibleState(device, currentReferenceTime());
        persistDevice(device, currentReferenceTime());
    }

    public void useNow(ScheduledDevice device) {
        useNow(device, currentReferenceTime());
    }

    void useNow(ScheduledDevice device, LocalDateTime startTime) {
        if (isNonInterruptibleRunning(device, startTime)) {
            persistDevice(device, startTime);
            return;
        }
        startRun(device, startTime, DeviceRunMode.MANUAL, true);
        persistDevice(device, startTime);
    }

    public void applyPlan(ScheduledDevice device, LocalDateTime windowStart, LocalDateTime referenceTime) {
        if (device == null || windowStart == null) {
            return;
        }

        LocalDateTime effectiveReference = referenceTime == null ? currentReferenceTime() : referenceTime;
        device.setSchedule(windowStart.toLocalTime());
        device.setSkipNextRun(false);

        LocalDateTime windowEnd = windowStart.plus(resolveRunDuration(device));
        if (!effectiveReference.isBefore(windowStart) && effectiveReference.isBefore(windowEnd)) {
            if (!isNonInterruptibleRunning(device, effectiveReference)) {
                startRun(device, effectiveReference, DeviceRunMode.SCHEDULED, true);
            }
        } else if (!device.isOn()) {
            device.setStatus(DeviceStatus.SCHEDULED);
            device.setRunMode(null);
            device.setActiveRunUntil(null);
        }

        persistDevice(device, effectiveReference);
    }

    public void skipNextScheduledRun(ScheduledDevice device) {
        device.setSkipNextRun(true);
        if (!device.isOn()) {
            device.setStatus(DeviceStatus.SKIPPED);
        }
        persistDevice(device, currentReferenceTime());
    }

    public void reschedule(ScheduledDevice device, LocalTime newTime) {
        device.setSchedule(newTime);
        device.setSkipNextRun(false);
        if (!device.isOn()) {
            device.setStatus(DeviceStatus.SCHEDULED);
        }
        persistDevice(device, currentReferenceTime());
    }

    public void deleteDevice(Device device) {
        if (device == null || device.getDeviceId() == null || device.getDeviceId().isBlank()) {
            return;
        }

        deviceRepository.softDelete(
                device.getDeviceId(),
                currentReferenceTime().atOffset(ZoneOffset.UTC).toInstant()
        );
        allDevices.removeIf(existing -> device.getDeviceId().equals(existing.getDeviceId()));
    }

    private void handleSimulationUpdate(com.energyapp.model.EnergyRecord energyRecord) {
        LocalDateTime currentTime = energyRecord.getTimestamp();
        double currentPrice = energyRecord.getPricePerKwh();

        for (Device device : allDevices) {
            boolean changed = false;

            if (device instanceof ScheduledDevice scheduledDevice) {
                changed |= evaluateScheduledDevice(scheduledDevice, currentTime);
            }

            if (device instanceof SmartPlug smartPlug) {
                changed |= evaluateCutoffRules(smartPlug, currentPrice, currentTime);
            }

            if (changed) {
                persistDevice(device, currentTime);
            }
        }
    }

    private boolean evaluateCutoffRules(SmartPlug smartPlug, double currentPrice, LocalDateTime currentTime) {
        if (smartPlug.isOn() && currentPrice > smartPlug.getPriceCutoff()) {
            smartPlug.setOn(false);
            smartPlug.setStatus(DeviceStatus.OFF);
            smartPlug.setActiveRunUntil(null);
            smartPlug.setRunMode(null);
            smartPlug.setLastRunEndedAt(currentTime);
            System.out.println(String.format("%s plug exceeded threshold, turning off...", smartPlug.getDeviceName()));
            return true;
        }
        return false;
    }

    private boolean evaluateScheduledDevice(ScheduledDevice device, LocalDateTime currentTime) {
        boolean changed = false;

        if (device.getActiveRunUntil() != null && !currentTime.isBefore(device.getActiveRunUntil())) {
            if (device.isOn() || device.getStatus() == DeviceStatus.RUNNING) {
                device.setOn(false);
                device.setLastRunEndedAt(device.getActiveRunUntil());
                changed = true;
            }
            device.setActiveRunUntil(null);
            device.setRunMode(null);
            device.setStatus(DeviceStatus.OFF);
            changed = true;
        }

        LocalTime schedule = device.getSchedule();
        if (schedule == null) {
            changed |= updateStatus(device, device.isOn() ? DeviceStatus.IDLE : DeviceStatus.OFF);
            return changed;
        }

        LocalDate currentDate = currentTime.toLocalDate();
        boolean alreadyHandledToday = currentDate.equals(device.getLastScheduledRunDate());
        LocalDateTime scheduledStart = currentDate.atTime(schedule);

        if (!alreadyHandledToday && device.isSkipNextRun() && !currentTime.isBefore(scheduledStart)) {
            device.setSkipNextRun(false);
            device.setLastScheduledRunDate(currentDate);
            device.setOn(false);
            device.setActiveRunUntil(null);
            device.setRunMode(null);
            device.setStatus(DeviceStatus.SKIPPED);
            changed = true;
            alreadyHandledToday = true;
        }

        if (!alreadyHandledToday && !currentTime.isBefore(scheduledStart) && !device.isOn()) {
            startRun(device, currentTime, DeviceRunMode.SCHEDULED, true);
            changed = true;
        } else {
            changed |= refreshDeviceStatus(device, currentTime);
        }

        return changed;
    }

    private boolean refreshDeviceStatus(Device device, LocalDateTime referenceTime) {
        DeviceStatus targetStatus;
        if (device.getActiveRunUntil() != null && device.isOn()) {
            targetStatus = DeviceStatus.RUNNING;
        } else if (device instanceof ScheduledDevice scheduledDevice) {
            targetStatus = deriveScheduledStatus(scheduledDevice, referenceTime);
        } else {
            targetStatus = device.isOn() ? DeviceStatus.IDLE : DeviceStatus.OFF;
        }
        return updateStatus(device, targetStatus);
    }

    private DeviceStatus deriveScheduledStatus(ScheduledDevice device, LocalDateTime referenceTime) {
        if (device.getActiveRunUntil() != null && device.isOn()) {
            return DeviceStatus.RUNNING;
        }
        if (device.getSchedule() == null) {
            return device.isOn() ? DeviceStatus.IDLE : DeviceStatus.OFF;
        }
        if (device.isSkipNextRun()) {
            return DeviceStatus.SKIPPED;
        }
        if (referenceTime != null && referenceTime.toLocalDate().equals(device.getLastScheduledRunDate())) {
            if (device.getStatus() == DeviceStatus.SKIPPED) {
                return DeviceStatus.SKIPPED;
            }
            return device.isOn() ? DeviceStatus.IDLE : DeviceStatus.OFF;
        }
        return DeviceStatus.SCHEDULED;
    }

    private void startRun(ScheduledDevice device, LocalDateTime startTime, DeviceRunMode runMode, boolean countForToday) {
        device.setOn(true);
        device.setStatus(DeviceStatus.RUNNING);
        device.setRunMode(runMode);
        device.setActiveRunUntil(startTime.plus(resolveRunDuration(device)));
        device.setLastRunStartedAt(startTime);
        device.setSkipNextRun(false);
        if (countForToday) {
            device.setLastScheduledRunDate(startTime.toLocalDate());
        }
    }

    private void persistDevice(Device device, LocalDateTime updatedAt) {
        ensureCanonicalIdentity(device);
        enforceNonInterruptibleState(device, updatedAt);
        refreshDeviceStatus(device, updatedAt);

        CanonicalDeviceRecord record = deviceRepository.findById(device.getDeviceId())
                .map(existing -> CanonicalDeviceMapper.merge(existing, device, updatedAt))
                .orElseGet(() -> CanonicalDeviceMapper.newRecord(device, homeId, updatedAt));

        if (deviceRepository.findById(device.getDeviceId()).isPresent()) {
            deviceRepository.update(record);
        } else {
            deviceRepository.insert(record);
        }
    }

    public String presentationStatus(Device device) {
        if (device == null) {
            return "OFF";
        }
        if (device.isOn()) {
            return "ON";
        }
        if (device instanceof ScheduledDevice scheduledDevice) {
            if (scheduledDevice.isSkipNextRun()) {
                return "SKIPPED";
            }
            if (scheduledDevice.getSchedule() != null) {
                return "SCHEDULED";
            }
        }
        return "OFF";
    }

    public boolean isPowerLocked(Device device) {
        return isNonInterruptibleRunning(device, currentReferenceTime());
    }

    private void enforceNonInterruptibleState(Device device, LocalDateTime referenceTime) {
        if (!isNonInterruptibleRunning(device, referenceTime)) {
            return;
        }

        device.setOn(true);
        if (device.getRunMode() == null) {
            device.setRunMode(DeviceRunMode.SCHEDULED);
        }
        if (device.getStatus() != DeviceStatus.RUNNING) {
            device.setStatus(DeviceStatus.RUNNING);
        }
    }

    private boolean isNonInterruptibleRunning(Device device, LocalDateTime referenceTime) {
        if (!(device instanceof WashingMachine)) {
            return false;
        }
        if (device.getActiveRunUntil() == null) {
            return false;
        }
        LocalDateTime effectiveReference = referenceTime == null ? currentReferenceTime() : referenceTime;
        return effectiveReference.isBefore(device.getActiveRunUntil());
    }

    private Duration resolveRunDuration(ScheduledDevice device) {
        if (device == null || device.getDeviceType() == null) {
            return Duration.ofHours(1);
        }
        return switch (device.getDeviceType()) {
            case WASHING_MACHINE, DISHWASHER -> Duration.ofHours(2);
            case EV_CHARGER -> Duration.ofHours(4);
            default -> Duration.ofHours(1);
        };
    }

    private boolean updateStatus(Device device, DeviceStatus targetStatus) {
        if (device.getStatus() != targetStatus) {
            device.setStatus(targetStatus);
            return true;
        }
        return false;
    }

    private void ensureCanonicalIdentity(Device device) {
        if (device.getDeviceId() == null || device.getDeviceId().isBlank()) {
            device.setDeviceId("DEV_USER_" + UUID.randomUUID().toString().replace("-", ""));
        }
    }

    private LocalDateTime currentReferenceTime() {
        if (simulationService.getCurrentEnergyRecord().get() != null) {
            return simulationService.getCurrentEnergyRecord().get().getTimestamp();
        }
        return LocalDateTime.now();
    }
}
