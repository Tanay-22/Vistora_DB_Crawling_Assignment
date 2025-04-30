package com.tanay.vistora.model;

import lombok.Data;

@Data
public class DatabaseForeignKey
{
    private String name;
    private String columnName;
    private String foreignTableName;
    private String foreignColumnName;
    private int updateRule;
    private int deleteRule;
    private int deferrability;
    private String foreignKeyName;
}