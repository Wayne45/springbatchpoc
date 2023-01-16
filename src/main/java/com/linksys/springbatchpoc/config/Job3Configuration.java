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
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.PostgresPagingQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
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

  @Bean("taskExecutor3")
  public TaskExecutor taskExecutor3() {
    return new SimpleAsyncTaskExecutor("spring_batch_job3");
  }

  @Bean
  public RangePartitioner rangePartitioner() {
    return new RangePartitioner();
  }

  @Bean("partitionerJob")
  public Job partitionerJob(@Qualifier("masterStep") Step masterStep) {
    return jobBuilderFactory.get("partitionerJob")
                            .start(masterStep)
                            .build();
  }

  @Bean("masterStep")
  @SuppressWarnings({"rawtypes"})
  public Step masterStep(@Qualifier("slaveStep") Step slaveStep,
                         @Qualifier("taskExecutor3") TaskExecutor taskExecutor) {
    return stepBuilderFactory.get("masterStep")
                             .partitioner("slaveStep", rangePartitioner())
                             .step(slaveStep)
                             .taskExecutor(taskExecutor)
                             .gridSize(10)
                             .build();
  }

  @Bean("slaveStep")
  public Step slaveStep(CoffeeItemProcessor processor,
                        @Qualifier("pagingItemReader") JdbcPagingItemReader<CoffeeEntity> pagingItemReader,
                        @Qualifier("dbStatusWriter3") JdbcBatchItemWriter<CoffeeEntity> dbStatusWriter) {
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
    queryProvider.setWhereClause("WHERE id >= :fromId AND id <= :toId");
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
      @Value("#{stepExecutionContext[fromId]}") Integer fromId,
      @Value("#{stepExecutionContext[toId]}") Integer toId,
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
        .pageSize(10)
        .build();
  }

  @Bean("dbStatusWriter3")
  @StepScope
  public JdbcBatchItemWriter<CoffeeEntity> dbStatusWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<CoffeeEntity>()
        .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
        .itemPreparedStatementSetter((item, ps) -> {
          ps.setString(1, "SENT");
          ps.setObject(2, item.getExternalId());
        })
        .sql("UPDATE coffee SET status = ? WHERE external_id = ?")
        .dataSource(dataSource)
        .build();
  }

}
