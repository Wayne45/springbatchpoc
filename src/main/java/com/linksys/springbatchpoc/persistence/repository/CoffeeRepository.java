package com.linksys.springbatchpoc.persistence.repository;

import com.linksys.springbatchpoc.persistence.entity.CoffeeEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface CoffeeRepository extends BaseRepository<CoffeeEntity> {

  @Query("select it.externalId from CoffeeEntity it")
  Page<UUID> findAllExternalIdsWithPagination(Pageable pageable);

}
