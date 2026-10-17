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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    // Regex to robustly parse (jdbc:)?mysql://[user[:pass]@]host[:port][/database][?query]
    private static final Pattern DB_URL_PATTERN = Pattern.compile(
            "^(?:jdbc:)?mysql://(?:([^:@/\\s]+)(?::([^@/\\s]+))?@)?([^:/\\s?#]+)(?::(\\d+))?(/[^?#\\s]*)?(?:\\?([^#\\s]*))?$",
            Pattern.CASE_INSENSITIVE
    );

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

        String host = "localhost";
        int port = 3306;
        String path = "/financemanager";
        String query = "";

        Matcher matcher = DB_URL_PATTERN.matcher(finalUrl);
        if (matcher.matches()) {
            String urlUser = matcher.group(1);
            String urlPass = matcher.group(2);
            String urlHost = matcher.group(3);
            String urlPort = matcher.group(4);
            String urlPath = matcher.group(5);
            String urlQuery = matcher.group(6);

            if (urlUser != null && !urlUser.isEmpty()) {
                finalUser = urlUser;
            }
            if (urlPass != null && !urlPass.isEmpty()) {
                finalPass = urlPass;
            }
            if (urlHost != null && !urlHost.isEmpty()) {
                host = urlHost;
            }
            if (urlPort != null && !urlPort.isEmpty()) {
                try {
                    port = Integer.parseInt(urlPort);
                } catch (NumberFormatException ignored) {}
            }
            if (urlPath != null && !urlPath.isEmpty() && !urlPath.equals("/")) {
                // If TiDB template pointed to system schema /sys, redirect to default user schema /test
                if (urlPath.equalsIgnoreCase("/sys")) {
                    path = "/test";
                } else {
                    path = urlPath;
                }
            } else {
                path = host.contains("tidbcloud.com") ? "/test" : "/defaultdb";
            }
            if (urlQuery != null && !urlQuery.isEmpty()) {
                query = urlQuery.replace("ssl-mode=", "sslMode=");
            }

            // Remote cloud host SSL configuration
            if (!host.equals("localhost") && !host.equals("127.0.0.1")) {
                if (query.isEmpty()) {
                    query = "sslMode=VERIFY_IDENTITY&allowPublicKeyRetrieval=true&autoReconnect=true";
                } else {
                    if (!query.contains("sslMode") && !query.contains("useSSL")) {
                        query += "&sslMode=VERIFY_IDENTITY";
                    }
                    if (!query.contains("allowPublicKeyRetrieval")) {
                        query += "&allowPublicKeyRetrieval=true";
                    }
                    if (!query.contains("autoReconnect")) {
                        query += "&autoReconnect=true";
                    }
                }
            }

            // Construct standard clean JDBC URL WITHOUT embedded credentials in the URL
            finalUrl = "jdbc:mysql://" + host + ":" + port + path + (query.isEmpty() ? "" : "?" + query);
        } else {
            // Fallback for non-standard format
            if (!finalUrl.startsWith("jdbc:")) {
                finalUrl = "jdbc:" + finalUrl;
            }
            finalUrl = finalUrl.replace("ssl-mode=", "sslMode=");
        }

        log.info("Connecting to Database at clean JDBC URL: {} with user: {}", finalUrl, finalUser);

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
}


