package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.oracledb.SchemaExtractor;

import java.nio.file.Paths;
import java.util.List;

public class ExportSchema {
  public static void saveSql(String path, List<String> users) {
    String content = SchemaExtractor.toPostgre(users);
    FileWriter.write(
            Paths.get(path),
            "ALLSCHEMA.sql",
            content
    );
  }
}
