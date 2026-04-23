package com.energyapp.service;

import com.energyapp.db.UiDeviceRepository;
import com.energyapp.model.Device;
import com.energyapp.model.DeviceType;
import com.energyapp.model.SmartPlug;

import java.util.ArrayList;
import java.util.List;

public class DeviceService {

    private static final String HOME_ID = "HOME_1";

    private List<Device> allDevices = new ArrayList<>();
    private final UiDeviceRepository uiDeviceRepository;

    public DeviceService(SimulationService service) {
        this.uiDeviceRepository = new UiDeviceRepository();
        this.allDevices = new ArrayList<>(uiDeviceRepository.fetchAll(HOME_ID));

        service.getCurrentEnergyRecord().addListener((observable, oldRec,
                                                      newRec) -> evaluateCutoffRules(newRec.getPricePerKwh()));
    }

    public List<Device> getAllDevices() {
        return this.allDevices;
    }

    private void evaluateCutoffRules(double currentPrice) {
        for (Device device : allDevices) {
            if (device.getDeviceType() == DeviceType.SMART_PLUG) {
                if (device.isOn() && currentPrice > ((SmartPlug) device).getPriceCutoff()) {
                    device.setOn(false);
                    System.out.println(String.format("%s plug exceeded threshold, turning off...", device.getDeviceName()));
                }
            }
        }
    }

    public void addDevice(Device device) {
        uiDeviceRepository.insert(HOME_ID, device);
        this.allDevices.add(device);
    }

    public void saveDevice(Device device) {
        uiDeviceRepository.upsert(HOME_ID, device);
    }

}
