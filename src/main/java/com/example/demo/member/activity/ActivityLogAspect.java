package com.example.demo.member.activity;

import java.time.LocalDate;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.example.demo.member.dao.MemberLogDao;
import com.example.demo.member.dto.MemberLog;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class ActivityLogAspect {
    private final MemberLogDao memberLogDao;

    public Object record(ProceedingJoinPoint pjp, ActivityLog activityLog) throws Throwable {
        Object result = pjp.proceed();

        Long memberId = extractMemberId(pjp.getArgs());

        if (memberId != null) {
            MemberLog dto = new MemberLog();
            dto.setMemberId(memberId);
            dto.setLogType(activityLog.value());
            dto.setLogDate(LocalDate.now());
            memberLogDao.insert(dto);
        }

        return result;
    }

    private Long extractMemberId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long l)
                return l;
        }
        return null;
    }
}
