package com.linksys.springbatchpoc.processor;

import java.util.HashMap;
import java.util.Map;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

public class RangePartitioner implements Partitioner {

  private final Long minId;
  private final Long maxId;

  public RangePartitioner(Long minId, Long maxId) {
    this.minId = minId;
    this.maxId = maxId;
  }

  @Override
  public Map<String, ExecutionContext> partition(int gridSize) {

    Map<String, ExecutionContext> result = new HashMap<String, ExecutionContext>();

    float count = maxId - minId + 1;
    int range = Math.round(count / gridSize);
    long fromId = minId;
    long toId = fromId + range - 1;

    for (int i = 1; i <= gridSize; i++) {
      ExecutionContext value = new ExecutionContext();

      System.out.println("\nStarting : Thread" + i);
      System.out.println("fromId : " + fromId);
      System.out.println("toId : " + toId);
      System.out.println("count : " + range);

      value.putLong("fromId", fromId);
      value.putLong("toId", toId);

      // give each thread a name, thread 1,2,3
      value.putString("name", "Thread" + i);

      result.put("partition" + i, value);

      fromId = toId + 1;
      toId += range;
    }

    return result;
  }
}
