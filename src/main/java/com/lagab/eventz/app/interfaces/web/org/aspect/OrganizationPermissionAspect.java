package com.lagab.eventz.app.interfaces.web.org.aspect;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.lagab.eventz.app.domain.org.model.OrganizationPermission;
import com.lagab.eventz.app.domain.org.service.OrganizationSecurityService;
import com.lagab.eventz.app.interfaces.web.org.annotation.RequireOrganizationPermission;
import com.lagab.eventz.app.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class OrganizationPermissionAspect {

    private final OrganizationSecurityService securityService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequireOrganizationPermission requirePermission) throws Throwable {

        Long userId = SecurityUtils.getCurrentUserId();

        // Evaluate the SpEL expression for organizationId
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = getParameterNames(joinPoint);
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        Expression expression = parser.parseExpression(requirePermission.organizationId());
        String organizationId = expression.getValue(context, String.class);

        // Check the permission
        OrganizationPermission permission = OrganizationPermission.valueOf(requirePermission.permission());

        if (!securityService.hasPermission(userId, organizationId, permission)) {
            throw new AccessDeniedException("Insufficient permissions for organization: " + organizationId);
        }

        return joinPoint.proceed();
    }

    private String[] getParameterNames(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return parameterNameDiscoverer.getParameterNames(method);
    }
}
