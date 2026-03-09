package com.revpay.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final IdempotencyInterceptor idempotencyInterceptor;

    // REMOVED addCorsMappings entirely. SecurityConfig handles it now!

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // FIXED: Added /v1/ to match your API structure
        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns(
                        "/api/v1/wallet/send",
                        "/api/v1/wallet/add-funds",
                        "/api/v1/wallet/withdraw",
                        "/api/v1/wallet/pay-invoice"
                );
    }
}