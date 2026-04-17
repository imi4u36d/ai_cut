package com.jiandou.api.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据库SchemaInitializer。
 */
@Configuration
public class DatabaseSchemaInitializer {

    /**
     * 处理数据库SchemaRunner。
     * @param dataSource data来源值
     * @return 处理结果
     */
    @Bean("databaseSchemaReady")
    public InitializingBean databaseSchemaRunner(DataSource dataSource) {
        return () -> initializeSchema(dataSource);
    }

    /**
     * 处理initializeSchema。
     * @param dataSource data来源值
     */
    private void initializeSchema(DataSource dataSource) throws SQLException, IOException {
        String script;
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("schema.sql")) {
            if (inputStream == null) {
                return;
            }
            script = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String rawSql : script.split(";")) {
                String sql = rawSql.trim();
                if (!sql.isEmpty()) {
                    try {
                        statement.execute(sql);
                    } catch (SQLException ex) {
                        if (!isIgnorableSchemaError(ex)) {
                            throw ex;
                        }
                    }
                }
            }
            ensureCompatibleUrlColumns(connection);
        }
    }

    /**
     * 检查是否IgnorableSchemaError。
     * @param ex ex值
     * @return 是否满足条件
     */
    private boolean isIgnorableSchemaError(SQLException ex) {
        return ex.getErrorCode() == 1061 || ex.getErrorCode() == 1060 || ex.getErrorCode() == 1091;
    }

    /**
     * 处理ensureCompatibleURLColumns。
     * @param connection connection值
     */
    private void ensureCompatibleUrlColumns(Connection connection) throws SQLException {
        ensureTextColumn(connection, "biz_task_results", "preview_path");
        ensureTextColumn(connection, "biz_task_results", "download_path");
        ensureTextColumn(connection, "biz_task_results", "remote_url");
        ensureTextColumn(connection, "biz_material_assets", "local_storage_path");
        ensureTextColumn(connection, "biz_material_assets", "local_file_path");
        ensureTextColumn(connection, "biz_material_assets", "public_url");
        ensureTextColumn(connection, "biz_material_assets", "third_party_url");
        ensureTextColumn(connection, "biz_material_assets", "remote_url");
    }

    /**
     * 处理ensure文本Column。
     * @param connection connection值
     * @param tableName tableName值
     * @param columnName columnName值
     */
    private void ensureTextColumn(Connection connection, String tableName, String columnName) throws SQLException {
        ColumnInfo column = loadColumnInfo(connection, tableName, columnName);
        if (column == null || column.isWideText()) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE `" + tableName + "` MODIFY COLUMN `" + columnName + "` TEXT NOT NULL");
        }
    }

    /**
     * 加载Column信息。
     * @param connection connection值
     * @param tableName tableName值
     * @param columnName columnName值
     * @return 处理结果
     */
    private ColumnInfo loadColumnInfo(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            if (!resultSet.next()) {
                return null;
            }
            return new ColumnInfo(
                resultSet.getString("TYPE_NAME"),
                resultSet.getInt("COLUMN_SIZE")
            );
        }
    }

    /**
     * 处理Column信息。
     * @param typeName 类型Name值
     * @param size size值
     * @return 处理结果
     */
    private record ColumnInfo(String typeName, int size) {
        /**
         * 检查是否Wide文本。
         * @return 是否满足条件
         */
        boolean isWideText() {
            String normalized = typeName == null ? "" : typeName.trim().toUpperCase();
            if ("TEXT".equals(normalized) || "MEDIUMTEXT".equals(normalized) || "LONGTEXT".equals(normalized)
                || "LONGVARCHAR".equals(normalized) || "CLOB".equals(normalized)) {
                return true;
            }
            return size >= 4096;
        }
    }
}
