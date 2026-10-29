package dev.rimaz.clientele.customer;

import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository("jdbc")
public class CustomerJDBCDataAccessService implements CustomerDao {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final CustomerRowMapper customerRowMapper;

    public CustomerJDBCDataAccessService(JdbcTemplate jdbcTemplate,
                                         CustomerRowMapper customerRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.customerRowMapper = customerRowMapper;
    }

    @Override
    public PageResponse<Customer> selectCustomers(CustomerQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String where = whereClause(query, parameters);

        Long total = namedParameterJdbcTemplate.queryForObject(
                "SELECT count(*) FROM customer" + where, parameters, Long.class);

        parameters.addValue("limit", query.size());
        parameters.addValue("offset", (long) query.page() * query.size());
        var sql = """
                SELECT id, name, email, password, age, gender, profile_image_id, role, created_at
                FROM customer
                """ + where + orderByClause(query) + " LIMIT :limit OFFSET :offset";
        List<Customer> content = namedParameterJdbcTemplate.query(sql, parameters, customerRowMapper);

        return PageResponse.of(content, query.page(), query.size(), total == null ? 0 : total);
    }

    // User input only ever reaches the SQL as named parameters.
    private static String whereClause(CustomerQuery query, MapSqlParameterSource parameters) {
        List<String> conditions = new ArrayList<>();
        if (query.search() != null) {
            conditions.add("(name ILIKE :search ESCAPE '\\' OR email ILIKE :search ESCAPE '\\')");
            parameters.addValue("search", LikePattern.contains(query.search()));
        }
        if (query.gender() != null) {
            conditions.add("gender = :gender");
            parameters.addValue("gender", query.gender().name());
        }
        if (query.minAge() != null) {
            conditions.add("age >= :minAge");
            parameters.addValue("minAge", query.minAge());
        }
        if (query.maxAge() != null) {
            conditions.add("age <= :maxAge");
            parameters.addValue("maxAge", query.maxAge());
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    // The column comes from the CustomerSortField whitelist, never from the
    // request. The id tie-breaker keeps pages stable when sort values repeat.
    private static String orderByClause(CustomerQuery query) {
        CustomerSortField field = query.sortField();
        String direction = query.sortDirection() == Sort.Direction.DESC ? "DESC" : "ASC";
        String orderBy = " ORDER BY " + field.column() + " " + direction;
        return field == CustomerSortField.ID ? orderBy : orderBy + ", id ASC";
    }

    @Override
    public Optional<Customer> selectCustomerById(Integer id) {
        var sql = """
                SELECT id, name, email, password, age, gender, profile_image_id, role, created_at
                FROM customer
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, customerRowMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public void insertCustomer(Customer customer) {
        var sql = """
                INSERT INTO customer(name, email, password, age, gender, role)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        int result = jdbcTemplate.update(
                sql,
                customer.getName(),
                customer.getEmail(),
                customer.getPassword(),
                customer.getAge(),
                customer.getGender().name(),
                customer.getRole().name()
        );

        System.out.println("insertCustomer result " + result);
    }

    @Override
    public boolean existsCustomerWithEmail(String email) {
        var sql = """
                SELECT count(id)
                FROM customer
                WHERE email = ?
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    @Override
    public boolean existsCustomerById(Integer id) {
        var sql = """
                SELECT count(id)
                FROM customer
                WHERE id = ?
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public void deleteCustomerById(Integer customerId) {
        var sql = """
                DELETE
                FROM customer
                WHERE id = ?
                """;
        int result = jdbcTemplate.update(sql, customerId);
        System.out.println("deleteCustomerById result = " + result);
    }

    @Override
    public void updateCustomer(Customer update) {
        if (update.getName() != null) {
            String sql = "UPDATE customer SET name = ? WHERE id = ?";
            int result = jdbcTemplate.update(
                    sql,
                    update.getName(),
                    update.getId()
            );
            System.out.println("update customer name result = " + result);
        }
        if (update.getAge() != null) {
            String sql = "UPDATE customer SET age = ? WHERE id = ?";
            int result = jdbcTemplate.update(
                    sql,
                    update.getAge(),
                    update.getId()
            );
            System.out.println("update customer age result = " + result);
        }
        if (update.getEmail() != null) {
            String sql = "UPDATE customer SET email = ? WHERE id = ?";
            int result = jdbcTemplate.update(
                    sql,
                    update.getEmail(),
                    update.getId());
            System.out.println("update customer email result = " + result);
        }
    }

    @Override
    public Optional<Customer> selectUserByEmail(String email) {
        var sql = """
                SELECT id, name, email, password, age, gender, profile_image_id, role, created_at
                FROM customer
                WHERE email = ?
                """;
        return jdbcTemplate.query(sql, customerRowMapper, email)
                .stream()
                .findFirst();
    }

    @Override
    public void updateCustomerProfileImageId(String profileImageId,
                                             Integer customerId) {
        var sql = """
                UPDATE customer
                SET profile_image_id = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, profileImageId, customerId);
    }
}
