package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils;

import java.util.List;

public class Expression extends PlSqlAst {
  private final CursorExpression cursorExpression;
  private final LogicalExpression logicalExpression;

  // Constructor for cursor expression
  public Expression(CursorExpression cursorExpression) {
    this.cursorExpression = cursorExpression;
    this.logicalExpression = null;
  }

  // Constructor for logical expression
  public Expression(LogicalExpression logicalExpression) {
    this.cursorExpression = null;
    this.logicalExpression = logicalExpression;
  }

  public boolean isCursorExpression() {
    return cursorExpression != null;
  }

  public boolean isLogicalExpression() {
    return logicalExpression != null;
  }

  public CursorExpression getCursorExpression() {
    return cursorExpression;
  }

  public LogicalExpression getLogicalExpression() {
    return logicalExpression;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    if (cursorExpression != null) {
      return cursorExpression.toString();
    } else if (logicalExpression != null) {
      return logicalExpression.toString();
    } else {
      return "Expression{}";
    }
  }

  // toJava() method removed - expressions stay in PostgreSQL

  public String toPostgre(Everything data) {
    if (cursorExpression != null) {
      return cursorExpression.toPostgre(data);
    } else if (logicalExpression != null) {
      return logicalExpression.toPostgre(data);
    } else {
      throw new IllegalStateException("Expression must have either cursor or logical expression");
    }
  }

  /**
   * Determines if this expression represents a function call.
   * For logical expressions, check if the underlying expression is a function call.
   */
  public boolean isFunctionCall() {
    if (logicalExpression != null) {
      // For now, we'll need to examine the logical expression structure
      // This may need further refinement based on how logical expressions are structured
      return logicalExpression.toString().contains("(") && logicalExpression.toString().contains(")");
    }
    return false; // Cursor expressions are not function calls
  }

  /**
   * Gets the processed name with schema prefix for PostgreSQL output.
   * Adds schema prefix for function calls to packages/object types when needed.
   * Returns raw text for simple column references.
   */
  public String getNameForStatementExpressionWithSchema(Everything data, String schemaWhereTheStatementIsRunning, List<TableReference> fromTables) {
    String expressionText = this.toString();
    String schemaPrefix = data.lookupSchemaForExpression(this, schemaWhereTheStatementIsRunning, fromTables);
    
    if (schemaPrefix != null) {
      // A schema prefix has been found, so it is a function
      // if the text starts with the Schema prefix (case insenstive compared)
      // then we do not need to add the schema again, but need to follow the convention
      // for schema and function/procedurenaming: the schema is always upper case,
      // the packagename is also upper case and a PREFIX (with a following underscore)
      // to the function/procedurename which is always lowercase
      // if the schema name is present it will be added.
      // so in both cases the segment that is the packagename must be found, uppercased
      // and the dot between packagename and function/procedurename needs to be replaced
      // with an underscore.
      
      String processedText = expressionText;
      boolean hasSchemaInText = false;
      
      // Check if the text already starts with the schema prefix (case insensitive)
      if (expressionText.toLowerCase().startsWith(schemaPrefix.toLowerCase() + ".")) {
        // Remove the schema prefix from the text since we'll handle it separately  
        processedText = expressionText.substring(schemaPrefix.length() + 1);
        hasSchemaInText = true;
      }
      
      // Find the package name and function name
      int dotIndex = processedText.indexOf('.');
      if (dotIndex > 0) {
        String packageName = processedText.substring(0, dotIndex);
        String functionName = processedText.substring(dotIndex + 1);
        
        // Apply naming convention: SCHEMA.PACKAGE_functionname
        String result = schemaPrefix.toUpperCase() + "." + 
                       packageName.toUpperCase() + "_" + 
                       functionName.toLowerCase();
        return result;
      } else {
        // No package separation found, treat as simple function
        return schemaPrefix.toUpperCase() + "." + processedText.toLowerCase();
      }
    }
    
    // Return expression text for simple column references
    if (expressionText.contains(".")) {
      return expressionText; // TODO it has a table alias or name prefix..
    }
    
    // Check if this is a literal constant that should not be quoted
    if (isLiteralConstant(expressionText)) {
      return expressionText; // Return literals unchanged
    }
    
    return PostgreSqlIdentifierUtils.quoteIdentifier(expressionText.toUpperCase());
  }

  /**
   * Determines if the given expression text represents a literal constant that should
   * not be quoted as an identifier.
   * 
   * @param expressionText The text to check
   * @return true if this is a literal constant (number, string, boolean, etc.)
   */
  private boolean isLiteralConstant(String expressionText) {
    if (expressionText == null || expressionText.trim().isEmpty()) {
      return false;
    }
    
    String trimmed = expressionText.trim();
    
    // Check for numeric literals (integers, decimals, scientific notation)
    if (isNumericLiteral(trimmed)) {
      return true;
    }
    
    // Check for string literals (single quoted)
    if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
      return true;
    }
    
    // Check for boolean literals
    if (trimmed.equalsIgnoreCase("TRUE") || trimmed.equalsIgnoreCase("FALSE")) {
      return true;
    }
    
    // Check for NULL literal
    if (trimmed.equalsIgnoreCase("NULL")) {
      return true;
    }
    
    return false;
  }
  
  /**
   * Checks if a string represents a numeric literal.
   */
  private boolean isNumericLiteral(String text) {
    if (text == null || text.isEmpty()) {
      return false;
    }
    
    try {
      // Try parsing as different numeric types
      if (text.contains(".") || text.toLowerCase().contains("e")) {
        // Decimal or scientific notation
        Double.parseDouble(text);
      } else {
        // Integer
        Long.parseLong(text);
      }
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

}