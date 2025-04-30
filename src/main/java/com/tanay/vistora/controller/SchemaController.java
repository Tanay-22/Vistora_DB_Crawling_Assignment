package com.tanay.vistora.controller;

import com.tanay.vistora.model.DatabaseTable;
import com.tanay.vistora.service.ModelGeneratorService;
import com.tanay.vistora.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schema")
public class SchemaController
{
    private final SchemaService schemaService;
    private final ModelGeneratorService modelGeneratorService;

    @Autowired
    public SchemaController(SchemaService schemaService, ModelGeneratorService modelGeneratorService)
    {
        this.schemaService = schemaService;
        this.modelGeneratorService = modelGeneratorService;
    }

    @GetMapping("/tables")
    public List<DatabaseTable> getAllTables() throws SQLException
    {
        return schemaService.extractDatabaseSchema();
    }

    @GetMapping("/tables/{tableName}")
    public DatabaseTable getTable(@PathVariable String tableName) throws SQLException
    {
        return schemaService.extractDatabaseSchema().stream()
                .filter(table -> table.getName().equalsIgnoreCase(tableName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableName));
    }

    @GetMapping("/generate-models")
    public Map<String, String> generateAllModels() throws SQLException, IOException
    {
        List<DatabaseTable> tables = schemaService.extractDatabaseSchema();
        return modelGeneratorService.generateAllModels(tables);
    }

    @GetMapping("/generate-model/{tableName}")
    public String generateModel(@PathVariable String tableName) throws SQLException
    {
        DatabaseTable table = getTable(tableName);
        return modelGeneratorService.generateModelClass(table);
    }
}
