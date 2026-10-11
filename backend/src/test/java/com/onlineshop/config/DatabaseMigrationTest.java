package com.onlineshop.config;

import com.onlineshop.mapper.SellerMapper;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据持久化（US-25）：表结构由 Flyway 自动创建，重复启动不出错，已有数据不丢失。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@SpringBootTest
@ActiveProfiles("test")
class DatabaseMigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private FlywayProperties flywayProperties;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SellerMapper sellerMapper;

    @Autowired
    private SellerAccountInitializer sellerAccountInitializer;

    @Test
    @DisplayName("启动时执行完全部迁移脚本")
    void allMigrationsApplied() {
        assertThat(Arrays.stream(flyway.info().applied()).map(MigrationInfo::getVersion).map(Object::toString))
                .contains("1", "2", "3");
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    @DisplayName("三张业务表和查询索引都已创建")
    void tablesAndIndexesCreated() throws SQLException {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (String table : new String[]{"seller", "product", "purchase_intent"}) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class)).isNotNull();
        }
        assertThat(indexNames(dataSource, "product")).contains("idx_product_status_time");
        assertThat(indexNames(dataSource, "purchase_intent")).contains("idx_intent_product_status");
    }

    @Test
    @DisplayName("再次启动不会重复执行脚本")
    void migrateAgainDoesNothing() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    @DisplayName("卖家表为空时创建初始账号，重复启动不会重复创建")
    void defaultSellerCreatedOnce() throws Exception {
        sellerAccountInitializer.run(null);
        sellerAccountInitializer.run(null);

        assertThat(sellerMapper.selectCount(null)).isEqualTo(1L);
        assertThat(sellerMapper.selectList(null).get(0).getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("已有表和数据、但没有 Flyway 记录的库：原有数据保留，并补上索引")
    void existingDatabaseKeepsData() throws SQLException {
        DataSource existing = h2("existing_db");
        new ResourceDatabasePopulator(new ClassPathResource("db/existing-database.sql")).execute(existing);

        migrateWithAppSettings(existing);

        JdbcTemplate jdbc = new JdbcTemplate(existing);
        assertThat(jdbc.queryForObject("SELECT password FROM seller WHERE username = 'admin'", String.class))
                .isEqualTo("123456");
        assertThat(jdbc.queryForObject("SELECT name FROM product", String.class)).isEqualTo("已有商品");
        assertThat(indexNames(existing, "product")).contains("idx_product_status_time");
    }

    @Test
    @DisplayName("库里只有无关的表时，照常建出全部业务表")
    void databaseWithUnrelatedTableIsInitialized() {
        DataSource other = h2("other_db");
        new JdbcTemplate(other).execute("CREATE TABLE app_config (config_key VARCHAR(50) PRIMARY KEY)");

        migrateWithAppSettings(other);

        JdbcTemplate jdbc = new JdbcTemplate(other);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM purchase_intent", Long.class)).isZero();
    }

    /** 用 application.yml 里的 Flyway 配置迁移另一个库 */
    private void migrateWithAppSettings(DataSource target) {
        Flyway.configure()
                .dataSource(target)
                .locations(flywayProperties.getLocations().toArray(String[]::new))
                .baselineOnMigrate(flywayProperties.isBaselineOnMigrate())
                .baselineVersion(flywayProperties.getBaselineVersion())
                .load()
                .migrate();
    }

    private static DataSource h2(String name) {
        String url = "jdbc:h2:mem:" + name + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";
        return new SimpleDriverDataSource(new org.h2.Driver(), url, "sa", "");
    }

    private static Set<String> indexNames(DataSource dataSource, String table) throws SQLException {
        Set<String> names = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             ResultSet rs = connection.getMetaData().getIndexInfo(null, null, table, false, false)) {
            while (rs.next()) {
                String name = rs.getString("INDEX_NAME");
                if (name != null) {
                    names.add(name.toLowerCase());
                }
            }
        }
        return names;
    }
}
