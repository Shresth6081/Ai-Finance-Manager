package com.financemanager.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/financemanager?createDatabaseIfNotExist=true}")
    private String rawUrl;

    @Value("${spring.datasource.username:root}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        String finalUrl = rawUrl;
        String finalUser = username;
        String finalPass = password;

        // Auto-sanitize mysql:// cloud URIs (e.g. Aiven, Railway, Render) to jdbc:mysql:// format
        if (finalUrl != null && finalUrl.startsWith("mysql://")) {
            try {
                URI uri = URI.create(finalUrl);
                String host = uri.getHost();
                int port = uri.getPort() != -1 ? uri.getPort() : 3306;
                String path = uri.getPath() != null ? uri.getPath() : "/defaultdb";
                String query = uri.getQuery();

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    if (finalUser == null || finalUser.isEmpty() || finalUser.equals("root")) {
                        finalUser = userInfo[0];
                    }
                    if (userInfo.length > 1 && (finalPass == null || finalPass.isEmpty())) {
                        finalPass = userInfo[1];
                    }
                }

                finalUrl = "jdbc:mysql://" + host + ":" + port + path + (query != null ? "?" + query : "");
            } catch (Exception e) {
                finalUrl = "jdbc:" + finalUrl;
            }
        } else if (finalUrl != null && !finalUrl.startsWith("jdbc:")) {
            finalUrl = "jdbc:" + finalUrl;
        }

        System.out.println("Connecting to Database at URL: " + finalUrl + " with user: " + finalUser);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(finalUrl);
        config.setUsername(finalUser);
        config.setPassword(finalPass);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return new HikariDataSource(config);
    }
}
