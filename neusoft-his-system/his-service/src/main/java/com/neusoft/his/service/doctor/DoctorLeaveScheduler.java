package com.neusoft.his.service.doctor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DoctorLeaveScheduler {
    private final DoctorWorkstationService doctorService;

    public DoctorLeaveScheduler(DoctorWorkstationService doctorService) {
        this.doctorService = doctorService;
    }

    @Scheduled(fixedDelay = 60_000L, initialDelay = 5_000L)
    public void refreshDoctorLeaveStatuses() {
        doctorService.refreshAllDoctorLeaveStatuses();
    }
}
