package com.linksys.springbatchpoc.persistence.entity;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Data
@Builder(setterPrefix = "with", builderMethodName = "newBuilder")
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coffee")
public class CoffeeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "coffee_id_seq")
  @SequenceGenerator(name = "coffee_id_seq", sequenceName = "coffee_id_seq", allocationSize = 1)
  @Column(name = "id", columnDefinition = "bigserial")
  private Long id;

  @Column(name = "external_id")
  private UUID externalId;

  @Column(name = "brand")
  private String brand;

  @Column(name = "origin")
  private String origin;

  @Column(name = "characteristics")
  private String characteristics;

  @Column(name = "status")
  private String status;

}
