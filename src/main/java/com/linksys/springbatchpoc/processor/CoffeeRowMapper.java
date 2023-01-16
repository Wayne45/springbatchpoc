package com.linksys.springbatchpoc.processor;

import com.linksys.springbatchpoc.persistence.entity.CoffeeEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public class CoffeeRowMapper implements RowMapper<CoffeeEntity> {

  @Override
  public CoffeeEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
    return CoffeeEntity.newBuilder()
                       .withId(rs.getLong("id"))
                       .withExternalId(UUID.fromString(rs.getString("external_id")))
                       .withBrand(rs.getString("brand"))
                       .withOrigin(rs.getString("origin"))
                       .withCharacteristics(rs.getString("characteristics"))
                       .withStatus(rs.getString("status"))
                       .build();
  }
}
