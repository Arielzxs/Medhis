package com.neusoft.his.common.audit;

import com.neusoft.his.common.api.PageResponse;

import java.util.List;

public interface AuditService {
    void log(String operation, String detail);

    List<AuditLogEntry> list();

    PageResponse<AuditLogEntry> page(long page, long size);

    void clear();
}
