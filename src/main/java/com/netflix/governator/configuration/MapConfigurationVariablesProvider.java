/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netflix.governator.configuration;

import com.google.common.collect.Maps;
import java.util.Map;

/**
 *
 * @author Mario Franco
 */
public class MapConfigurationVariablesProvider implements ConfigurationVariablesProvider {

    private final Map<String, String> variableValues;

    public MapConfigurationVariablesProvider(Map<String, String> variableValues) {
        this.variableValues = Maps.newHashMap(variableValues);
    }

    @Override
    public String put(String key, String value) {
        return this.variableValues.put(key, value);
    }

    @Override
    public String get(String key) {
        return this.variableValues.get(key);
    }
}
