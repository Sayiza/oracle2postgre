package me.christianrobert.ora2postgre.plsql.ast;

public class SelectOrderByClause extends PlSqlAst {

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

}
