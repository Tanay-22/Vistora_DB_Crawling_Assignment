package com.tanay.vistora.service;

import com.squareup.javapoet.*;
import com.tanay.vistora.model.DatabaseColumn;
import com.tanay.vistora.model.DatabaseTable;
import org.springframework.stereotype.Service;

import javax.lang.model.element.Modifier;
import java.util.HashMap;
import java.util.Map;

@Service
public class ModelGeneratorService
{
    private static final Map<String, TypeName> TYPE_MAPPING = new HashMap<>();
    private static final Map<String, ClassName> JPA_ANNOTATIONS = new HashMap<>();
    private static final String PERSISTENCE = "javax.persistence";
    private static final String DEFAULT_PACKAGE = "com.tanay.vistora.generated";

    static
    {
        // Type mappings
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

        // JPA Annotations
        JPA_ANNOTATIONS.put("Entity", ClassName.get(PERSISTENCE, "Entity"));
        JPA_ANNOTATIONS.put("Table", ClassName.get(PERSISTENCE, "Table"));
        JPA_ANNOTATIONS.put("Id", ClassName.get(PERSISTENCE, "Id"));
        JPA_ANNOTATIONS.put("GeneratedValue", ClassName.get(PERSISTENCE, "GeneratedValue"));
        JPA_ANNOTATIONS.put("Column", ClassName.get(PERSISTENCE, "Column"));
        JPA_ANNOTATIONS.put("ManyToOne", ClassName.get(PERSISTENCE, "ManyToOne"));
        JPA_ANNOTATIONS.put("OneToMany", ClassName.get(PERSISTENCE, "OneToMany"));
        JPA_ANNOTATIONS.put("JoinColumn", ClassName.get(PERSISTENCE, "JoinColumn"));
    }

    public String generateModelClass(DatabaseTable table)
    {
        String className = toCamelCase(table.getName(), true);
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC);

        // Add JPA Entity annotation
        classBuilder.addAnnotation(JPA_ANNOTATIONS.get("Entity"));

        // Add Table annotation
        AnnotationSpec tableAnnotation = AnnotationSpec.builder(JPA_ANNOTATIONS.get("Table"))
                .addMember("name", "$S", table.getName())
                .build();
        classBuilder.addAnnotation(tableAnnotation);

        for (DatabaseColumn column : table.getColumns())
        {
            String fieldName = toCamelCase(column.getName(), false);
            TypeName fieldType = TYPE_MAPPING.getOrDefault(column.getType().toUpperCase(), ClassName.get(String.class));

            FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, fieldName)
                    .addModifiers(Modifier.PRIVATE);

            // Add Column annotation
            AnnotationSpec.Builder columnAnnotation = AnnotationSpec.builder(JPA_ANNOTATIONS.get("Column"))
                    .addMember("name", "$S", column.getName());

            if (!column.isNullable())
                columnAnnotation.addMember("nullable", "$L", false);

            if (column.getSize() > 0 && !column.getType().equalsIgnoreCase("TEXT"))
                columnAnnotation.addMember("length", "$L", column.getSize());

            fieldBuilder.addAnnotation(columnAnnotation.build());

            // Add Id annotation for primary keys
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
            // Add field to class
            classBuilder.addField(fieldBuilder.build());

            // Generate getter
            MethodSpec getter = MethodSpec.methodBuilder("get" + toCamelCase(column.getName(), true))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(fieldType)
                    .addStatement("return this.$N", fieldName)
                    .build();
            classBuilder.addMethod(getter);

            // Generate setter
            MethodSpec setter = MethodSpec.methodBuilder("set" + toCamelCase(column.getName(), true))
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(fieldType, fieldName)
                    .addStatement("this.$N = $N", fieldName, fieldName)
                    .build();
            classBuilder.addMethod(setter);
        }

        // Generate toString()
        MethodSpec toString = MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $S + $L + $S", className + "{", buildToStringFields(table), "}")
                .build();
        classBuilder.addMethod(toString);

        // Generate the class
        TypeSpec modelClass = classBuilder.build();
        JavaFile javaFile = JavaFile.builder("com.example.generated.models", modelClass)
                .indent("    ")
                .build();

        return javaFile.toString();
    }

    private String buildToStringFields(DatabaseTable table)
    {
        StringBuilder sb = new StringBuilder();
        for (DatabaseColumn column : table.getColumns())
        {
            String fieldName = toCamelCase(column.getName(), false);
            if (!sb.isEmpty())
                sb.append(" + \", ");
            else
                sb.append("\"");

            sb.append(fieldName).append("='\" + ").append(fieldName).append(" + \"'");
        }
        return sb.toString();
    }

    private String toCamelCase(String input, boolean captalizeFirst)
    {
        if (input == null || input.isEmpty())
            return input;

        String[] parts = input.split("[_\\s]");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++)
        {
            String part = parts[i];
            if (part.isEmpty())
                continue;

            if (i == 0 && !captalizeFirst)
                result.append(part.toLowerCase());
            else
                result.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }

    private JavaFile buildJavaFile(DatabaseTable table)
    {
        return buildJavaFile(table, "com.example.generated.models");
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
                .addModifiers(Modifier.PUBLIC);

        // Add JPA Entity annotation
        classBuilder.addAnnotation(JPA_ANNOTATIONS.get("Entity"));

        // Add Table annotation
        classBuilder.addAnnotation(AnnotationSpec.builder(JPA_ANNOTATIONS.get("Table"))
                .addMember("name", "$S", table.getName())
                .build());

        // Process columns
        for (DatabaseColumn column : table.getColumns())
        {
            addColumnToClass(table, classBuilder, column);
        }

        // Generate toString()
        classBuilder.addMethod(MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $S + $L + $S",
                        className + "{",
                        buildToStringFields(table),
                        "}")
                .build());

        return classBuilder.build();
    }

    private void addColumnToClass(DatabaseTable table, TypeSpec.Builder classBuilder, DatabaseColumn column)
    {
        String fieldName = toCamelCase(column.getName(), false);
        TypeName fieldType = TYPE_MAPPING.getOrDefault(column.getType().toUpperCase(),
                ClassName.get(String.class));

        FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, fieldName)
                .addModifiers(Modifier.PRIVATE);

        // Add Column annotation
        AnnotationSpec.Builder columnAnnotation = AnnotationSpec.builder(JPA_ANNOTATIONS.get("Column"))
                .addMember("name", "$S", column.getName());

        if (!column.isNullable())
        {
            columnAnnotation.addMember("nullable", "$L", false);
        }
        if (column.getSize() > 0 && !column.getType().equalsIgnoreCase("TEXT"))
        {
            columnAnnotation.addMember("length", "$L", column.getSize());
        }

        fieldBuilder.addAnnotation(columnAnnotation.build());

        // Add Id annotation for primary keys
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

        // Generate getter
        classBuilder.addMethod(MethodSpec.methodBuilder("get" + toCamelCase(column.getName(), true))
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldType)
                .addStatement("return this.$N", fieldName)
                .build());

        // Generate setter
        classBuilder.addMethod(MethodSpec.methodBuilder("set" + toCamelCase(column.getName(), true))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(fieldType, fieldName)
                .addStatement("this.$N = $N", fieldName, fieldName)
                .build());
    }
}
