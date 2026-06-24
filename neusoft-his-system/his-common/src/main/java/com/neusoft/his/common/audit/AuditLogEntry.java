package com.neusoft.his.common.audit;

import java.time.LocalDateTime;

public record AuditLogEntry(LocalDateTime time, String username, String operation, String detail) {
}
