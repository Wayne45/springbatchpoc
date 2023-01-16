package com.linksys.springbatchpoc.config;

import com.linksys.springbatchpoc.persistence.entity.CoffeeEntity;
import com.linksys.springbatchpoc.processor.CoffeeItemProcessor;
import com.linksys.springbatchpoc.processor.JobCompletionNotificationListener;
import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

@Configuration
public class Job2Configuration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  public Job2Configuration(
      JobBuilderFactory jobBuilderFactory,
      StepBuilderFactory stepBuilderFactory) {
    this.jobBuilderFactory = jobBuilderFactory;
    this.stepBuilderFactory = stepBuilderFactory;
  }

  @Bean
  public TaskExecutor taskExecutor() {
    return new SimpleAsyncTaskExecutor("spring_batch");
  }

  @Bean("dbReader")
  @StepScope
  public JdbcCursorItemReader<CoffeeEntity> dbReader(
    @Value("#{jobParameters['externalId']}") String externalId, DataSource dataSource) {
    System.out.println("dbReader externalId: " + externalId);
    String query = "SELECT * FROM coffee WHERE status = 'NEW' AND external_id='%s'".formatted(externalId);
    return new JdbcCursorItemReaderBuilder<CoffeeEntity>()
        .name("coffee_reader")
        .sql(query)
        //.preparedStatementSetter()
        .dataSource(dataSource)
        .maxItemCount(1)
        .rowMapper(new BeanPropertyRowMapper<>(CoffeeEntity.class))
        .build();
  }

  @Bean("dbStatusWriter")
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

  @Bean("processStep")
  @SuppressWarnings({"rawtypes"})
  public Step processStep(@Qualifier("dbReader") JdbcCursorItemReader<CoffeeEntity> dbReader,
                          @Qualifier("dbStatusWriter") JdbcBatchItemWriter<CoffeeEntity> dbStatusWriter) {
    return stepBuilderFactory.get("processStepJob")
                             .<CoffeeEntity, CoffeeEntity>chunk(10)
                             .reader(dbReader)
                             .processor(processor())
                             .writer(dbStatusWriter)
                             .taskExecutor(taskExecutor())
                             .throttleLimit(20)
                             .build();
  }

  @Bean
  public CoffeeItemProcessor processor() {
    return new CoffeeItemProcessor();
  }

  @Bean("processDataJob")
  public Job processData(JobCompletionNotificationListener listener,
                         @Qualifier("processStep") Step processStep) {
    return jobBuilderFactory.get("processDataJob")
                            .incrementer(new RunIdIncrementer())
                            .listener(listener)
                            .start(processStep)
                            .build();
  }
}
