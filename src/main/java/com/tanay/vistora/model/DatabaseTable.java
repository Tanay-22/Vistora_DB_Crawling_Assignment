package com.tanay.vistora.model;

import lombok.Data;

import java.util.List;

@Data
public class DatabaseTable
{
    private String name;
    private String catalog;
    private String schema;
    private String remarks;
    private List<DatabaseColumn> columns;
    private List<String> primaryKeys;
    private List<DatabaseForeignKey> foreignKeys;
    private List<DatabaseIndex> indices;
}
