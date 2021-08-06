/**
 * 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.upscale.externalapplication;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class RestConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("https://<store-locator-custom-component-location>.com");
        // config.addAllowedOrigin("http://localhost:4200");    // localhost custom component
        config.addAllowedHeader("*");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("GET");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}