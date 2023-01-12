package com.linksys.springbatchpoc.processor;

import com.linksys.springbatchpoc.model.Coffee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

  private static final Logger logger = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

  private final JdbcTemplate jdbcTemplate;

  public JobCompletionNotificationListener(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void beforeJob(JobExecution jobExecution){
    super.beforeJob(jobExecution);

    logger.info("Job Started");
  }

  @Override
  public void afterJob(JobExecution jobExecution){
    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      logger.info("!!! JOB FINISHED! Time to verify the results");

      String query = "SELECT brand, origin, characteristics FROM coffee";
      jdbcTemplate.query(query, (rs, row) -> new Coffee(rs.getString(1), rs.getString(2), rs.getString(3)))
                  .forEach(coffee -> logger.info("Found < {} > in the database.", coffee));
    }
  }

}
