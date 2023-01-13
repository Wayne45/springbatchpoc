package com.linksys.springbatchpoc.processor;

import com.linksys.springbatchpoc.persistence.entity.CoffeeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

public class CoffeeItemProcessor implements ItemProcessor<CoffeeEntity, CoffeeEntity> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CoffeeItemProcessor.class);

  @Override
  public CoffeeEntity process(final CoffeeEntity coffee) throws Exception {

    LOGGER.info("==== Processing ( {} ) ====", coffee);

    return coffee;
  }
}
