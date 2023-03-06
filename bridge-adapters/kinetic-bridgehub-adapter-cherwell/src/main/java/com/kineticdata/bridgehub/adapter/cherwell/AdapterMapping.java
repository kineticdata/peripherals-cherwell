/*
 * This class is intened to become generic so that many agent adapters can leverage
 * the same functionality.  
 */
package com.kineticdata.bridgehub.adapter.cherwell;

import com.kineticdata.bridgehub.adapter.BridgeError;
import java.util.List;
import java.util.Map;

/**
 * This class defines valid Structures.
 * Properties:
 *  String structure - Name of the data model.
 *  PathBuilder pathBuilder - URL path to asset.  Defined in child class.
 *  String accessor - property name accessor when multiple results returned.
 */
public class AdapterMapping {
    private final String structure;
    private String accessor;
    private final PathBuilder pathbuilder;
    
    public AdapterMapping(String structure, String accessor, PathBuilder pathbuilder){
        this.accessor = accessor;
        this.structure = structure;
        this.pathbuilder = pathbuilder;
    }
    
    /**
     * Interfaces for mappings.
     */
    @FunctionalInterface
    public static interface PathBuilder {
        String apply(List<String> structureList, Map<String, String> parameters) 
           throws BridgeError;
    }

    /**
     * @return the structure
     */
    public String getStructure() {
        return structure;
    }

    /**
     * If request has multiple results the return object has a property key.
     * This accessor maps the property key to a Structure.
     * @return the accessor
     */
    public String getAccessor() {
        return accessor;
    }
    
    /**
     * @return the pathbuilder
     */
    public PathBuilder getPathbuilder() {
        return pathbuilder;
    }
}
