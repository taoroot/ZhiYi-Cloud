package com.github.taoroot.cloud.common.security.log;

import cn.hutool.extra.servlet.ServletUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.taoroot.cloud.common.core.utils.R;
import com.github.taoroot.cloud.common.core.vo.AuthUserInfo;
import com.github.taoroot.cloud.common.core.vo.LogInfo;
import com.github.taoroot.cloud.common.security.SecurityUtils;
import com.github.taoroot.cloud.common.security.annotation.Log;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Objects;

@Aspect
public class LogAspect {

    private final ApplicationEventPublisher publisher;

    private static final Logger logger = LogManager.getLogger(LogAspect.class);

    public LogAspect(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @SneakyThrows
    @Around("@annotation(log)")
    public Object around(ProceedingJoinPoint point, Log log) {
        String strClassName = point.getTarget().getClass().getName();
        String strMethodName = point.getSignature().getName();
        logger.debug("[类名]:{},[方法]:{}", strClassName, strMethodName);

        long startTime = System.currentTimeMillis();
        Object obj;
        try {
            obj = point.proceed();
            handleLog(point, log, startTime, null, obj);
        } catch (Exception e) {
            handleLog(point, log, startTime, e, null);
            throw e;
        }
        return obj;
    }

    protected void handleLog(JoinPoint joinPoint, Log logAnnotation, long startTime, Exception e, Object jsonResult) {
        ObjectMapper objectMapper = new ObjectMapper();
        HttpServletRequest request = ((ServletRequestAttributes) Objects
                .requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        try {
            LogInfo logInfo = new LogInfo();
            logInfo.setStatus(R.OK);
            logInfo.setIp(ServletUtil.getClientIP(request));
            logInfo.setResult(objectMapper.writeValueAsString(jsonResult));
            logInfo.setUrl(request.getRequestURL().toString());
            logInfo.setTime(System.currentTimeMillis() - startTime);
            if (jsonResult instanceof AuthUserInfo && SecurityUtils.userId() == -1) {
                logInfo.setUserId(Integer.valueOf(((AuthUserInfo) jsonResult).getUsername()));
            } else {
                logInfo.setUserId(SecurityUtils.userId());
            }

            if (e != null) {
                logInfo.setStatus(R.ERROR);
                logInfo.setError(e.getMessage());
            }

            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            logInfo.setMethod(className + "." + methodName + "()");
            logInfo.setRequestMethod(request.getMethod());

            logInfo.setBusinessType(logAnnotation.businessType());
            logInfo.setTitle(logAnnotation.value());
            logInfo.setOperatorType(logAnnotation.operatorType());
            if (logAnnotation.isSaveRequestData()) {
                setRequestValue(joinPoint, logInfo);
            }
            publisher.publishEvent(logInfo);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }


    private void setRequestValue(JoinPoint joinPoint, LogInfo logInfo) throws Exception {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }

        StringBuilder params = new StringBuilder();
        Arrays.stream(args).filter(this::isFilterObject).forEach(o -> {
            try {
                params.append(o.toString()).append(" ");
            } catch (Exception e) {
                // do nothing
            }
        });
        logInfo.setParam(params.toString());
    }

    public boolean isFilterObject(final Object o) {
        return !(o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse);
    }
}
