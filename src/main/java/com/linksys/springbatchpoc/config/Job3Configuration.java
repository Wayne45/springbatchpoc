package com.linksys.springbatchpoc.config;

import com.linksys.springbatchpoc.persistence.entity.CoffeeEntity;
import com.linksys.springbatchpoc.processor.CoffeeItemProcessor;
import com.linksys.springbatchpoc.processor.CoffeeRowMapper;
import com.linksys.springbatchpoc.processor.RangePartitioner;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.PostgresPagingQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class Job3Configuration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  public Job3Configuration(
      JobBuilderFactory jobBuilderFactory,
      StepBuilderFactory stepBuilderFactory) {
    this.jobBuilderFactory = jobBuilderFactory;
    this.stepBuilderFactory = stepBuilderFactory;
  }

  @Bean("partitionerJob")
  public Job partitionerJob(@Qualifier("masterStep") Step masterStep) {
    return jobBuilderFactory.get("partitionerJob")
                            .start(masterStep)
                            .build();
  }

  @Bean("masterStep")
  @SuppressWarnings({"rawtypes"})
  @JobScope
  public Step masterStep(@Value("#{jobParameters['minId']}") Long minId,
                         @Value("#{jobParameters['maxId']}") Long maxId,
                         @Value("#{jobParameters['threadSize']}") Integer threadSize,
                         @Qualifier("slaveStep") Step slaveStep,
                         TaskExecutor taskExecutor) {

    RangePartitioner rangePartitioner = new RangePartitioner(minId, maxId);

    return stepBuilderFactory.get("masterStep")
                             .partitioner("slaveStep", rangePartitioner)
                             .step(slaveStep)
                             .taskExecutor(taskExecutor)
                             .gridSize(threadSize)
                             .build();
  }

  @Bean("slaveStep")
  public Step slaveStep(CoffeeItemProcessor processor,
                        @Qualifier("pagingItemReader") JdbcPagingItemReader<CoffeeEntity> pagingItemReader,
                        @Qualifier("dbStatusWriter") JdbcBatchItemWriter<CoffeeEntity> dbStatusWriter) {
    return stepBuilderFactory.get("slaveStep")
        .<CoffeeEntity, CoffeeEntity>chunk(1)
        .reader(pagingItemReader)
        .processor(processor)
        .writer(dbStatusWriter)
        .build();
  }

  @Bean
  public PostgresPagingQueryProvider queryProvider() {
    Map<String, Order> sortKeys = new HashMap<>();
    sortKeys.put("id", Order.ASCENDING);

    PostgresPagingQueryProvider queryProvider = new PostgresPagingQueryProvider();
    queryProvider.setSelectClause("SELECT *");
    queryProvider.setFromClause("FROM coffee");
    queryProvider.setWhereClause("WHERE id >= :fromId AND id <= :toId AND status = 'NEW'");
    queryProvider.setSortKeys(sortKeys);

    return queryProvider;
  }

  @Bean
  public CoffeeRowMapper rowMapper() {
    return new CoffeeRowMapper();
  }

  @Bean("pagingItemReader")
  @StepScope
  public JdbcPagingItemReader<CoffeeEntity> pagingItemReader(
      @Value("#{stepExecutionContext[fromId]}") Long fromId,
      @Value("#{stepExecutionContext[toId]}") Long toId,
      DataSource dataSource) {
    Map<String, Object> parameterValues = new HashMap<>();
    parameterValues.put("fromId", fromId);
    parameterValues.put("toId", toId);

    return new JdbcPagingItemReaderBuilder<CoffeeEntity>()
        .name("pagingItemReader")
        .dataSource(dataSource)
        .queryProvider(queryProvider())
        .parameterValues(parameterValues)
        .rowMapper(rowMapper())
        .pageSize(10) // set to thread size ?
        .build();
  }

}
