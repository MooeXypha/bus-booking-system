package com.xypha.onlineBus.config;

import com.xypha.onlineBus.account.users.mapper.UserMapper;
import com.xypha.onlineBus.account.users.service.UserService;
import com.xypha.onlineBus.auth.filter.JwtAuthFilter;
import com.xypha.onlineBus.auth.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {



    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService){
        this.jwtService = jwtService;
    }


    @Bean
    public JwtAuthFilter jwtAuthFilter(UserDetailsService userDetailsService){
        return new JwtAuthFilter(userDetailsService, jwtService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity, UserService userService) throws Exception {
        return httpSecurity.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userService)
                .passwordEncoder(passwordEncoder())
                .and()
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,JwtAuthFilter jwtAuthFilter) throws Exception{
        httpSecurity
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        //Public endpoints
                        //Normal user endpoints
                        .requestMatchers("/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/reset-tokens").permitAll()


                        //Normal user endpoints
                        .requestMatchers("/api/auth/me").authenticated()
                        //ADMIN scope(can view users)
                        .requestMatchers(HttpMethod.GET,"/api/users/**").hasAnyRole("ADMIN","SUPER_ADMIN")

                        //Super Admin endpoints (full control)
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE,"/api/users/**").hasRole("SUPER_ADMIN")

                        .anyRequest().authenticated()
                ).addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }

    @Bean
        public WebMvcConfigurer corsConfigurer(){
        return new WebMvcConfigurer(){
            @Override
            public void addCorsMappings(CorsRegistry registry){
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000")
                        .allowedMethods("GET","POST","PUT","DELETE")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
        }

}
