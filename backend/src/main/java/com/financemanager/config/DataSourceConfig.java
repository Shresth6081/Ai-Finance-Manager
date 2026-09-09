package com.financemanager.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/financemanager?createDatabaseIfNotExist=true}")
    private String rawUrl;

    @Value("${spring.datasource.username:root}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        String finalUrl = rawUrl != null ? rawUrl.trim() : "";
        String finalUser = username != null ? username.trim() : "";
        String finalPass = password != null ? password : "";

        // Auto-sanitize mysql:// cloud URIs (e.g. Aiven, TiDB, Railway, Supabase) to jdbc:mysql:// format
        try {
            String uriStringToParse = finalUrl;
            if (uriStringToParse.startsWith("jdbc:")) {
                uriStringToParse = uriStringToParse.substring(5);
            }

            if (uriStringToParse.startsWith("mysql://")) {
                URI uri = URI.create(uriStringToParse);
                String host = uri.getHost();
                int port = uri.getPort() != -1 ? uri.getPort() : 3306;
                String path = (uri.getPath() != null && !uri.getPath().isEmpty() && !uri.getPath().equals("/"))
                        ? uri.getPath()
                        : "/defaultdb";
                String query = uri.getQuery();

                // Extract credentials from URI if provided in the URL
                if (uri.getUserInfo() != null && !uri.getUserInfo().isEmpty()) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    if (finalUser.isEmpty() || finalUser.equals("root")) {
                        finalUser = userInfo[0];
                    }
                    if (userInfo.length > 1 && finalPass.isEmpty()) {
                        finalPass = userInfo[1];
                    }
                }

                // Convert CLI param ssl-mode= to JDBC param sslMode=
                if (query != null) {
                    query = query.replace("ssl-mode=", "sslMode=");
                }

                // Enforce SSL & public key retrieval for remote cloud hosts
                if (host != null && !host.equals("localhost") && !host.equals("127.0.0.1")) {
                    if (query == null || query.isEmpty()) {
                        query = "sslMode=REQUIRED&allowPublicKeyRetrieval=true&autoReconnect=true";
                    } else if (!query.contains("sslMode") && !query.contains("useSSL")) {
                        query = query + "&sslMode=REQUIRED&allowPublicKeyRetrieval=true&autoReconnect=true";
                    } else if (!query.contains("allowPublicKeyRetrieval")) {
                        query = query + "&allowPublicKeyRetrieval=true";
                    }
                }

                finalUrl = "jdbc:mysql://" + host + ":" + port + path + (query != null && !query.isEmpty() ? "?" + query : "");
            } else {
                if (!finalUrl.startsWith("jdbc:")) {
                    finalUrl = "jdbc:" + finalUrl;
                }
                // Convert CLI param ssl-mode= to JDBC param sslMode=
                finalUrl = finalUrl.replace("ssl-mode=", "sslMode=");
                if (!finalUrl.contains("localhost") && !finalUrl.contains("127.0.0.1")) {
                    if (!finalUrl.contains("allowPublicKeyRetrieval")) {
                        finalUrl = finalUrl + (finalUrl.contains("?") ? "&" : "?") + "allowPublicKeyRetrieval=true";
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse datasource URL as structured URI ({}), falling back to direct JDBC string", e.getMessage());
            if (!finalUrl.startsWith("jdbc:")) {
                finalUrl = "jdbc:" + finalUrl;
            }
            finalUrl = finalUrl.replace("ssl-mode=", "sslMode=");
        }

        log.info("Connecting to Database at URL: {} with user: {}", sanitizeUrlForLogging(finalUrl), finalUser);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(finalUrl);
        config.setUsername(finalUser);
        config.setPassword(finalPass);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Optimal pooling parameters for cloud deployments
        config.setConnectionTimeout(30000);   // 30 seconds
        config.setValidationTimeout(5000);     // 5 seconds
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(600000);         // 10 minutes
        config.setMaxLifetime(1800000);        // 30 minutes
        config.setLeakDetectionThreshold(60000);

        return new HikariDataSource(config);
    }

    private String sanitizeUrlForLogging(String url) {
        if (url == null) return "";
        return url.replaceAll("(?i)(password|pass)=[^&]*", "$1=***");
    }
}

