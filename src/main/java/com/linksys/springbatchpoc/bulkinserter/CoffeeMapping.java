package com.linksys.springbatchpoc.bulkinserter;

import com.linksys.springbatchpoc.persistence.entity.CoffeeEntity;
import de.bytefish.pgbulkinsert.mapping.AbstractMapping;

public class CoffeeMapping extends AbstractMapping<CoffeeEntity> {

  protected CoffeeMapping() {
    super("public", "coffee");
    mapLong("id", CoffeeEntity::getId);
    mapUUID("external_id", CoffeeEntity::getExternalId);
    mapText("brand", CoffeeEntity::getBrand);
    mapText("origin", CoffeeEntity::getOrigin);
    mapText("characteristics", CoffeeEntity::getCharacteristics);
    mapText("status", CoffeeEntity::getStatus);
  }
}
