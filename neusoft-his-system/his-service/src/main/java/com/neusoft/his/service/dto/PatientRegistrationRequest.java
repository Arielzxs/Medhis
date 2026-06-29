package com.neusoft.his.service.dto;

import java.math.BigDecimal;

public record PatientRegistrationRequest(Long patientId, Long doctorId, String department, String scheduleDate, String shift, BigDecimal fee) {
}
