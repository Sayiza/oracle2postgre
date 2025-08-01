package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.PackageTransformationManager;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExportPackage {
  
  private static final PackageTransformationManager packageManager = new PackageTransformationManager();
  
  // save2Java method disabled - no longer generating Java classes from Oracle packages
  public static void save2Java(String path, String javaPackageName, List<OraclePackage> specs, List<OraclePackage> bodies, Everything data) {
    // Method body removed - no longer generating Java classes from Oracle packages
    // Silently ignore for now to avoid breaking existing calls
  }

  public static void savePackageSpecToPostgre(String path, List<OraclePackage> specs, List<OraclePackage> bodies, Everything data) {
    for (OraclePackage o : mergeSpecAndBody(specs, bodies)) {
      String fullPathAsString = path +
              File.separator +
              o.getSchema().toLowerCase() +
              File.separator +
              "step3packagespec";
      // TODO name
      String transformedContent = packageManager.transform(o, data, true);
      FileWriter.write(Paths.get(fullPathAsString), o.getName() + ".sql", transformedContent);
    }
  }

  public static void savePackageBodyToPostgre(String path, List<OraclePackage> specs, List<OraclePackage> bodies, Everything data) {
    for (OraclePackage o : mergeSpecAndBody(specs, bodies)) {
      String fullPathAsString = path +
              File.separator +
              o.getSchema().toLowerCase() +
              File.separator +
              "step6packagebody";
      // TODO name
      String transformedContent = packageManager.transform(o, data, false);
      FileWriter.write(Paths.get(fullPathAsString), o.getName() + ".sql", transformedContent);
    }
  }

  /**
   * Merges Oracle package specifications and bodies into unified packages.
   * All components (variables, functions, procedures, types, etc.) from both spec and body are combined.
   * The assumption is that everything should be public in the generated PostgreSQL code.
   */
  private static List<OraclePackage> mergeSpecAndBody(List<OraclePackage> specs, List<OraclePackage> bodies) {
    List<OraclePackage> mergedPackages = new ArrayList<>();
    
    // Process each spec, looking for matching body
    for (OraclePackage spec : specs) {
      OraclePackage matchingBody = findMatchingBody(spec, bodies);
      
      if (matchingBody != null) {
        // Merge spec and body
        OraclePackage merged = createMergedPackage(spec, matchingBody);
        mergedPackages.add(merged);
      } else {
        // Spec only (no matching body)
        mergedPackages.add(spec);
      }
    }
    
    // Add any body-only packages (bodies without matching specs)
    for (OraclePackage body : bodies) {
      if (findMatchingSpec(body, specs) == null) {
        mergedPackages.add(body);
      }
    }
    
    return mergedPackages;
  }
  
  /**
   * Creates a merged package from spec and body, combining all components.
   */
  private static OraclePackage createMergedPackage(OraclePackage spec, OraclePackage body) {
    return new OraclePackage(
      spec.getName(),
      spec.getSchema(),
      mergeVariables(spec.getVariables(), body.getVariables()),
      mergeSubTypes(spec.getSubtypes(), body.getSubtypes()),
      mergeCursors(spec.getCursors(), body.getCursors()),
      mergePackageTypes(spec.getTypes(), body.getTypes()),
      mergeRecordTypes(spec.getRecordTypes(), body.getRecordTypes()),
      mergeVarrayTypes(spec.getVarrayTypes(), body.getVarrayTypes()),
      mergeNestedTableTypes(spec.getNestedTableTypes(), body.getNestedTableTypes()),
      mergeFunctions(spec.getFunctions(), body.getFunctions()),
      mergeProcedures(spec.getProcedures(), body.getProcedures()),
      body.getBodyStatements() // Body statements only exist in body
    );
  }
  
  /**
   * Finds matching body for a given spec.
   */
  private static OraclePackage findMatchingBody(OraclePackage spec, List<OraclePackage> bodies) {
    return bodies.stream()
      .filter(body -> spec.getName().equals(body.getName()) && spec.getSchema().equals(body.getSchema()))
      .findFirst()
      .orElse(null);
  }
  
  /**
   * Finds matching spec for a given body.
   */
  private static OraclePackage findMatchingSpec(OraclePackage body, List<OraclePackage> specs) {
    return specs.stream()
      .filter(spec -> body.getName().equals(spec.getName()) && body.getSchema().equals(spec.getSchema()))
      .findFirst()
      .orElse(null);
  }

  /**
   * Merges variables from spec and body, avoiding duplicates by name.
   * Variables from both spec and body are included.
   */
  private static List<Variable> mergeVariables(List<Variable> specVariables, List<Variable> bodyVariables) {
    List<Variable> merged = new ArrayList<>();
    Set<String> variableNames = new HashSet<>();
    
    // Add spec variables first
    if (specVariables != null) {
      for (Variable var : specVariables) {
        if (variableNames.add(var.getName())) {
          merged.add(var);
        }
      }
    }
    
    // Add body variables (avoid duplicates)
    if (bodyVariables != null) {
      for (Variable var : bodyVariables) {
        if (variableNames.add(var.getName())) {
          merged.add(var);
        }
      }
    }
    
    return merged;
  }
  
  /**
   * Merges subtypes from spec and body, avoiding duplicates by name.
   */
  private static List<SubType> mergeSubTypes(List<SubType> specSubTypes, List<SubType> bodySubTypes) {
    List<SubType> merged = new ArrayList<>();
    Set<String> subTypeNames = new HashSet<>();
    
    // Add spec subtypes first
    if (specSubTypes != null) {
      for (SubType subType : specSubTypes) {
        if (subTypeNames.add(subType.getName())) {
          merged.add(subType);
        }
      }
    }
    
    // Add body subtypes (avoid duplicates)
    if (bodySubTypes != null) {
      for (SubType subType : bodySubTypes) {
        if (subTypeNames.add(subType.getName())) {
          merged.add(subType);
        }
      }
    }
    
    return merged;
  }
  
  /**
   * Merges cursors from spec and body, avoiding duplicates by name.
   */
  private static List<Cursor> mergeCursors(List<Cursor> specCursors, List<Cursor> bodyCursors) {
    List<Cursor> merged = new ArrayList<>();
    Set<String> cursorNames = new HashSet<>();
    
    // Add spec cursors first
    if (specCursors != null) {
      for (Cursor cursor : specCursors) {
        if (cursorNames.add(cursor.getName())) {
          merged.add(cursor);
        }
      }
    }
    
    // Add body cursors (avoid duplicates)
    if (bodyCursors != null) {
      for (Cursor cursor : bodyCursors) {
        if (cursorNames.add(cursor.getName())) {
          merged.add(cursor);
        }
      }
    }
    
    return merged;
  }
  
  /**
   * Merges package types from spec and body, avoiding duplicates by name.
   */
  private static List<PackageType> mergePackageTypes(List<PackageType> specTypes, List<PackageType> bodyTypes) {
    List<PackageType> merged = new ArrayList<>();
    Set<String> typeNames = new HashSet<>();
    
    // Add spec types first
    if (specTypes != null) {
      for (PackageType type : specTypes) {
        if (typeNames.add(type.getName())) {
          merged.add(type);
        }
      }
    }
    
    // Add body types (avoid duplicates)
    if (bodyTypes != null) {
      for (PackageType type : bodyTypes) {
        if (typeNames.add(type.getName())) {
          merged.add(type);
        }
      }
    }
    
    return merged;
  }
  
  /**
   * Returns functions from body only.
   * Package specs contain only declarations - implementations are in body.
   * Since everything is considered public in PostgreSQL, we only need body implementations.
   */
  private static List<Function> mergeFunctions(List<Function> specFunctions, List<Function> bodyFunctions) {
    // Only return body functions (they have implementations)
    // Spec functions are just declarations without implementation
    return bodyFunctions != null ? bodyFunctions : new ArrayList<>();
  }
  
  /**
   * Returns procedures from body only.
   * Package specs contain only declarations - implementations are in body.
   * Since everything is considered public in PostgreSQL, we only need body implementations.
   */
  private static List<Procedure> mergeProcedures(List<Procedure> specProcedures, List<Procedure> bodyProcedures) {
    // Only return body procedures (they have implementations)
    // Spec procedures are just declarations without implementation  
    return bodyProcedures != null ? bodyProcedures : new ArrayList<>();
  }

  /**
   * Merges record types from spec and body, avoiding duplicates by name.
   * Record types can be declared in both spec and body.
   */
  private static List<RecordType> mergeRecordTypes(List<RecordType> specRecordTypes, List<RecordType> bodyRecordTypes) {
    List<RecordType> merged = new ArrayList<>();
    Set<String> recordTypeNames = new HashSet<>();
    
    // Add spec record types first (they are declarations)
    if (specRecordTypes != null) {
      for (RecordType recordType : specRecordTypes) {
        if (recordTypeNames.add(recordType.getName())) {
          merged.add(recordType);
        }
      }
    }
    
    // Add body record types (avoid duplicates)
    if (bodyRecordTypes != null) {
      for (RecordType recordType : bodyRecordTypes) {
        if (recordTypeNames.add(recordType.getName())) {
          merged.add(recordType);
        }
      }
    }
    
    return merged;
  }

  private static List<VarrayType> mergeVarrayTypes(List<VarrayType> specVarrayTypes, List<VarrayType> bodyVarrayTypes) {
    List<VarrayType> merged = new ArrayList<>();
    Set<String> varrayTypeNames = new HashSet<>();
    
    // Add spec varray types first (they are declarations)
    if (specVarrayTypes != null) {
      for (VarrayType varrayType : specVarrayTypes) {
        if (varrayTypeNames.add(varrayType.getName())) {
          merged.add(varrayType);
        }
      }
    }
    
    // Add body varray types (avoid duplicates)
    if (bodyVarrayTypes != null) {
      for (VarrayType varrayType : bodyVarrayTypes) {
        if (varrayTypeNames.add(varrayType.getName())) {
          merged.add(varrayType);
        }
      }
    }
    
    return merged;
  }

  private static List<NestedTableType> mergeNestedTableTypes(List<NestedTableType> specNestedTableTypes, List<NestedTableType> bodyNestedTableTypes) {
    List<NestedTableType> merged = new ArrayList<>();
    Set<String> nestedTableTypeNames = new HashSet<>();
    
    // Add spec nested table types first (they are declarations)
    if (specNestedTableTypes != null) {
      for (NestedTableType nestedTableType : specNestedTableTypes) {
        if (nestedTableTypeNames.add(nestedTableType.getName())) {
          merged.add(nestedTableType);
        }
      }
    }
    
    // Add body nested table types (avoid duplicates)
    if (bodyNestedTableTypes != null) {
      for (NestedTableType nestedTableType : bodyNestedTableTypes) {
        if (nestedTableTypeNames.add(nestedTableType.getName())) {
          merged.add(nestedTableType);
        }
      }
    }
    
    return merged;
  }
}
