package com.tanay.dbcrawler.metadata;

import lombok.Data;

@Data
public class DatabaseColumn
{
    private String name;
    private String type;
    private int size;
    private int decimalDigits;
    private boolean nullable;
    private boolean autoIncrement;
    private boolean generated;
    private String defaultValue;
    private String remarks;
    private int ordinalPosition;
}