package com.github.taoroot.cloud.auth.config;

import com.github.taoroot.cloud.auth.social.SocialAuthenticationProvider;
import com.github.taoroot.cloud.auth.social.SocialDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.client.RestTemplate;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private SocialDetailsService socialDetailsService;

    @Qualifier("authUserService")
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * 注入 Spring 容器中, 在授权服务器中密码模式下,验证用户密码正确性, 以及第三方授权码验证
     */
    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(new SocialAuthenticationProvider(socialDetailsService));
        auth.userDetailsService(userDetailsService);
    }

    /**
     * 配置 HttpSecurity
     */
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
                .csrf().disable()
                .formLogin()
                .loginPage("/login").and()
                .authorizeRequests()
                .antMatchers("/login").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic();
        // @formatter:on
    }

    @Bean
    @Primary
    @LoadBalanced
    public RestTemplate lbRestTemplate() {
        return new RestTemplate();
    }
}