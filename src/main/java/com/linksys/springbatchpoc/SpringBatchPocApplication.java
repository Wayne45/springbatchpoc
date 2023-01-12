package com.linksys.springbatchpoc;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchProcessing
public class SpringBatchPocApplication implements CommandLineRunner {

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  private Job job;

  public static void main(String[] args) {
    SpringApplication.run(SpringBatchPocApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    // Pass the required Job Parameters from here to read it anywhere within
    // Spring Batch infrastructure
    long fileNumber = 1;
    long fileCount = 3;
    for (long i = fileNumber; i <= fileCount; i++) {
      try {
        JobParameters jobParameters = new JobParametersBuilder().addLong("fileNumber", i)
                                                                .toJobParameters();
        JobExecution execution = jobLauncher.run(job, jobParameters);
        System.out.println(
            String.format("fileNumber=[%d] JobInstance STATUS :: %s", i, execution.getStatus()));
      } catch (JobInstanceAlreadyCompleteException e) {
        System.out
            .println(
                String.format("JobInstance Already Completed !! fileNumber=[%d]", i));
      }
    }
  }
}
