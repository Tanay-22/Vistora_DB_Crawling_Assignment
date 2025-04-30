package com.tanay.vistora.controller;

import com.tanay.vistora.model.DatabaseTable;
import com.tanay.vistora.service.ModelGeneratorService;
import com.tanay.vistora.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<List<DatabaseTable>> getAllTables() throws SQLException
    {
        List<DatabaseTable> tables = schemaService.extractDatabaseSchema();
        return new ResponseEntity<>(tables, HttpStatus.OK);
    }

    @GetMapping("/tables/{tableName}")
    public ResponseEntity<DatabaseTable> getTableByName(@PathVariable String tableName) throws SQLException
    {
        DatabaseTable table = getTable(tableName);

        return new ResponseEntity<>(table, HttpStatus.OK);
    }

    @GetMapping("/generate-models")
    public ResponseEntity<Map<String, String>> generateAllModels() throws SQLException, IOException
    {
        List<DatabaseTable> tables = schemaService.extractDatabaseSchema();
        Map<String, String> models = modelGeneratorService.generateAllModels(tables);

        return new ResponseEntity<>(models, HttpStatus.OK);
    }

    @GetMapping("/generate-model/{tableName}")
    public ResponseEntity<String> generateModel(@PathVariable String tableName) throws SQLException
    {
        DatabaseTable table = getTable(tableName);
        String s = modelGeneratorService.generateModelClass(table);

        return new ResponseEntity<>(s, HttpStatus.OK);
    }

    private DatabaseTable getTable(String tableName) throws SQLException
    {
        return schemaService.extractDatabaseSchema().stream()
                .filter(t -> t.getName().equalsIgnoreCase(tableName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableName));
    }
}
