package com.neusoft.his.common.audit;

import java.util.List;

public interface AuditService {
    void log(String operation, String detail);

    List<AuditLogEntry> list();

    void clear();
}
