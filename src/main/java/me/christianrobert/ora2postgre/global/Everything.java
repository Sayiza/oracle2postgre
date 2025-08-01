package me.christianrobert.ora2postgre.global;

import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.oracledb.IndexMetadata;
import me.christianrobert.ora2postgre.oracledb.SynonymMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.oracledb.ViewMetadata;
import me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.NestedTableType;
import me.christianrobert.ora2postgre.plsql.ast.ObjectType;
import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;
import me.christianrobert.ora2postgre.plsql.ast.TableReference;
import me.christianrobert.ora2postgre.plsql.ast.Trigger;
import me.christianrobert.ora2postgre.plsql.ast.Variable;
import me.christianrobert.ora2postgre.plsql.ast.VarrayType;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@ApplicationScoped
public class Everything {
  // raw data from the database
  private List<String> userNames = new ArrayList<>();
  private List<TableMetadata> tableSql = new ArrayList<>();
  
  // CTE (Common Table Expression) scope tracking
  private Set<String> activeCTENames = new HashSet<>();
  
  // Function context tracking for semantic resolution
  private Function currentFunction = null;
  private List<ViewMetadata> viewDefinition = new ArrayList<>();
  private List<SynonymMetadata> synonyms = new ArrayList<>();
  private List<IndexMetadata> indexes = new ArrayList<>();
  private List<PlsqlCode> objectTypeSpecPlsql = new ArrayList<>();
  private List<PlsqlCode> objectTypeBodyPlsql = new ArrayList<>();
  private List<PlsqlCode> packageSpecPlsql = new ArrayList<>();
  private List<PlsqlCode> packageBodyPlsql = new ArrayList<>();
  private List<PlsqlCode> standaloneFunctionPlsql = new ArrayList<>();
  private List<PlsqlCode> standaloneProcedurePlsql = new ArrayList<>();
  private List<PlsqlCode> triggerPlsql = new ArrayList<>();

  // parsed data
  // TODO table default Expression!!!
  private List<ViewSpecAndQuery> viewSpecAndQueries = new ArrayList<>();
  private List<ObjectType> objectTypeSpecAst = new ArrayList<>();
  private List<ObjectType> objectTypeBodyAst = new ArrayList<>();
  private List<OraclePackage> packageSpecAst = new ArrayList<>();
  private List<OraclePackage> packageBodyAst = new ArrayList<>();
  private List<Function> standaloneFunctionAst = new ArrayList<>();
  private List<Procedure> standaloneProcedureAst = new ArrayList<>();
  private List<Trigger> triggerAst = new ArrayList<>();

  private long totalRowCount = 0;
  private int intendations = 0;

  public List<String> getUserNames() {
    return userNames;
  }

  public List<TableMetadata> getTableSql() {
    return tableSql;
  }

  public List<ViewMetadata> getViewDefinition() {
    return viewDefinition;
  }

  public List<SynonymMetadata> getSynonyms() { return synonyms; }

  public List<IndexMetadata> getIndexes() { return indexes; }

  public List<PlsqlCode> getObjectTypeSpecPlsql() {
    return objectTypeSpecPlsql;
  }

  public List<PlsqlCode> getObjectTypeBodyPlsql() {
    return objectTypeBodyPlsql;
  }

  public List<PlsqlCode> getPackageSpecPlsql() {
    return packageSpecPlsql;
  }

  public List<PlsqlCode> getPackageBodyPlsql() {
    return packageBodyPlsql;
  }

  public List<PlsqlCode> getStandaloneFunctionPlsql() {
    return standaloneFunctionPlsql;
  }

  public List<PlsqlCode> getStandaloneProcedurePlsql() {
    return standaloneProcedurePlsql;
  }

  public List<PlsqlCode> getTriggerPlsql() {
    return triggerPlsql;
  }

  public List<ViewSpecAndQuery> getViewSpecAndQueries() {
    return viewSpecAndQueries;
  }

  public List<ObjectType> getObjectTypeSpecAst() {
    return objectTypeSpecAst;
  }

  public List<ObjectType> getObjectTypeBodyAst() {
    return objectTypeBodyAst;
  }

  public List<OraclePackage> getPackageSpecAst() {
    return packageSpecAst;
  }

  public List<OraclePackage> getPackageBodyAst() {
    return packageBodyAst;
  }

  public List<Function> getStandaloneFunctionAst() {
    return standaloneFunctionAst;
  }

  public List<Procedure> getStandaloneProcedureAst() {
    return standaloneProcedureAst;
  }

  public List<Trigger> getTriggerAst() {
    return triggerAst;
  }

  public long getTotalRowCount() {
    return totalRowCount;
  }

  public void setTotalRowCount(long totalRowCount) {
    this.totalRowCount = totalRowCount;
  }

  // Statistics methods for standalone functions and procedures
  public int getStandaloneFunctionCount() {
    return standaloneFunctionAst.size();
  }

  public int getStandaloneProcedureCount() {
    return standaloneProcedureAst.size();
  }

  public int getStandaloneFunctionPlsqlCount() {
    return standaloneFunctionPlsql.size();
  }

  public int getStandaloneProcedurePlsqlCount() {
    return standaloneProcedurePlsql.size();
  }

  public void intendMore() {
    intendations += 2;
  }

  public void intendLess() {
    intendations -= 2;
  }

  public void resetIntendation() {
    intendations = 0;
  }

  public String getIntendation() {
    if (intendations <= 0) {
      return "";
    }
    return " ".repeat(intendations);
  }

  public String lookupSchema4ObjectType(String objectTypeName, String schemaWhereTheAskingCodeResides) {
    for (ObjectType objectType : objectTypeSpecAst) {
      if (objectType.getName().equalsIgnoreCase(objectTypeName)
              && objectType.getSchema().equalsIgnoreCase(schemaWhereTheAskingCodeResides)) {
        return objectType.getSchema();
      }
    }

    // Step 2: Look up synonym in the given schema
    List<SynonymMetadata> matchingSynonyms = synonyms.stream()
            .filter(s -> s.getSynonymName().equalsIgnoreCase(objectTypeName) &&
                    ( s.getSchema().equalsIgnoreCase(schemaWhereTheAskingCodeResides)
                            || s.getSchema().equalsIgnoreCase("PUBLIC")
                    )
            )
            .toList();

    if (!matchingSynonyms.isEmpty()) {
      SynonymMetadata synonym = matchingSynonyms.get(0);
      // Verify the referenced object exists in tableSql or viewDefinition
      String refSchema = synonym.getReferencedSchema();
      String refObject = synonym.getReferencedObjectName();
      boolean objectExists = objectTypeBodyAst.stream()
              .anyMatch(t -> t.getName().equalsIgnoreCase(refObject) &&
                      t.getSchema().equalsIgnoreCase(refSchema));

      if (objectExists) {
        return refSchema;
      }
    }

    // Step 3, public synonyms are not included, take the next best!? TODO
    for (ObjectType objectType : objectTypeSpecAst) {
      if (objectType.getName().equalsIgnoreCase(objectTypeName)) {
        return objectType.getSchema();
      }
    }
    return null;
  }

  public String lookupSchema4Field(String tableOrViewName, String schema) {
    // Step 1: Check for exact match in the given schema
    for (TableMetadata t : tableSql) {
      if (t.getTableName().equalsIgnoreCase(tableOrViewName) && t.getSchema().equalsIgnoreCase(schema)) {
        return t.getSchema();
      }
    }
    for (ViewMetadata v : viewDefinition) {
      if (v.getViewName().equalsIgnoreCase(tableOrViewName) && v.getSchema().equalsIgnoreCase(schema)) {
        return v.getSchema();
      }
    }

    // Step 2: Look up synonym in the given schema
    List<SynonymMetadata> matchingSynonyms = synonyms.stream()
            .filter(s -> s.getSynonymName().equalsIgnoreCase(tableOrViewName) &&
                    s.getSchema().equalsIgnoreCase(schema))
            .toList();

    if (matchingSynonyms.size() == 1) {
      SynonymMetadata synonym = matchingSynonyms.get(0);
      // Verify the referenced object exists in tableSql or viewDefinition
      String refSchema = synonym.getReferencedSchema();
      String refObject = synonym.getReferencedObjectName();
      boolean objectExists = tableSql.stream()
              .anyMatch(t -> t.getTableName().equalsIgnoreCase(refObject) &&
                      t.getSchema().equalsIgnoreCase(refSchema)) ||
              viewDefinition.stream()
                      .anyMatch(v -> v.getViewName().equalsIgnoreCase(refObject) &&
                              v.getSchema().equalsIgnoreCase(refSchema));

      if (objectExists) {
        return refSchema;
      } else {
        throw new IllegalStateException("Synonym " + schema + "." + tableOrViewName +
                " points to non-existent object: " + refSchema + "." + refObject);
      }
    } else if (matchingSynonyms.isEmpty()) {
      throw new IllegalStateException("No synonym found for " + schema + "." + tableOrViewName +
              " in schema " + schema);
    } else {
      throw new IllegalStateException("Multiple synonyms found for " + schema + "." + tableOrViewName +
              " in schema " + schema);
    }
  }

  public String lookUpDataType(Expression expression, String schemaWhereTheStatementIsRunning, List<TableReference> fromTables) {
    String rawExpression = expression.toString();
    String identifierPattern = "[a-zA-Z_][a-zA-Z0-9_]*";
    String[] segments = rawExpression.split("\\.");

    String prefix = null;
    String column = null;
    String otherschema = null;

    if (segments.length == 1) {
      // Just a column name
      if (segments[0].matches(identifierPattern)) {
        column = segments[0];
      }
    } else if (segments.length == 2) {
      // table.column
      if (segments[0].matches(identifierPattern) && segments[1].matches(identifierPattern)) {
        prefix = segments[0];
        column = segments[1];
      }
    } else if (segments.length == 3) {
      // schema.table.column
      if (segments[0].matches(identifierPattern) &&
              segments[1].matches(identifierPattern) &&
              segments[2].matches(identifierPattern)) {
        prefix = segments[1];
        column = segments[2];
      }
    }
    if (column != null) {
      String dataType = lookUpDataType(column, prefix, otherschema, schemaWhereTheStatementIsRunning, fromTables);
      if (dataType != null) {
        return dataType; // nice it really is a column!
      }
    }

    // what else could it be ... a function:
    String functionReturnType = lookUpFunctionReturnType(rawExpression, schemaWhereTheStatementIsRunning);
    if (functionReturnType != null) {
      return functionReturnType;
    }

    return "varchar2"; // TODO: handle other cases like literals, operators, etc.
  }

  /**
   * Determines the Oracle data type of a column in an SQL statement, based on the FROM clause tables/views
   * and considering aliases, table names, synonyms, and optional schema prefix.
   *
   * @param targetColumnName                 The column name (e.g., "first_name") or function name in a package reference
   * @param targetColumnTablePrefixExpression The table alias or table name prefix (e.g., "e" or "employees"), or null/empty if none
   * @param targetColumnSchemaPrefixExpression The schema prefix (e.g., "HR" in "HR.employees.first_name"), or null if none
   * @param schemaWhereTheStatementIsRunning The schema context where the statement is running
   * @param fromTables                       List of TableReference objects from the FROM clause
   * @return The Oracle data type of the column (e.g., "VARCHAR2", "NUMBER"), or null if not found or if referencing a package function
   */
  private String lookUpDataType(String targetColumnName, String targetColumnTablePrefixExpression,
                                String targetColumnSchemaPrefixExpression, String schemaWhereTheStatementIsRunning,
                                List<TableReference> fromTables) {
    if (targetColumnName == null || targetColumnName.trim().isEmpty()) {
      return null; // Invalid column name
    }
    if (fromTables == null || fromTables.isEmpty()) {
      return null; // No tables/views to search
    }

    // Step 1: Handle three-part identifier (schema.table.column) if schema prefix is provided
    if (targetColumnSchemaPrefixExpression != null && !targetColumnSchemaPrefixExpression.trim().isEmpty() &&
            targetColumnTablePrefixExpression != null && !targetColumnTablePrefixExpression.trim().isEmpty()) {
      String schema = targetColumnSchemaPrefixExpression.trim();
      String table = targetColumnTablePrefixExpression.trim();
      String dataType = findColumnDataType(targetColumnName, schema, table);
      if (dataType != null) {
        return dataType;
      }
      // If schema.table.column doesn't match, assume it might be a package function (e.g., pkg.func)
      return null;
    }

    // Step 2: Handle column with table prefix (alias or table name)
    if (targetColumnTablePrefixExpression != null && !targetColumnTablePrefixExpression.trim().isEmpty()) {
      String prefix = targetColumnTablePrefixExpression.trim();

      // Find matching TableReference by alias or table name
      TableReference matchedTable = null;
      for (TableReference tr : fromTables) {
        String alias = tr.getTableAlias();
        String tableName = tr.getTableName();
        if (alias != null && alias.equalsIgnoreCase(prefix)) {
          matchedTable = tr;
          break;
        }
        if (tableName != null && tableName.equalsIgnoreCase(prefix)) {
          matchedTable = tr;
          break;
        }
      }

      if (matchedTable != null) {
        // Resolve schema using lookupSchema4Field (handles synonyms)
        String schema = lookupSchema4Field(matchedTable.getTableName(),
                matchedTable.getSchemaName() != null ? matchedTable.getSchemaName() : schemaWhereTheStatementIsRunning);
        String dataType = findColumnDataType(targetColumnName, schema, matchedTable.getTableName());
        if (dataType != null) {
          return dataType;
        }
        // Column not found in matched table; assume package function or invalid reference
        return null;
      }

      // Prefix not found in fromTables; try synonym in the current schema
      try {
        String refSchema = lookupSchema4Field(prefix, schemaWhereTheStatementIsRunning);
        String dataType = findColumnDataType(targetColumnName, refSchema, prefix);
        if (dataType != null) {
          return dataType;
        }
        // Column not found for synonym; assume package function or invalid reference
        return null;
      } catch (IllegalStateException e) {
        // No synonym or multiple synonyms found; assume package function
        return null;
      }
    }

    // Step 3: Handle column without prefix (must be unique across fromTables)
    List<String> foundDataTypes = new ArrayList<>();
    for (TableReference tr : fromTables) {
      String tableName = tr.getTableName();
      String schema = tr.getSchemaName() != null ? tr.getSchemaName() : schemaWhereTheStatementIsRunning;

      // Resolve schema using lookupSchema4Field (handles synonyms)
      try {
        String resolvedSchema = lookupSchema4Field(tableName, schema);
        String dataType = findColumnDataType(targetColumnName, resolvedSchema, tableName);
        if (dataType != null) {
          foundDataTypes.add(dataType);
        }
      } catch (IllegalStateException e) {
        // Skip tables with unresolved synonyms
        continue;
      }
    }

    if (foundDataTypes.size() == 1) {
      return foundDataTypes.get(0);
    } else {
      // Ambiguous column or no match; return null
      return null;
    }
  }

  /**
   * Helper method to find the data type of a column in a specific table or view.
   *
   * @param columnName The column name to look up
   * @param schema     The schema of the table/view
   * @param tableName  The table or view name
   * @/rootfs/podman/podman.socketurn The Oracle data type, or null if not found
   */
  private String findColumnDataType(String columnName, String schema, String tableName) {
    // Check tables
    for (TableMetadata t : tableSql) {
      if (t.getSchema().equalsIgnoreCase(schema) && t.getTableName().equalsIgnoreCase(tableName)) {
        for (ColumnMetadata col : t.getColumns()) {
          if (col.getColumnName().equalsIgnoreCase(columnName)) {
            return col.getDataType();
          }
        }
      }
    }

    // Check views
    for (ViewMetadata v : viewDefinition) {
      if (v.getSchema().equalsIgnoreCase(schema) && v.getViewName().equalsIgnoreCase(tableName)) {
        for (ColumnMetadata col : v.getColumns()) {
          if (col.getColumnName().equalsIgnoreCase(columnName)) {
            return col.getDataType();
          }
        }
      }
    }

    return null;
  }


  /**
   * Unified function for resolving schema and name for both object types and packages.
   * Handles direct lookups and synonym resolution.
   * 
   * @param name The object type or package name to look up
   * @param schema The schema context to search in
   * @param type The type of object we're looking for
   * @return SynonymResolutionResult with resolved schema and name, or null if not found
   */
  private SynonymResolutionResult lookupSchemaAndName(String name, String schema, DatabaseObjectType type) {
    // Step 1: Check for direct match in the given schema
    switch (type) {
      case OBJECT_TYPE:
        for (ObjectType objType : objectTypeSpecAst) {
          if (objType.getName().equalsIgnoreCase(name) && objType.getSchema().equalsIgnoreCase(schema)) {
            return new SynonymResolutionResult(objType.getSchema(), objType.getName());
          }
        }
        break;
      case PACKAGE:
        for (OraclePackage pkg : packageSpecAst) {
          if (pkg.getName().equalsIgnoreCase(name) && pkg.getSchema().equalsIgnoreCase(schema)) {
            return new SynonymResolutionResult(pkg.getSchema(), pkg.getName());
          }
        }
        break;
    }

    // Step 2: Look up synonym in the given schema
    String synonymObjectType = type == DatabaseObjectType.OBJECT_TYPE ? "TYPE" : "PACKAGE";
    List<SynonymMetadata> matchingSynonyms = synonyms.stream()
            .filter(s -> s.getSynonymName().equalsIgnoreCase(name) &&
                    s.getSchema().equalsIgnoreCase(schema) &&
                    synonymObjectType.equalsIgnoreCase(s.getReferencedObjectType()))
            .toList();

    if (matchingSynonyms.size() == 1) {
      SynonymMetadata synonym = matchingSynonyms.get(0);
      String refSchema = synonym.getReferencedSchema();
      String refObject = synonym.getReferencedObjectName();
      
      // Verify the referenced object exists
      boolean objectExists = false;
      switch (type) {
        case OBJECT_TYPE:
          objectExists = objectTypeSpecAst.stream()
                  .anyMatch(ot -> ot.getName().equalsIgnoreCase(refObject) &&
                          ot.getSchema().equalsIgnoreCase(refSchema));
          break;
        case PACKAGE:
          objectExists = packageSpecAst.stream()
                  .anyMatch(pkg -> pkg.getName().equalsIgnoreCase(refObject) &&
                          pkg.getSchema().equalsIgnoreCase(refSchema));
          break;
      }

      if (objectExists) {
        return new SynonymResolutionResult(refSchema, refObject);
      } else {
        throw new IllegalStateException("Synonym " + schema + "." + name +
                " points to non-existent " + type.toString().toLowerCase() + ": " + refSchema + "." + refObject);
      }
    } else if (matchingSynonyms.size() > 1) {
      throw new IllegalStateException("Multiple synonyms found for " + type.toString().toLowerCase() + " " + schema + "." + name);
    }

    // No direct match or synonym found
    return null;
  }

  public void findDefaultExpression(String schemaWhereWeAreNow, String myTableName, String columnName) {
    //TODO
  }

  /**
   * Enum for specifying the type of database object we're looking for.
   */
  private enum DatabaseObjectType {
    OBJECT_TYPE,
    PACKAGE
  }

  /**
   * Helper class to hold synonym resolution results.
   */
  private static class SynonymResolutionResult {
    final String schema;
    final String objectTypeName;

    SynonymResolutionResult(String schema, String objectTypeName) {
      this.schema = schema;
      this.objectTypeName = objectTypeName;
    }
  }

  /**
   * Attempts to resolve a function call expression to its return type.
   * Handles various function call patterns:
   * - package.function(args)
   * - objecttype.function(args) 
   * - schema.package.function(args)
   * - schema.objecttype.function(args)
   * - Chained calls: obj1.getObj2(x).getField(y)
   * 
   * @param functionExpression The raw function expression (e.g., "PKG.FUNC(1,2)" or "obj.getX().getY()")
   * @param schemaWhereTheStatementIsRunning The schema context where the statement is running
   * @return The return type of the function, or null if not found or not a function
   */
  private String lookUpFunctionReturnType(String functionExpression, String schemaWhereTheStatementIsRunning) {
    if (functionExpression == null || functionExpression.trim().isEmpty()) {
      return null;
    }

    // Handle chained function calls by processing each segment
    String[] chainSegments = splitFunctionChain(functionExpression);
    if (chainSegments.length == 0) {
      return null;
    }

    String currentType = null;
    String currentSchema = schemaWhereTheStatementIsRunning;

    for (int i = 0; i < chainSegments.length; i++) {
      String segment = chainSegments[i].trim();
      FunctionCallInfo callInfo = parseFunctionCall(segment);
      
      if (callInfo == null) {
        return null; // Not a valid function call
      }

      if (i == 0) {
        // First segment: resolve package/objecttype and function
        currentType = resolveFirstFunctionCall(callInfo, currentSchema);
      } else {
        // Subsequent segments: function calls on the result of previous call
        currentType = resolveChainedFunctionCall(callInfo, currentType, currentSchema);
      }

      if (currentType == null) {
        return null; // Chain broken
      }
    }

    return currentType;
  }

  /**
   * Splits a function expression into chain segments.
   * E.g., "obj.getX().getY(1,2)" -> ["obj.getX()", "getY(1,2)"]
   */
  private String[] splitFunctionChain(String expression) {
    // This is a simplified approach - in reality you'd need proper parsing
    // to handle nested parentheses correctly
    String[] segments = expression.split("\\)\\.");
    for (int i = 0; i < segments.length - 1; i++) {
      segments[i] += ")"; // Add back the closing parenthesis
    }
    return segments;
  }

  /**
   * Parses a function call segment to extract components.
   */
  private FunctionCallInfo parseFunctionCall(String segment) {
    int parenIndex = segment.indexOf('(');
    if (parenIndex == -1) {
      return null; // Not a function call
    }

    String beforeParen = segment.substring(0, parenIndex).trim();
    String[] parts = beforeParen.split("\\.");

    FunctionCallInfo info = new FunctionCallInfo();
    
    if (parts.length == 1) {
      // function()
      info.functionName = parts[0];
    } else if (parts.length == 2) {
      // package.function() or objecttype.function()
      info.packageOrTypeName = parts[0];
      info.functionName = parts[1];
    } else if (parts.length == 3) {
      // schema.package.function() or schema.objecttype.function()
      info.schemaName = parts[0];
      info.packageOrTypeName = parts[1];
      info.functionName = parts[2];
    } else {
      return null; // Too many parts
    }

    return info;
  }

  /**
   * Resolves the first function call in a chain.
   */
  private String resolveFirstFunctionCall(FunctionCallInfo callInfo, String defaultSchema) {
    String targetSchema = callInfo.schemaName != null ? callInfo.schemaName : defaultSchema;
    
    if (callInfo.packageOrTypeName != null) {
      // Look for package function first
      String returnType = findPackageFunction(targetSchema, callInfo.packageOrTypeName, callInfo.functionName);
      if (returnType != null) {
        return returnType;
      }

      // Look for object type function
      returnType = findObjectTypeFunction(targetSchema, callInfo.packageOrTypeName, callInfo.functionName);
      if (returnType != null) {
        return returnType;
      }

      // Check synonyms for object types
      try {
        SynonymResolutionResult synonymResult = lookupSchemaAndName(callInfo.packageOrTypeName, targetSchema, DatabaseObjectType.OBJECT_TYPE);
        if (synonymResult != null && !synonymResult.schema.equals(targetSchema)) {
          return resolveFirstFunctionCall(
            new FunctionCallInfo(null, synonymResult.objectTypeName, callInfo.functionName), 
            synonymResult.schema);
        }
      } catch (IllegalStateException e) {
        // Object type synonym not found, try package synonyms
      }

      // Check synonyms for packages
      try {
        SynonymResolutionResult synonymResult = lookupSchemaAndName(callInfo.packageOrTypeName, targetSchema, DatabaseObjectType.PACKAGE);
        if (synonymResult != null && !synonymResult.schema.equals(targetSchema)) {
          return resolveFirstFunctionCall(
            new FunctionCallInfo(null, synonymResult.objectTypeName, callInfo.functionName), 
            synonymResult.schema);
        }
      } catch (IllegalStateException e) {
        // Package synonym not found, continue
      }
    } else {
      // Just function name - search all packages and object types in schema
      for (OraclePackage pkg : packageSpecAst) {
        if (pkg.getSchema().equalsIgnoreCase(targetSchema)) {
          String returnType = findFunctionInPackage(pkg, callInfo.functionName);
          if (returnType != null) {
            return returnType;
          }
        }
      }

      for (ObjectType objType : objectTypeSpecAst) {
        if (objType.getSchema().equalsIgnoreCase(targetSchema)) {
          String returnType = findFunctionInObjectType(objType, callInfo.functionName);
          if (returnType != null) {
            return returnType;
          }
        }
      }
    }

    return null;
  }

  /**
   * Resolves a chained function call based on the type returned by previous call.
   */
  private String resolveChainedFunctionCall(FunctionCallInfo callInfo, String objectTypeName, String schema) {
    // For chained calls, we expect the previous call returned an object type
    // Now we look for the function in that object type
    return findObjectTypeFunction(schema, objectTypeName, callInfo.functionName);
  }

  /**
   * Finds a function in a specific package.
   */
  private String findPackageFunction(String schema, String packageName, String functionName) {
    for (OraclePackage pkg : packageSpecAst) {
      if (pkg.getSchema().equalsIgnoreCase(schema) && pkg.getName().equalsIgnoreCase(packageName)) {
        return findFunctionInPackage(pkg, functionName);
      }
    }
    return null;
  }

  /**
   * Finds a function in a specific object type.
   */
  private String findObjectTypeFunction(String schema, String typeName, String functionName) {
    for (ObjectType objType : objectTypeSpecAst) {
      if (objType.getSchema().equalsIgnoreCase(schema) && objType.getName().equalsIgnoreCase(typeName)) {
        return findFunctionInObjectType(objType, functionName);
      }
    }
    return null;
  }

  /**
   * Helper to find a function within a package and return its return type.
   */
  private String findFunctionInPackage(OraclePackage pkg, String functionName) {
    for (Function func : pkg.getFunctions()) {
      if (func.getName().equalsIgnoreCase(functionName)) {
        return func.getReturnType();
      }
    }
    return null;
  }

  /**
   * Helper to find a function within an object type and return its return type.
   */
  private String findFunctionInObjectType(ObjectType objType, String functionName) {
    for (Function func : objType.getFunctions()) {
      if (func.getName().equalsIgnoreCase(functionName)) {
        return func.getReturnType();
      }
    }
    return null;
  }

  /**
   * Helper to find a procedure within a package.
   */
  private boolean findProcedureInPackage(OraclePackage pkg, String procedureName) {
    for (Procedure proc : pkg.getProcedures()) {
      if (proc.getName().equalsIgnoreCase(procedureName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Lookup procedure with schema resolution and synonym support.
   * 
   * @param procedureName The name of the procedure to find
   * @param packageName The package name (optional)
   * @param currentSchema The current schema context
   * @return The resolved schema name, or null if not found
   */
  public String lookupProcedureSchema(String procedureName, String packageName, String currentSchema) {
    return lookupProcedureSchema(procedureName, packageName, currentSchema, 0);
  }
  
  /**
   * Internal lookup procedure with recursion depth tracking to prevent infinite loops.
   * 
   * @param procedureName The name of the procedure to find
   * @param packageName The package name (optional)
   * @param currentSchema The current schema context
   * @param recursionDepth Current recursion depth for infinite loop prevention
   * @return The resolved schema name, or null if not found
   */
  private String lookupProcedureSchema(String procedureName, String packageName, String currentSchema, int recursionDepth) {
    // Prevent infinite recursion in synonym resolution
    if (recursionDepth > 10) {
      System.err.println("Warning: Maximum recursion depth reached in lookupProcedureSchema for " + 
                        procedureName + " (package: " + packageName + ", schema: " + currentSchema + ")");
      return null;
    }
    if (packageName != null) {
      // Look for package.procedure
      for (OraclePackage pkg : packageSpecAst) {
        if (pkg.getName().equalsIgnoreCase(packageName) && 
            pkg.getSchema().equalsIgnoreCase(currentSchema)) {
          if (findProcedureInPackage(pkg, procedureName)) {
            return pkg.getSchema();
          }
        }
      }
      for (OraclePackage pkg : packageBodyAst) {
        if (pkg.getName().equalsIgnoreCase(packageName) && 
            pkg.getSchema().equalsIgnoreCase(currentSchema)) {
          if (findProcedureInPackage(pkg, procedureName)) {
            return pkg.getSchema();
          }
        }
      }
      
      // Try synonym resolution for package
      try {
        SynonymResolutionResult synonymResult = lookupSchemaAndName(packageName, currentSchema, DatabaseObjectType.PACKAGE);
        if (synonymResult != null) {
          return lookupProcedureSchema(procedureName, synonymResult.objectTypeName, synonymResult.schema, recursionDepth + 1);
        }
      } catch (IllegalStateException e) {
        // Package synonym not found, continue
      }
    } else {
      // Look for standalone procedure
      for (Procedure proc : standaloneProcedureAst) {
        if (proc.getName().equalsIgnoreCase(procedureName) && 
            proc.getSchema().equalsIgnoreCase(currentSchema)) {
          return proc.getSchema();
        }
      }
    }
    
    return null; // Not found
  }

  /**
   * Determine if a routine is a function or procedure.
   * 
   * @param routineName The name of the routine
   * @param packageName The package name (optional)
   * @param schema The schema name
   * @return true if it's a function, false if it's a procedure
   */
  public boolean isFunction(String routineName, String packageName, String schema) {
    if (packageName != null) {
      // Check in packages
      for (OraclePackage pkg : packageSpecAst) {
        if (pkg.getName().equalsIgnoreCase(packageName) && 
            pkg.getSchema().equalsIgnoreCase(schema)) {
          // Check if it's a function
          for (Function func : pkg.getFunctions()) {
            if (func.getName().equalsIgnoreCase(routineName)) {
              return true;
            }
          }
          // Check if it's a procedure
          for (Procedure proc : pkg.getProcedures()) {
            if (proc.getName().equalsIgnoreCase(routineName)) {
              return false;
            }
          }
        }
      }
      
      for (OraclePackage pkg : packageBodyAst) {
        if (pkg.getName().equalsIgnoreCase(packageName) && 
            pkg.getSchema().equalsIgnoreCase(schema)) {
          // Check if it's a function
          for (Function func : pkg.getFunctions()) {
            if (func.getName().equalsIgnoreCase(routineName)) {
              return true;
            }
          }
          // Check if it's a procedure
          for (Procedure proc : pkg.getProcedures()) {
            if (proc.getName().equalsIgnoreCase(routineName)) {
              return false;
            }
          }
        }
      }
    } else {
      // Check standalone functions
      for (Function func : standaloneFunctionAst) {
        if (func.getName().equalsIgnoreCase(routineName) && 
            func.getSchema().equalsIgnoreCase(schema)) {
          return true;
        }
      }
      
      // Check standalone procedures
      for (Procedure proc : standaloneProcedureAst) {
        if (proc.getName().equalsIgnoreCase(routineName) && 
            proc.getSchema().equalsIgnoreCase(schema)) {
          return false;
        }
      }
    }
    
    // Default to procedure if not found (safer assumption)
    return false;
  }

  /**
   * Determines the schema prefix needed for an expression in PostgreSQL output.
   * Handles function calls to packages/object types with synonym resolution.
   * Returns null for simple column references or non-function expressions.
   *
   * @param expression The expression to analyze
   * @param schemaWhereTheStatementIsRunning The schema context where the statement is running
   * @param fromTables List of TableReference objects from the FROM clause (may be null for some contexts)
   * @return The schema prefix to use, or null if no schema prefix is needed
   */
  public String lookupSchemaForExpression(Expression expression, String schemaWhereTheStatementIsRunning, List<TableReference> fromTables) {
    String rawExpression = expression.toString();
    
    // Check if it's a function call pattern
    if (!expression.isFunctionCall()) {
      return null; // Simple column reference - no schema prefix needed
    }
    
    // Parse function call to extract components
    FunctionCallInfo callInfo = parseFunctionCall(rawExpression);
    if (callInfo == null || callInfo.packageOrTypeName == null) {
      return null; // Not a qualified function call
    }
    
    String targetSchema = callInfo.schemaName != null ? callInfo.schemaName : schemaWhereTheStatementIsRunning;
    
    // Try package lookup first
    SynonymResolutionResult packageResult = lookupSchemaAndName(
        callInfo.packageOrTypeName, targetSchema, DatabaseObjectType.PACKAGE);
    if (packageResult != null) {
      return packageResult.schema;
    }
    
    // Try object type lookup
    SynonymResolutionResult objectTypeResult = lookupSchemaAndName(
        callInfo.packageOrTypeName, targetSchema, DatabaseObjectType.OBJECT_TYPE);
    if (objectTypeResult != null) {
      return objectTypeResult.schema;
    }
    
    return null;
  }

  /**
   * Determines if a given identifier is a known function rather than a variable.
   * This is crucial for distinguishing Oracle array indexing (arr(i)) from function calls (func(i)).
   * 
   * @param identifier The identifier to check (e.g., "v_arr", "my_function")
   * @param schema The schema context where the check is happening
   * @param currentFunction The function context where this identifier is being used (may be null)
   * @return true if the identifier is a known function, false if it's likely a variable
   */
  public boolean isKnownFunction(String identifier, String schema, Function currentFunction) {
    if (identifier == null || identifier.trim().isEmpty()) {
      return false;
    }
    
    String cleanIdentifier = identifier.trim();
    
    // Priority 1: Check if it's a variable in current function context
    if (currentFunction != null) {
      // Check function parameters
      if (currentFunction.getParameters() != null) {
        for (Parameter param : currentFunction.getParameters()) {
          if (param.getName().equalsIgnoreCase(cleanIdentifier)) {
            return false; // It's a parameter, not a function
          }
        }
      }
      
      // Check local variables
      if (currentFunction.getVariables() != null) {
        for (Variable var : currentFunction.getVariables()) {
          if (var.getName().equalsIgnoreCase(cleanIdentifier)) {
            return false; // It's a local variable, not a function
          }
        }
      }
      
      // Check local collection types (VARRAY/TABLE OF variables)
      if (currentFunction.getVarrayTypes() != null) {
        for (VarrayType varrayType : currentFunction.getVarrayTypes()) {
          // VarrayType represents a type declaration, but we need to check if variables use this type
          // This is more complex - for now, we'll rely on the variable list above
        }
      }
      
      if (currentFunction.getNestedTableTypes() != null) {
        for (NestedTableType nestedTableType : currentFunction.getNestedTableTypes()) {
          // Similar to VarrayType - type declarations don't directly tell us variable names
        }
      }
    }
    
    // Priority 2: Check standalone functions in the schema
    for (Function func : standaloneFunctionAst) {
      if (func.getSchema() != null && func.getSchema().equalsIgnoreCase(schema) &&
          func.getName().equalsIgnoreCase(cleanIdentifier)) {
        return true; // It's a standalone function
      }
    }
    
    // Priority 3: Check functions in packages within the schema
    for (OraclePackage pkg : packageSpecAst) {
      if (pkg.getSchema().equalsIgnoreCase(schema)) {
        for (Function func : pkg.getFunctions()) {
          if (func.getName().equalsIgnoreCase(cleanIdentifier)) {
            return true; // It's a package function
          }
        }
      }
    }
    
    // Priority 4: Check functions in object types within the schema
    for (ObjectType objType : objectTypeSpecAst) {
      if (objType.getSchema().equalsIgnoreCase(schema)) {
        for (Function func : objType.getFunctions()) {
          if (func.getName().equalsIgnoreCase(cleanIdentifier)) {
            return true; // It's an object type function
          }
        }
      }
    }
    
    // Priority 5: Check built-in Oracle functions (common ones that might be confused with variables)
    if (isBuiltInOracleFunction(cleanIdentifier)) {
      return true;
    }
    
    // If we can't find it as a function and didn't find it as a variable, 
    // assume it's a variable (safer for array indexing)
    return false;
  }
  
  /**
   * Helper method to identify common Oracle built-in functions that might be confused with variables.
   */
  private boolean isBuiltInOracleFunction(String identifier) {
    // Common Oracle functions that might appear in parentheses syntax
    String upperIdentifier = identifier.toUpperCase();
    switch (upperIdentifier) {
      case "SUBSTR":
      case "LENGTH":
      case "UPPER":
      case "LOWER":
      case "TRIM":
      case "LTRIM":
      case "RTRIM":
      case "DECODE":
      case "NVL":
      case "NVL2":
      case "COALESCE":
      case "TO_CHAR":
      case "TO_NUMBER":
      case "TO_DATE":
      case "SYSDATE":
      case "GREATEST":
      case "LEAST":
      case "ABS":
      case "ROUND":
      case "TRUNC":
      case "FLOOR":
      case "CEIL":
      case "MOD":
      case "POWER":
      case "SQRT":
        return true;
      default:
        return false;
    }
  }

  /**
   * Helper class to hold parsed function call information.
   */
  private static class FunctionCallInfo {
    String schemaName;
    String packageOrTypeName;
    String functionName;

    FunctionCallInfo() {}

    FunctionCallInfo(String schemaName, String packageOrTypeName, String functionName) {
      this.schemaName = schemaName;
      this.packageOrTypeName = packageOrTypeName;
      this.functionName = functionName;
    }
  }
  
  /**
   * CTE (Common Table Expression) scope tracking methods.
   * These methods manage the current CTE scope to prevent CTE names from being
   * resolved as regular table names during query processing.
   */
  
  /**
   * Adds a CTE name to the current scope.
   * @param cteName The name of the CTE to add to the active scope
   */
  public void addActiveCTE(String cteName) {
    if (cteName != null && !cteName.trim().isEmpty()) {
      activeCTENames.add(cteName.toUpperCase());
    }
  }
  
  /**
   * Removes a CTE name from the current scope.
   * @param cteName The name of the CTE to remove from the active scope
   */
  public void removeActiveCTE(String cteName) {
    if (cteName != null && !cteName.trim().isEmpty()) {
      activeCTENames.remove(cteName.toUpperCase());
    }
  }
  
  /**
   * Checks if a table name is actually a CTE name in the current scope.
   * @param tableName The table name to check
   * @return true if the name is an active CTE, false otherwise
   */
  public boolean isActiveCTE(String tableName) {
    if (tableName == null || tableName.trim().isEmpty()) {
      return false;
    }
    return activeCTENames.contains(tableName.toUpperCase());
  }
  
  /**
   * Clears all active CTE names from the current scope.
   * This should be called when exiting a query context that had CTEs.
   */
  public void clearActiveCTEs() {
    activeCTENames.clear();
  }
  
  /**
   * Gets a copy of the current active CTE names for debugging purposes.
   * @return A copy of the set of active CTE names
   */
  public Set<String> getActiveCTENames() {
    return new HashSet<>(activeCTENames);
  }
  
  /**
   * Gets the current function context for semantic resolution.
   * @return The current function context, or null if none set
   */
  public Function getCurrentFunction() {
    return currentFunction;
  }
  
  /**
   * Sets the current function context for semantic resolution.
   * @param function The function context to set
   */
  public void setCurrentFunction(Function function) {
    this.currentFunction = function;
  }
  
  /**
   * Gets all functions from all parsed packages and standalone functions.
   * This enables comprehensive search for function-local collection types.
   * @return List of all parsed functions
   */
  public List<Function> getAllFunctions() {
    List<Function> allFunctions = new ArrayList<>();
    
    // Add functions from package specs
    for (OraclePackage pkg : packageSpecAst) {
      if (pkg.getFunctions() != null) {
        allFunctions.addAll(pkg.getFunctions());
      }
    }
    
    // Add functions from package bodies
    for (OraclePackage pkg : packageBodyAst) {
      if (pkg.getFunctions() != null) {
        allFunctions.addAll(pkg.getFunctions());
      }
    }
    
    // Add standalone functions
    for (Function func : standaloneFunctionAst) {
      allFunctions.add(func);
    }
    
    return allFunctions;
  }
  
  /**
   * Determines if an identifier represents a collection type constructor.
   * This is the central semantic analysis point for collection constructors,
   * following the architectural principle of keeping semantic logic in Everything.
   * 
   * @param identifier The identifier to check (e.g., "local_array", "t_numbers")
   * @param currentFunction The function context (null if not in function)
   * @return true if this is a collection type constructor
   */
  public boolean isCollectionTypeConstructor(String identifier, Function currentFunction) {
    if (identifier == null || identifier.trim().isEmpty()) {
      return false;
    }
    
    String cleanIdentifier = identifier.trim();
    
    // 1. Check function-local collection types first (highest priority)
    if (currentFunction != null) {
      if (hasCollectionType(currentFunction, cleanIdentifier)) {
        return true;
      }
    }
    
    // 2. Check all functions for function-local collection types
    for (Function func : getAllFunctions()) {
      if (hasCollectionType(func, cleanIdentifier)) {
        return true;
      }
    }
    
    // 3. Check package-level collection types
    for (OraclePackage pkg : packageSpecAst) {
      if (hasCollectionType(pkg, cleanIdentifier)) {
        return true;
      }
    }
    
    for (OraclePackage pkg : packageBodyAst) {
      if (hasCollectionType(pkg, cleanIdentifier)) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Transforms a collection constructor to PostgreSQL array syntax.
   * This centralizes all collection constructor transformation logic.
   * 
   * @param identifier The collection type name
   * @param arguments The constructor arguments (may be truncated due to parsing limitations)
   * @param currentFunction The function context
   * @return PostgreSQL array syntax
   */
  public String transformCollectionConstructor(String identifier, List<Expression> arguments, Function currentFunction) {
    // Get the collection type information
    DataTypeSpec dataType = getCollectionElementType(identifier, currentFunction);
    
    if (arguments == null || arguments.isEmpty()) {
      // Empty constructor
      String postgresType = dataType != null ? dataType.toPostgre(this) : "TEXT";
      return "ARRAY[]::" + postgresType + "[]";
    }
    
    // Transform arguments
    StringBuilder argList = new StringBuilder();
    for (int i = 0; i < arguments.size(); i++) {
      if (i > 0) argList.append(", ");
      argList.append(arguments.get(i).toPostgre(this));
    }
    
    return "ARRAY[" + argList.toString() + "]";
  }
  
  /**
   * Helper: Check if a function has a collection type with the given name.
   */
  private boolean hasCollectionType(Function func, String typeName) {
    if (func == null || typeName == null) return false;
    
    // Check VARRAY types
    for (VarrayType varray : func.getVarrayTypes()) {
      if (typeName.equalsIgnoreCase(varray.getName())) {
        return true;
      }
    }
    
    // Check nested table types
    for (NestedTableType nestedTable : func.getNestedTableTypes()) {
      if (typeName.equalsIgnoreCase(nestedTable.getName())) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Helper: Check if a package has a collection type with the given name.
   */
  private boolean hasCollectionType(OraclePackage pkg, String typeName) {
    if (pkg == null || typeName == null) return false;
    
    // Check VARRAY types
    for (VarrayType varray : pkg.getVarrayTypes()) {
      if (typeName.equalsIgnoreCase(varray.getName())) {
        return true;
      }
    }
    
    // Check nested table types
    for (NestedTableType nestedTable : pkg.getNestedTableTypes()) {
      if (typeName.equalsIgnoreCase(nestedTable.getName())) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Helper: Get the element data type for a collection type.
   */
  private DataTypeSpec getCollectionElementType(String typeName, Function currentFunction) {
    // Check current function first
    if (currentFunction != null) {
      DataTypeSpec type = getCollectionElementTypeFromFunction(currentFunction, typeName);
      if (type != null) return type;
    }
    
    // Check all functions
    for (Function func : getAllFunctions()) {
      DataTypeSpec type = getCollectionElementTypeFromFunction(func, typeName);
      if (type != null) return type;
    }
    
    // Check packages
    for (OraclePackage pkg : packageSpecAst) {
      DataTypeSpec type = getCollectionElementTypeFromPackage(pkg, typeName);
      if (type != null) return type;
    }
    
    for (OraclePackage pkg : packageBodyAst) {
      DataTypeSpec type = getCollectionElementTypeFromPackage(pkg, typeName);
      if (type != null) return type;
    }
    
    return null; // Will default to TEXT in caller
  }
  
  private DataTypeSpec getCollectionElementTypeFromFunction(Function func, String typeName) {
    for (VarrayType varray : func.getVarrayTypes()) {
      if (typeName.equalsIgnoreCase(varray.getName())) {
        return varray.getDataType();
      }
    }
    
    for (NestedTableType nestedTable : func.getNestedTableTypes()) {
      if (typeName.equalsIgnoreCase(nestedTable.getName())) {
        return nestedTable.getDataType();
      }
    }
    
    return null;
  }
  
  private DataTypeSpec getCollectionElementTypeFromPackage(OraclePackage pkg, String typeName) {
    for (VarrayType varray : pkg.getVarrayTypes()) {
      if (typeName.equalsIgnoreCase(varray.getName())) {
        return varray.getDataType();
      }
    }
    
    for (NestedTableType nestedTable : pkg.getNestedTableTypes()) {
      if (typeName.equalsIgnoreCase(nestedTable.getName())) {
        return nestedTable.getDataType();
      }
    }
    
    return null;
  }
}
