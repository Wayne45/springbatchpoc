package com.linksys.springbatchpoc;

import com.linksys.springbatchpoc.persistence.repository.CoffeeRepository;
import java.util.UUID;
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
  @Qualifier("partitionerJob")
  private Job partitionerJob;

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
//                                                                .addString("randomId",
//                                                                           UUID.randomUUID()
//                                                                               .toString()
//                                                                               .toUpperCase())
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
//    long totalCount = coffeeRepository.count();
//    int pageSize = 100;
//    int page = 0;
//    long totalPage = (totalCount / pageSize) + 1;
//    Pageable pageable;
//    while (page < totalPage) {
//      pageable = PageRequest.of(page, pageSize);
//      Page<UUID> coffeeExtIds = coffeeRepository.findAllExternalIdsWithPagination(pageable);
//      for (UUID id : coffeeExtIds.getContent()) {
//        try {
//          JobParameters jobParameters = new JobParametersBuilder()
//              .addString("externalId", id.toString())
//              //.addString("randomId", UUID.randomUUID().toString().toUpperCase())
//              .addLong("totalCount", totalCount)
//              .toJobParameters();
//          JobExecution execution = jobLauncher.run(processDataJob, jobParameters);
//          System.out.println(
//            String.format("JobInstance STATUS :: %s", execution.getStatus()));
//        } catch (JobInstanceAlreadyCompleteException | JobExecutionAlreadyRunningException | JobRestartException |
//                 JobParametersInvalidException e) {
//          System.out.println(
//              String
//                  .format("[ProcessDataJob] JobInstance Already Completed !! externalId=[%s]", id));
//        }
//      }
//      page++;
//    }

    // Job3
    try {
      JobParameters jobParameters = new JobParametersBuilder()
          //.addString("externalId", id.toString())
          .addString("randomId", UUID.randomUUID().toString().toUpperCase())
          .toJobParameters();
      JobExecution execution = jobLauncher.run(partitionerJob, jobParameters);
      System.out.println(
          String.format("JobInstance STATUS :: %s", execution.getStatus()));
    } catch (JobInstanceAlreadyCompleteException | JobExecutionAlreadyRunningException | JobRestartException |
        JobParametersInvalidException e) {
      System.out.println("[ProcessDataJob] JobInstance Already Completed !!");
    }
  }
}
