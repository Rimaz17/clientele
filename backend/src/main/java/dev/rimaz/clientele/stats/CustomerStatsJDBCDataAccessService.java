package dev.rimaz.clientele.stats;

import dev.rimaz.clientele.customer.Gender;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Repository
public class CustomerStatsJDBCDataAccessService implements CustomerStatsDao {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public CustomerStatsJDBCDataAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public CustomerTotals selectTotals(Instant newSince) {
        var sql = """
                SELECT count(*) AS total,
                       count(*) FILTER (WHERE created_at >= :newSince) AS new_since,
                       round(avg(age), 1) AS average_age
                FROM customer
                """;
        var parameters = new MapSqlParameterSource("newSince", utc(newSince));
        return namedParameterJdbcTemplate.queryForObject(sql, parameters, (rs, rowNum) -> {
            BigDecimal averageAge = rs.getBigDecimal("average_age");
            return new CustomerTotals(
                    rs.getLong("total"),
                    rs.getLong("new_since"),
                    averageAge == null ? null : averageAge.doubleValue()
            );
        });
    }

    @Override
    public Map<Gender, Long> countByGender() {
        var sql = """
                SELECT gender, count(*) AS count
                FROM customer
                GROUP BY gender
                """;
        Map<Gender, Long> counts = new EnumMap<>(Gender.class);
        jdbcTemplate.query(sql, rs -> {
            counts.put(Gender.valueOf(rs.getString("gender")), rs.getLong("count"));
        });
        return counts;
    }

    // Keep the labels in sync with AgeRange. Ages below 16 only exist in rows
    // created before the API validated ages, and count in the youngest bucket.
    @Override
    public Map<AgeRange, Long> countByAgeRange() {
        var sql = """
                SELECT CASE
                           WHEN age < 25 THEN '16-24'
                           WHEN age < 35 THEN '25-34'
                           WHEN age < 45 THEN '35-44'
                           WHEN age < 55 THEN '45-54'
                           WHEN age < 65 THEN '55-64'
                           ELSE '65+'
                       END AS age_range,
                       count(*) AS count
                FROM customer
                GROUP BY age_range
                """;
        Map<AgeRange, Long> counts = new EnumMap<>(AgeRange.class);
        jdbcTemplate.query(sql, rs -> {
            counts.put(AgeRange.fromLabel(rs.getString("age_range")), rs.getLong("count"));
        });
        return counts;
    }

    // created_at is converted to UTC before truncating, so the month does not
    // depend on the database session's time zone.
    @Override
    public Map<YearMonth, Long> countSignupsByMonth(Instant from) {
        var sql = """
                SELECT to_char(date_trunc('month', created_at AT TIME ZONE 'UTC'), 'YYYY-MM') AS month,
                       count(*) AS count
                FROM customer
                WHERE created_at >= :from
                GROUP BY month
                """;
        var parameters = new MapSqlParameterSource("from", utc(from));
        Map<YearMonth, Long> counts = new HashMap<>();
        namedParameterJdbcTemplate.query(sql, parameters, rs -> {
            counts.put(YearMonth.parse(rs.getString("month")), rs.getLong("count"));
        });
        return counts;
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
