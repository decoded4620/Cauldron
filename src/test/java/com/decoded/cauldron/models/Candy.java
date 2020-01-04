package com.decoded.cauldron.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


@JsonSerialize
@JsonIgnoreProperties(ignoreUnknown = true)
public class Candy {
  @JsonProperty
  public String id = "";

  @JsonProperty
  public String name = "Candy";

  @JsonProperty
  public String[] ingredients = {"butter", "sugar", "heat"};
}