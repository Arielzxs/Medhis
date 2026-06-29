package com.neusoft.his.common.audit;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.dal.entity.SysAuditLog;
import com.neusoft.his.dal.mapper.SysAuditLogMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DbAuditServicePaginationTest {

    @Test
    void page_should_return_page_response_instead_of_full_list() {
        SysAuditLogMapper mapper = mock(SysAuditLogMapper.class);
        DbAuditServiceImpl service = new DbAuditServiceImpl(mapper);

        SysAuditLog log = new SysAuditLog();
        log.setId(1L);
        log.setUsername("admin");
        log.setOperation("LOGIN");
        log.setDetail("登录系统");
        log.setTime(LocalDateTime.of(2026, 6, 28, 10, 0));
        when(mapper.selectAuditPage(any(Page.class))).thenReturn(List.of(log));
        when(mapper.countAuditPage()).thenReturn(1L);

        PageResponse<AuditLogEntry> response = service.page(1, 10);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.records()).hasSize(1);
        assertThat(response.records().get(0).username()).isEqualTo("admin");
        verify(mapper).selectAuditPage(any(Page.class));
        verify(mapper).countAuditPage();
    }
}
