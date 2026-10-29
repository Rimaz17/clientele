package dev.rimaz.clientele.customer;

import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerRowMapperTest {

    @Test
    void mapRow() throws SQLException {
        // Given
        CustomerRowMapper customerRowMapper = new CustomerRowMapper();
        Instant createdAt = Instant.parse("2026-03-14T09:26:53.589793Z");

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getInt("age")).thenReturn(19);
        when(resultSet.getString("name")).thenReturn("Jamila");
        when(resultSet.getString("email")).thenReturn("jamila@gmail.com");
        when(resultSet.getString("gender")).thenReturn("FEMALE");
        when(resultSet.getString("password")).thenReturn("password");
        when(resultSet.getString("profile_image_id")).thenReturn("22222");
        when(resultSet.getString("role")).thenReturn("ADMIN");
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(createdAt));


        // When
        Customer actual = customerRowMapper.mapRow(resultSet, 1);

        // Then
        Customer expected = new Customer(
                1,
                "Jamila",
                "jamila@gmail.com",
                "password",
                19,
                Gender.FEMALE,
                "22222",
                Role.ADMIN,
                createdAt
        );
        assertThat(actual).isEqualTo(expected);
    }
}