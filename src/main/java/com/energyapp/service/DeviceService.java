package com.energyapp.service;

import com.energyapp.model.Device;
import com.energyapp.model.DeviceType;
import com.energyapp.model.SmartPlug;

import java.util.ArrayList;
import java.util.List;

public class DeviceService {

    private List<Device> allDevices = new ArrayList<>();

    public DeviceService(SimulationService service) {
        // instantiate a few dummy plugs for testing
        SmartPlug plug1 = new SmartPlug("Living Room", DeviceType.SMART_PLUG);
        SmartPlug plug2 = new SmartPlug("Kitchen", DeviceType.SMART_PLUG);
        SmartPlug plug3 = new SmartPlug("Bedroom", DeviceType.SMART_PLUG);
        allDevices.add(plug1); allDevices.add(plug2); allDevices.add(plug3);

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
        this.allDevices.add(device);
    }

}
