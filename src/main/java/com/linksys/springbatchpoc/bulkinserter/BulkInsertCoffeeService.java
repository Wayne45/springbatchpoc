package com.linksys.springbatchpoc.bulkinserter;

import com.linksys.springbatchpoc.persistence.entity.CoffeeEntity;
import de.bytefish.pgbulkinsert.PgBulkInsert;
import de.bytefish.pgbulkinsert.util.PostgreSqlUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BulkInsertCoffeeService {

  private final JdbcTemplate jdbcTemplate;

  public BulkInsertCoffeeService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void generate(int size) throws SQLException {
    List<CoffeeEntity> coffeeList = getCoffeeList(size);

    PgBulkInsert<CoffeeEntity> bulkInsert = new PgBulkInsert<>(new CoffeeMapping());
    // Now save all entities of a given stream:
    bulkInsert
        .saveAll(PostgreSqlUtils.getPGConnection(jdbcTemplate.getDataSource().getConnection()),
                 coffeeList.stream());
  }

  private List<CoffeeEntity> getCoffeeList(int num) {
    List<CoffeeEntity> list = new ArrayList<>();

    for (int pos = 1; pos <= num; pos++) {
      list.add(CoffeeEntity.newBuilder()
                           .withId(Integer.toUnsignedLong(pos))
                           .withExternalId(UUID.randomUUID())
                           .withBrand("brandData")
                           .withOrigin("originData")
                           .withCharacteristics("characteristicsData")
                           .withStatus("NEW")
                           .build());
    }

    return list;
  }
}
