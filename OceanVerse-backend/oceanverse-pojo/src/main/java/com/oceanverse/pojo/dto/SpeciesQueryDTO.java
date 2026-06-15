package com.oceanverse.pojo.dto;

import lombok.Data;

@Data
public class SpeciesQueryDTO {
    private String keyword;
    private String family;
    private String iucnStatus;
    private String protectionLevel;
    private Integer page = 1;
    private Integer size = 10;
}
