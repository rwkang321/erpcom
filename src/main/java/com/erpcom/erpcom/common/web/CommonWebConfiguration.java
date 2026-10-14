package com.erpcom.erpcom.common.web;

import jakarta.servlet.http.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import com.erpcom.erpcom.common.permission.PermissionService;

@Configuration
@EnableConfigurationProperties(CommonCorsProperties.class)
public class CommonWebConfiguration {
    @Bean
    WebMvcConfigurer commonCorsConfigurer(CommonCorsProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                if (!properties.allowedOrigin().isEmpty()) {
                    registry.addMapping("/api/common/**")
                            .allowedOrigins(properties.allowedOrigin())
                            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                            .allowedHeaders("*")
                            .allowCredentials(true)
                            .maxAge(3600);
                }
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(CurrentUserProvider.class)
    CurrentUserProvider unauthenticatedUserProvider() { return request -> null; }

    @Bean
    WebMvcConfigurer commonPermissionConfigurer(RequestUserService users, PermissionService permissions) {
        return new WebMvcConfigurer() {
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new HandlerInterceptor() {
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                        if (handler instanceof HandlerMethod method) {
                            var rule = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequiresPermission.class);
                            var classRule = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequiresPermission.class);
                            if (rule == null) rule = classRule;
                            if (rule != null) {
                                long userId = users.require(request).userId();
                                permissions.requireButton(userId, rule.menu(), rule.button(), rule.level());
                                var scopes = rule.scopes().length > 0 ? rule.scopes()
                                        : classRule == null ? new com.erpcom.erpcom.common.permission.DataScope[0]
                                          : classRule.scopes();
                                if (scopes.length > 0) permissions.requireScope(userId, rule.menu(), rule.level(), scopes);
                            }
                        }
                        return true;
                    }
                }).addPathPatterns("/api/common/**");
            }
        };
    }
    /**
     * @Bean
     * WebMvcConfigurer commonPermissionConfigurer(RequestUserService users, PermissionService permissions) {
     *     return new WebMvcConfigurer() {
     *         public void addInterceptors(InterceptorRegistry registry) {
     *             registry.addInterceptor(new HandlerInterceptor() {
     *                 public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
     *                     if (handler instanceof HandlerMethod method) {
     *                         var rule = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequiresPermission.class);
     *                         var classRule = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequiresPermission.class);
     *                         if (rule == null) rule = classRule;
     *                         if (rule != null) {
     *                             long userId = users.require(req).userId();
     *                             permissions.requireButton(userId, rule.menu(), rule.button(), rule.level());
     *                             var scopes = rule.scopes().length > 0 ? rule.scopes()
     *                                  : classRule == null ? new com.erpcom.erpcom.common.permission.DataScope[0] : classRule.scopes();
     *                             if (scopes.length > 0) permissions.requireScope(userId, rule.menu(), rule.level(), scopes);
     *                         }
     *                     }
     *                     return true;
     *                 }
     *             }).addPathPatterns("/api/common/**");
     *         }
     *     }
     * }
     */


}
