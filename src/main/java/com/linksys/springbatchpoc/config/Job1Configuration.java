package com.linksys.springbatchpoc.config;

import com.linksys.springbatchpoc.persistence.entity.CoffeeEntity;
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
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;

@Configuration
public class Job1Configuration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  public Job1Configuration(
      JobBuilderFactory jobBuilderFactory,
      StepBuilderFactory stepBuilderFactory) {
    this.jobBuilderFactory = jobBuilderFactory;
    this.stepBuilderFactory = stepBuilderFactory;
  }

  private String getFileName(String filename, Long fileNumber) {
    return String.format("%s_%d.csv", filename, fileNumber);
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

  @Bean("importDataJob")
  public Job importData(JobCompletionNotificationListener listener,
                        @Qualifier("importStep") Step importStep) {
    return jobBuilderFactory.get("importDataJob")
                            .incrementer(new RunIdIncrementer())
                            .listener(listener)
                            .start(importStep)
                            .build();
  }

}
