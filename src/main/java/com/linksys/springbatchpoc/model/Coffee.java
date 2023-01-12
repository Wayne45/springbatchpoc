package com.linksys.springbatchpoc.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(setterPrefix = "with", builderMethodName = "newBuilder")
@NoArgsConstructor
@AllArgsConstructor
public class Coffee {

  private UUID externalId;
  private String brand;
  private String origin;
  private String characteristics;

}
