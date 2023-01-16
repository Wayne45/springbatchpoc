package com.linksys.springbatchpoc.config;

import com.linksys.springbatchpoc.persistence.entity.CoffeeEntity;
import com.linksys.springbatchpoc.processor.CoffeeItemProcessor;
import com.linksys.springbatchpoc.processor.JobCompletionNotificationListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

  @Autowired
  public JobBuilderFactory jobBuilderFactory;

  @Autowired
  public StepBuilderFactory stepBuilderFactory;

  private String getFileName(String filename, Long fileNumber) {
    return String.format("%s_%d.csv", filename, fileNumber);
  }

  @Bean
  public TaskExecutor taskExecutor() {
    return new SimpleAsyncTaskExecutor("spring_batch");
  }

  /**
   * @Value("#{jobParameters}") Map jobParameters: can be used for load all job parameters.
   * includedFields: can load specified csv header fields.
   */
  @Bean("fileReader")
  @StepScope
  @SuppressWarnings({"rawtypes", "unchecked"})
  public FlatFileItemReader<CoffeeEntity> fileReader(
    @Value("#{jobParameters['fileNumber']}") Long fileNumber,
    @Value("${file.input}") String fileInput) {

    String filename = getFileName(fileInput, fileNumber);
    System.out.println("filename: " + filename);

    return new FlatFileItemReaderBuilder().name("coffeeItemReader")
                                          .resource(new ClassPathResource(filename))
                                          .delimited()
                                          .names("external_id", "brand", "origin",
                                                 "characteristics")
                                          .fieldSetMapper(new BeanWrapperFieldSetMapper() {{
                                            setTargetType(CoffeeEntity.class);
                                          }})
                                          .build();
  }

  @Bean("dbWriter")
  public JdbcBatchItemWriter<CoffeeEntity> writer(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<CoffeeEntity>()
      .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
      .sql(
        "INSERT INTO coffee (external_id, brand, origin, characteristics, status) VALUES (:externalId, :brand, :origin, :characteristics, 'NEW')")
      .dataSource(dataSource)
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

  @Bean("dbReader")
  @StepScope
  public JdbcCursorItemReader<CoffeeEntity> dbReader(
    @Value("#{jobParameters['externalId']}") String externalId, DataSource dataSource) {
    System.out.println("dbReader externalId: " + externalId);
    //String query =
    //    "SELECT * FROM coffee WHERE external_id = '" + externalId + "' AND status = 'NEW'";
    String query = "SELECT * FROM coffee WHERE status = 'NEW' AND external_id='%s'".formatted(externalId);
    return new JdbcCursorItemReaderBuilder<CoffeeEntity>()
      .name("coffee_reader")
      .sql(query)
      .dataSource(dataSource)
      .maxItemCount(1)
      .rowMapper(new BeanPropertyRowMapper<>(CoffeeEntity.class))
      .build();
  }

  @Bean("importDataJob")
  public Job importData(JobCompletionNotificationListener listener,
                        @Qualifier("importStep") Step importStep) {
    return jobBuilderFactory.get("importDataJob")
                            .incrementer(new RunIdIncrementer())
                            .listener(listener)
                            .start(importStep)
                            .build();
  }

  @Bean("importStep")
  @SuppressWarnings({"rawtypes", "unchecked"})
  public Step importStep(@Qualifier("dbWriter") JdbcBatchItemWriter writer,
                         @Qualifier("fileReader") FlatFileItemReader reader) {
    return stepBuilderFactory.get("importStep")
                             .<CoffeeEntity, CoffeeEntity>chunk(10)
                             .reader(reader)
                             .writer(writer)
                             .faultTolerant()
                             .skipLimit(10)
                             .skip(DuplicateKeyException.class)
                             .build();
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

}
