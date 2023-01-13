package com.linksys.springbatchpoc;

import com.linksys.springbatchpoc.persistence.entity.CoffeeEntity;
import com.linksys.springbatchpoc.persistence.repository.CoffeeRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@SpringBootApplication
@EnableBatchProcessing
public class SpringBatchPocApplication implements CommandLineRunner {

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  @Qualifier("importDataJob")
  private Job importDataJob;

  @Autowired
  @Qualifier("processDataJob")
  private Job processDataJob;

  @Autowired
  private CoffeeRepository coffeeRepository;

  public static void main(String[] args) {
    SpringApplication.run(SpringBatchPocApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    // Pass the required Job Parameters from here to read it anywhere within
    // Spring Batch infrastructure

    // Job1: Load all csv into DB
    long fileNumber = 1;
    long fileCount = 3;
    for (long i = fileNumber; i <= fileCount; i++) {
      try {
        JobParameters jobParameters = new JobParametersBuilder().addLong("fileNumber", i)
                                                                .toJobParameters();
        JobExecution execution = jobLauncher.run(importDataJob, jobParameters);
        System.out.println(
            String.format("fileNumber=[%d] JobInstance STATUS :: %s", i, execution.getStatus()));
      } catch (JobInstanceAlreadyCompleteException e) {
        System.out
            .println(
                String
                    .format("[ImportDataJob] JobInstance Already Completed !! fileNumber=[%d]", i));
      }
    }

    // Job2: Process data by page size
    long totalCount = coffeeRepository.count();
    int pageSize = 100;
    int page = 0;
    long totalPage = (totalCount / pageSize) + 1;
    Pageable pageable;
    while (page < totalPage) {
      pageable = PageRequest.of(page, pageSize);
      Page<CoffeeEntity> coffeePage = coffeeRepository.findAll(pageable);
      for (CoffeeEntity it : coffeePage) {
        try {
          JobParameters jobParameters = new JobParametersBuilder()
              .addString("externalId", it.getExternalId().toString())
              .toJobParameters();
          JobExecution execution = jobLauncher.run(processDataJob, jobParameters);
          System.out.println(
              String.format("JobInstance STATUS :: %s", execution.getStatus()));
        } catch (JobInstanceAlreadyCompleteException | JobExecutionAlreadyRunningException | JobRestartException | JobParametersInvalidException e) {
          System.out.println(
              String.format("[ProcessDataJob] JobInstance Already Completed !! externalId=[%s]",
                            it.getExternalId()));
        }
      }
      page++;
    }
  }
}
