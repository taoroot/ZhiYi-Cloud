package com.github.taoroot.cloud.common.security.log;

import com.github.taoroot.cloud.common.core.vo.LogInfo;
import com.github.taoroot.cloud.common.security.tenant.TenantContextHolder;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;

@AllArgsConstructor
public class LogListener {

    private final LogSaveService logSaveService;

    @Async
    @Order
    @EventListener(LogInfo.class)
    public void save(LogInfo event) {
        if (logSaveService != null) {
            TenantContextHolder.set(event.getTenantId());
            logSaveService.save(event);
        }
    }
}
