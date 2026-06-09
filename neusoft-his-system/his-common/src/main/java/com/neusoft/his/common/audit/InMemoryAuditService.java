package com.neusoft.his.common.audit;

import com.neusoft.his.common.security.SecurityUser;
import com.neusoft.his.common.security.SecurityUserHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryAuditService implements AuditService {
    private final List<AuditLogEntry> logs = new CopyOnWriteArrayList<>();

    @Override
    public void log(String operation, String detail) {
        SecurityUser user = SecurityUserHolder.get();
        logs.add(new AuditLogEntry(LocalDateTime.now(), user == null ? "anonymous" : user.username(), operation, detail));
    }

    @Override
    public List<AuditLogEntry> list() {
        return new ArrayList<>(logs);
    }
}
