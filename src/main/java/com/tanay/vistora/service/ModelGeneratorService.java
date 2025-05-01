package com.tanay.vistora.service;

import com.squareup.javapoet.*;
import com.tanay.vistora.metadata.DatabaseColumn;
import com.tanay.vistora.metadata.DatabaseTable;
import org.springframework.stereotype.Service;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class ModelGeneratorService
{
    private static final Map<String, TypeName> TYPE_MAPPING = new HashMap<>();
    private static final Map<String, ClassName> JPA_ANNOTATIONS = new HashMap<>();
    private static final String PERSISTENCE = "jakarta.persistence";
    private static final String DEFAULT_PACKAGE = "com.tanay.vistora.generated";
    private static final String DEFAULT_OUTPUT_ROOT = "src/main/java/com/tanay/vistora/generated";

    static
    {
        // Initialize type mappings
        TYPE_MAPPING.put("VARCHAR", ClassName.get(String.class));
        TYPE_MAPPING.put("CHAR", ClassName.get(String.class));
        TYPE_MAPPING.put("TEXT", ClassName.get(String.class));
        TYPE_MAPPING.put("INT", ClassName.get(Integer.class));
        TYPE_MAPPING.put("BIGINT", ClassName.get(Long.class));
        TYPE_MAPPING.put("TINYINT", ClassName.get(Integer.class));
        TYPE_MAPPING.put("SMALLINT", ClassName.get(Integer.class));
        TYPE_MAPPING.put("MEDIUMINT", ClassName.get(Integer.class));
        TYPE_MAPPING.put("DECIMAL", ClassName.get(java.math.BigDecimal.class));
        TYPE_MAPPING.put("NUMERIC", ClassName.get(java.math.BigDecimal.class));
        TYPE_MAPPING.put("FLOAT", ClassName.get(Float.class));
        TYPE_MAPPING.put("DOUBLE", ClassName.get(Double.class));
        TYPE_MAPPING.put("DATE", ClassName.get(java.time.LocalDate.class));
        TYPE_MAPPING.put("DATETIME", ClassName.get(java.time.LocalDateTime.class));
        TYPE_MAPPING.put("TIMESTAMP", ClassName.get(java.time.LocalDateTime.class));
        TYPE_MAPPING.put("TIME", ClassName.get(java.time.LocalTime.class));
        TYPE_MAPPING.put("BOOLEAN", ClassName.get(Boolean.class));
        TYPE_MAPPING.put("BIT", ClassName.get(Boolean.class));
        TYPE_MAPPING.put("BLOB", ArrayTypeName.of(byte.class));
        TYPE_MAPPING.put("LONGBLOB", ArrayTypeName.of(byte.class));
        TYPE_MAPPING.put("JSON", ClassName.get(String.class));

        // Initialize JPA annotations
        JPA_ANNOTATIONS.put("Entity", ClassName.get(PERSISTENCE, "Entity"));
        JPA_ANNOTATIONS.put("Table", ClassName.get(PERSISTENCE, "Table"));
        JPA_ANNOTATIONS.put("Id", ClassName.get(PERSISTENCE, "Id"));
        JPA_ANNOTATIONS.put("GeneratedValue", ClassName.get(PERSISTENCE, "GeneratedValue"));
        JPA_ANNOTATIONS.put("Column", ClassName.get(PERSISTENCE, "Column"));
        JPA_ANNOTATIONS.put("ManyToOne", ClassName.get(PERSISTENCE, "ManyToOne"));
        JPA_ANNOTATIONS.put("OneToMany", ClassName.get(PERSISTENCE, "OneToMany"));
        JPA_ANNOTATIONS.put("JoinColumn", ClassName.get(PERSISTENCE, "JoinColumn"));
        JPA_ANNOTATIONS.put("Lob", ClassName.get(PERSISTENCE, "Lob"));
    }

    public String generateModelClass(DatabaseTable table)
    {
        return buildJavaFile(table, DEFAULT_PACKAGE).toString();
    }

    private void writeModelToFile(JavaFile javaFile) throws IOException
    {
        // Convert package to path (com.example -> com/example)
        String packagePath = javaFile.packageName.replace('.', File.separatorChar);

        // Create full output path
        Path outputPath = Paths.get(DEFAULT_OUTPUT_ROOT, packagePath);

        // Create directories if they don't exist
        if (!Files.exists(outputPath))
            Files.createDirectories(outputPath);

        // Write the file
        javaFile.writeTo(outputPath);
    }

    public Map<String, String> generateAllModels(List<DatabaseTable> tables) throws IOException
    {
        Map<String, String> models = new LinkedHashMap<>();
        Set<String> usedClassNames = new HashSet<>();

        for (DatabaseTable table : tables)
        {
            String className = getUniqueClassName(table.getName(), usedClassNames);
            JavaFile javaFile = buildJavaFile(table, className);
            models.put(table.getName(), javaFile.toString());
            writeModelToFile(javaFile);
        }
        return models;
    }

    private String getUniqueClassName(String originalName, Set<String> usedClassNames)
    {
        String className = toCamelCase(originalName, true);
        String baseClassName = className;
        int counter = 1;

        while (usedClassNames.contains(className))
        {
            className = baseClassName + counter++;
        }
        usedClassNames.add(className);
        return className;
    }

    private JavaFile buildJavaFile(DatabaseTable table, String basePackage)
    {
        String className = toCamelCase(table.getName(), true);
        TypeSpec modelClass = buildTypeSpec(table, className);

        return JavaFile.builder(basePackage, modelClass)
                .indent("    ")
                .build();
    }

    private TypeSpec buildTypeSpec(DatabaseTable table, String className)
    {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(JPA_ANNOTATIONS.get("Entity"))
                .addAnnotation(AnnotationSpec.builder(JPA_ANNOTATIONS.get("Table"))
                        .addMember("name", "$S", table.getName())
                        .build());

        table.getColumns().forEach(column -> addColumnToClass(table, classBuilder, column));
        classBuilder.addMethod(buildToStringMethod(className, table));

        return classBuilder.build();
    }

    private void addColumnToClass(DatabaseTable table, TypeSpec.Builder classBuilder, DatabaseColumn column)
    {
        String fieldName = toCamelCase(column.getName(), false);
        TypeName fieldType = TYPE_MAPPING.getOrDefault(column.getType().toUpperCase(),
                ClassName.get(String.class));

        FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, fieldName)
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(buildColumnAnnotation(column));

        // Add special annotations for certain types
        if (column.getType().equalsIgnoreCase("BLOB") ||
                column.getType().equalsIgnoreCase("LONGBLOB"))
        {
            fieldBuilder.addAnnotation(JPA_ANNOTATIONS.get("Lob"));
        }

        // Handle primary keys
        if (table.getPrimaryKeys().contains(column.getName()))
        {
            fieldBuilder.addAnnotation(JPA_ANNOTATIONS.get("Id"));
            if (column.isAutoIncrement())
            {
                fieldBuilder.addAnnotation(AnnotationSpec.builder(JPA_ANNOTATIONS.get("GeneratedValue"))
                        .addMember("strategy", "$T.IDENTITY",
                                ClassName.get(PERSISTENCE, "GenerationType"))
                        .build());
            }
        }
        classBuilder.addField(fieldBuilder.build());
        classBuilder.addMethod(buildGetterMethod(fieldName, fieldType));
        classBuilder.addMethod(buildSetterMethod(fieldName, fieldType));
    }

    private AnnotationSpec buildColumnAnnotation(DatabaseColumn column)
    {
        AnnotationSpec.Builder builder = AnnotationSpec.builder(JPA_ANNOTATIONS.get("Column"))
                .addMember("name", "$S", column.getName());

        if (!column.isNullable())
        {
            builder.addMember("nullable", "$L", false);
        }

        if (column.getSize() > 0 && !column.getType().equalsIgnoreCase("TEXT"))
        {
            builder.addMember("length", "$L", column.getSize());
        }
        return builder.build();
    }

    private MethodSpec buildGetterMethod(String fieldName, TypeName fieldType)
    {
        return MethodSpec.methodBuilder("get" + toCamelCase(fieldName, true))
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldType)
                .addStatement("return this.$N", fieldName)
                .build();
    }

    private MethodSpec buildSetterMethod(String fieldName, TypeName fieldType)
    {
        return MethodSpec.methodBuilder("set" + toCamelCase(fieldName, true))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(fieldType, fieldName)
                .addStatement("this.$N = $N", fieldName, fieldName)
                .build();
    }

    private MethodSpec buildToStringMethod(String className, DatabaseTable table)
    {
        return MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $S + $L + $S",
                        className + "{",
                        buildToStringFields(table),
                        "}")
                .build();
    }

    private String buildToStringFields(DatabaseTable table)
    {
        StringBuilder sb = new StringBuilder();
        table.getColumns().forEach(column ->
        {
            String fieldName = toCamelCase(column.getName(), false);
            if (!sb.isEmpty())
            {
                sb.append(" + \", ");
            }
            else
            {
                sb.append("\"");
            }
            sb.append(fieldName).append("='\" + ").append(fieldName).append(" + \"'");
        });
        return sb.toString();
    }

    private String toCamelCase(String input, boolean capitalizeFirst)
    {
        if (input == null || input.isEmpty())
        {
            return input;
        }
        String[] parts = input.split("[_\\s]");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++)
        {
            String part = parts[i];
            if (part.isEmpty()) continue;

            if (i == 0 && !capitalizeFirst)
            {
                result.append(part.toLowerCase());
            }
            else
            {
                result.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }
}