package com.neusoft.his.service.dto;

import java.time.LocalDateTime;

public record DoctorLeaveApplyRequest(LocalDateTime startTime, LocalDateTime endTime, String reason) {
}
