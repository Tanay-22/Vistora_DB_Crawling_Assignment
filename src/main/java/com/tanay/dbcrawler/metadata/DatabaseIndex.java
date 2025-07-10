package com.tanay.dbcrawler.metadata;

import lombok.Data;

import java.util.List;

@Data
public class DatabaseIndex
{
    private String name;
    private boolean unique;
    private boolean clustered;
    private List<String> columns;
    private String filterCondition;
    private int ordinalPosition;
}