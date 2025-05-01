package com.tanay.vistora.service;

import com.tanay.vistora.metadata.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;


@Service
public class SchemaService
{
    private final DataSource dataSource;

    @Autowired
    public SchemaService(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public List<DatabaseTable> extractDatabaseSchema() throws SQLException
    {
        List<DatabaseTable> tables = new ArrayList<>();

        try (Connection connection = dataSource.getConnection())
        {
            DatabaseMetaData metaData = connection.getMetaData();

            // Get all tables
            try (ResultSet tableResultSet = metaData.getTables(null, null, null,
                    new String[]{"TABLE"}))
            {
                while (tableResultSet.next())
                {
                    DatabaseTable table = new DatabaseTable();

                    table.setName(tableResultSet.getString("TABLE_NAME"));
                    table.setCatalog(tableResultSet.getString("TABLE_CAT"));
                    table.setSchema(tableResultSet.getString("TABLE_SCHEM"));
                    table.setRemarks(tableResultSet.getString("REMARKS"));

                    table.setColumns(getColumns(metaData, table.getName()));
                    table.setPrimaryKeys(getPrimaryKeys(metaData, table.getName()));
                    table.setForeignKeys(getForeignKeys(metaData, table.getName()));
                    table.setIndices(getIndices(metaData, table.getName()));

                    tables.add(table);
                }
            }
        }
        return tables;
    }

    private List<DatabaseColumn> getColumns(DatabaseMetaData metaData, String tableName) throws SQLException
    {
        List<DatabaseColumn> columns = new ArrayList<>();

        try (ResultSet columnsResultSet = metaData.getColumns(null, null, tableName, null))
        {
            while (columnsResultSet.next())
            {
                DatabaseColumn column = new DatabaseColumn();

                column.setName(columnsResultSet.getString("COLUMN_NAME"));
                column.setType(columnsResultSet.getString("TYPE_NAME"));
                column.setSize(columnsResultSet.getInt("COLUMN_SIZE"));
                column.setDecimalDigits(columnsResultSet.getInt("DECIMAL_DIGITS"));
                column.setNullable("YES".equals(columnsResultSet.getString("IS_NULLABLE")));
                column.setAutoIncrement("YES".equals(columnsResultSet.getString("IS_AUTOINCREMENT")));
                column.setGenerated("YES".equals(columnsResultSet.getString("IS_GENERATEDCOLUMN")));
                column.setDefaultValue(columnsResultSet.getString("COLUMN_DEF"));
                column.setRemarks(columnsResultSet.getString("REMARKS"));
                column.setOrdinalPosition(columnsResultSet.getInt("ORDINAL_POSITION"));

                columns.add(column);
            }
        }
        return columns;
    }

    private List<String> getPrimaryKeys(DatabaseMetaData metaData, String tableName) throws SQLException
    {
        List<String> primaryKeys = new ArrayList<>();

        try (ResultSet primaryKeyResultSet = metaData.getPrimaryKeys(null, null, tableName))
        {
            while (primaryKeyResultSet.next())
            {
                primaryKeys.add(primaryKeyResultSet.getString("COLUMN_NAME"));
            }
        }
        return primaryKeys;
    }

    private List<DatabaseForeignKey> getForeignKeys(DatabaseMetaData metaData, String tableName) throws SQLException
    {
        List<DatabaseForeignKey> foreignKeys = new ArrayList<>();

        try (ResultSet foreignKeysResultSet = metaData.getImportedKeys(null, null, tableName))
        {
            while (foreignKeysResultSet.next())
            {
                DatabaseForeignKey foreignKey = new DatabaseForeignKey();

                foreignKey.setName(foreignKeysResultSet.getString("FK_NAME"));
                foreignKey.setColumnName(foreignKeysResultSet.getString("FKCOLUMN_NAME"));
                foreignKey.setForeignTableName(foreignKeysResultSet.getString("PKTABLE_NAME"));
                foreignKey.setForeignColumnName(foreignKeysResultSet.getString("PKCOLUMN_NAME"));
                foreignKey.setUpdateRule(foreignKeysResultSet.getInt("UPDATE_RULE"));
                foreignKey.setDeleteRule(foreignKeysResultSet.getInt("DELETE_RULE"));
                foreignKey.setDeferrability(foreignKeysResultSet.getInt("DEFERRABILITY"));
                foreignKey.setForeignKeyName(foreignKeysResultSet.getString("PK_NAME"));

                foreignKeys.add(foreignKey);
            }
        }
        return foreignKeys;
    }

    private List<DatabaseIndex> getIndices(DatabaseMetaData metaData, String tableName) throws SQLException
    {
        Map<String, DatabaseIndex> indexMap = new HashMap<>();

        // Define column name constants to avoid magic strings
        final String COL_INDEX_NAME = "INDEX_NAME";
        final String COL_NON_UNIQUE = "NON_UNIQUE";
        final String COL_TYPE = "TYPE";
        final String COL_FILTER_CONDITION = "FILTER_CONDITION";
        final String COL_COLUMN_NAME = "COLUMN_NAME";

        try (ResultSet indicesResultSet = metaData.getIndexInfo(null, null, tableName, false, true))
        {
            while (indicesResultSet.next())
            {
                // Safely get index name
                String indexName = indicesResultSet.getString(COL_INDEX_NAME);
                if (indexName == null)
                    continue;

                DatabaseIndex index = indexMap.computeIfAbsent(indexName, k ->
                {
                    DatabaseIndex newIndex = new DatabaseIndex();
                    newIndex.setName(indexName);

                    // Handle NON_UNIQUE safely
                    try
                    {
                        newIndex.setUnique(!indicesResultSet.getBoolean(COL_NON_UNIQUE));
                    }
                    catch (SQLException e)
                    {
                        newIndex.setUnique(false); // default if column not available
                    }

                    // Handle TYPE safely
                    try
                    {
                        short indexType = indicesResultSet.getShort(COL_TYPE);
                        newIndex.setClustered(indexType == DatabaseMetaData.tableIndexClustered);
                    }
                    catch (SQLException e)
                    {
                        newIndex.setClustered(false); // default if column not available
                    }

                    // Handle FILTER_CONDITION safely
                    String filterCondition = null;
                    try
                    {
                        filterCondition = indicesResultSet.getString(COL_FILTER_CONDITION);
                        newIndex.setFilterCondition(filterCondition);
                    }
                    catch (SQLException e)
                    {
                        newIndex.setFilterCondition("");
                    }
                    newIndex.setFilterCondition(filterCondition != null ? filterCondition : "");
                    newIndex.setColumns(new ArrayList<>());
                    return newIndex;
                });

                // Add column to index
                String columnName = indicesResultSet.getString(COL_COLUMN_NAME);
                if (columnName != null)
                {
                    index.getColumns().add(columnName);
                }
            }
        }
        return new ArrayList<>(indexMap.values());
    }
}
