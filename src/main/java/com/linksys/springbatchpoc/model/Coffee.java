package com.linksys.springbatchpoc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(setterPrefix = "with", builderMethodName = "newBuilder")
@NoArgsConstructor
@AllArgsConstructor
public class Coffee {

  private String brand;
  private String origin;
  private String characteristics;

}
