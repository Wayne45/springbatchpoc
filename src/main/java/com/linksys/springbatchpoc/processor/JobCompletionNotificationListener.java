package com.linksys.springbatchpoc.processor;

import com.linksys.springbatchpoc.persistence.entity.CoffeeEntity;
import java.util.UUID;
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
      String externalId = jobExecution.getJobParameters().getString("externalId");
      if (externalId != null) {
        String query =
            "SELECT id, external_id, brand, origin, characteristics, status FROM coffee WHERE external_id ='"
                + externalId + "'";
        jdbcTemplate.query(query, (rs, row) -> CoffeeEntity.newBuilder()
                                                           .withId(rs.getLong(1))
                                                           .withExternalId(
                                                               UUID.fromString(rs.getString(2)))
                                                           .withBrand(rs.getString(3))
                                                           .withOrigin(rs.getString(4))
                                                           .withCharacteristics(rs.getString(5))
                                                           .withStatus(rs.getString(6))
                                                           .build())
                    .forEach(coffee -> {
                      if (coffee.getStatus() == null) {
                        logger.error("Found < {} > in the database but status is null", coffee);
                      } else {
                        logger.info("Found < {} > in the database.", coffee);
                      }
                    });
      }
    }
  }

}
