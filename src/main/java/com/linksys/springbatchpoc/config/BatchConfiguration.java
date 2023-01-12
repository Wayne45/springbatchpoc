package com.linksys.springbatchpoc.config;

import com.linksys.springbatchpoc.model.Coffee;
import com.linksys.springbatchpoc.processor.CoffeeItemProcessor;
import com.linksys.springbatchpoc.processor.JobCompletionNotificationListener;
import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;

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

  /**
   * @Value("#{jobParameters}") Map jobParameters: can be used for load all job parameters.
   * includedFields: can load specified csv header fields.
   */
  @Bean
  @StepScope
  public FlatFileItemReader<Coffee> reader(@Value("#{jobParameters['fileNumber']}") Long fileNumber,
                                           @Value("${file.input}") String fileInput) {

    String filename = getFileName(fileInput, fileNumber);
    System.out.println("filename: " + filename);

    return new FlatFileItemReaderBuilder().name("coffeeItemReader")
                                          .resource(new ClassPathResource(filename))
                                          .delimited()
                                          .names(new String[] { "external_id", "brand", "origin", "characteristics" })
                                          .fieldSetMapper(new BeanWrapperFieldSetMapper() {{
                                            setTargetType(Coffee.class);
                                          }})
                                          .build();
  }

  @Bean
  public JdbcBatchItemWriter<Coffee> writer(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<Coffee>().itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                                                   .sql("INSERT INTO coffee (external_id, brand, origin, characteristics) VALUES (:externalId, :brand, :origin, :characteristics)")
                                                   .dataSource(dataSource)
                                                   .build();
  }

  @Bean
  public Job importUserJob(JobCompletionNotificationListener listener, Step step1) {
    return jobBuilderFactory.get("importUserJob")
                            .incrementer(new RunIdIncrementer())
                            .listener(listener)
                            .flow(step1)
                            .end()
                            .build();
  }

  @Bean
  public Step step1(JdbcBatchItemWriter writer, FlatFileItemReader reader) {
    return stepBuilderFactory.get("step1")
        .<Coffee, Coffee> chunk(10)
        .reader(reader)
        .processor(processor())
        .writer(writer)
        .faultTolerant()
        .skipLimit(10)
        .skip(DuplicateKeyException.class)
        .build();
  }

  @Bean
  public CoffeeItemProcessor processor() {
    return new CoffeeItemProcessor();
  }

}
