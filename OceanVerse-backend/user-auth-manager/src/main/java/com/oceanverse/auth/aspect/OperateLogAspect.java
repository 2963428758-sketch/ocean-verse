package com.oceanverse.auth.aspect;

import com.oceanverse.auth.context.UserContext;
import com.oceanverse.auth.mapper.OperateLogMapper;
import com.oceanverse.common.annotation.OperateLog;
import com.oceanverse.pojo.entity.SysOperateLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperateLogAspect {

    private final OperateLogMapper operateLogMapper;
    private final HttpServletRequest request;

    @Around("@annotation(operateLog)")
    public Object around(ProceedingJoinPoint pjp, OperateLog operateLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        String errorMessage = null;

        try {
            result = pjp.proceed();
            return result;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            saveLog(pjp, operateLog, elapsed, errorMessage);
        }
    }

    private void saveLog(ProceedingJoinPoint pjp, OperateLog operateLog, long elapsed, String errorMessage) {
        try {
            SysOperateLog logEntry = new SysOperateLog();
            logEntry.setModule(operateLog.module());
            logEntry.setOperationType(operateLog.type().name());
            logEntry.setRequestUrl(request.getRequestURI());
            logEntry.setRequestMethod(request.getMethod());

            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Object[] args = pjp.getArgs();
            if (args != null && args.length > 0) {
                String[] paramNames = signature.getParameterNames();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof HttpServletRequest) continue;
                    if (i > 0) sb.append(", ");
                    sb.append(paramNames[i]).append("=").append(args[i]);
                }
                String params = sb.toString();
                logEntry.setRequestParams(params.length() > 500 ? params.substring(0, 500) + "..." : params);
            }

            UserContext.UserInfo userInfo = UserContext.get();
            if (userInfo != null) {
                logEntry.setOperatorId(userInfo.getUserId());
                logEntry.setOperatorName(userInfo.getUsername());
            }

            logEntry.setIpAddress(getClientIp(request));
            logEntry.setExecutionTime((int) elapsed);
            logEntry.setOperationResult(errorMessage == null ? 1 : 0);
            logEntry.setErrorMessage(errorMessage);
            logEntry.setCreateTime(LocalDateTime.now());

            operateLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("操作日志记录失败: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
