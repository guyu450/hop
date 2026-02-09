/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.plugins.metadata.filter;

import org.apache.hop.core.extension.ExtensionPoint;
import org.apache.hop.core.extension.IExtensionPoint;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.vfs.HopVfs;
import org.apache.hop.ui.hopgui.perspective.metadata.MetadataPerspective;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@ExtensionPoint(
    id = "ConfigurableMetadataFilter",
    extensionPointId = "MetadataTypeFilter",
    description = "Filter metadata types based on configuration file")
public class ConfigurableMetadataFilter implements IExtensionPoint<MetadataPerspective.MetadataTypeFilter> {

  private static final String CONFIG_FILE_NAME = "metadata-filter-config.json";
  private static final String DEFAULT_CONFIG_PATH = "config/" + CONFIG_FILE_NAME;
  
  private Set<String> excludedMetadataKeys;
  private Set<String> includedMetadataKeys;
  private boolean initialized = false;

  @Override
  public void callExtensionPoint(ILogChannel log, IVariables variables, MetadataPerspective.MetadataTypeFilter filter) {
    // 初始化配置
    if (!initialized) {
      initializeConfig(log, variables);
    }

    // 应用过滤规则
    applyFilter(filter);
  }

  private void initializeConfig(ILogChannel log, IVariables variables) {
    excludedMetadataKeys = new HashSet<>();
    includedMetadataKeys = new HashSet<>();

    try {
      // 尝试从环境变量获取配置文件路径
      String configPath = variables.getVariable("HOP_METADATA_FILTER_CONFIG");
      if (Utils.isEmpty(configPath)) {
        configPath = DEFAULT_CONFIG_PATH;
      }

      // 加载配置文件
      log.logBasic("Loading metadata filter configuration from: " + configPath);
      
      try (InputStream inputStream = HopVfs.getInputStream(configPath)) {
        if (inputStream != null) {
          // 解析JSON配置
          ObjectMapper mapper = new ObjectMapper();
          JsonNode rootNode = mapper.readTree(inputStream);

          // 加载排除的元数据类型
          JsonNode excludedNode = rootNode.get("excluded");
          if (excludedNode != null && excludedNode.isArray()) {
            for (JsonNode node : excludedNode) {
              excludedMetadataKeys.add(node.asText());
            }
          }

          // 加载包含的元数据类型
          JsonNode includedNode = rootNode.get("included");
          if (includedNode != null && includedNode.isArray()) {
            for (JsonNode node : includedNode) {
              includedMetadataKeys.add(node.asText());
            }
          }

          log.logBasic("Metadata filter configuration loaded:");
          log.logBasic("Excluded metadata types: " + excludedMetadataKeys);
          log.logBasic("Included metadata types: " + includedMetadataKeys);
        } else {
          log.logBasic("Metadata filter configuration file not found: " + configPath);
          log.logBasic("Using default configuration (include all metadata types)");
        }
      }
    } catch (Exception e) {
      log.logError("Error loading metadata filter configuration: " + e.getMessage());
      // 如果配置加载失败，默认包含所有元数据类型
    }

    initialized = true;
  }

  private void applyFilter(MetadataPerspective.MetadataTypeFilter filter) {
    String metadataKey = filter.getMetadataKey();

    // 首先检查排除列表
    if (excludedMetadataKeys.contains(metadataKey)) {
      filter.setInclude(false);
      return;
    }

    // 然后检查包含列表（如果非空）
    if (!includedMetadataKeys.isEmpty() && !includedMetadataKeys.contains(metadataKey)) {
      filter.setInclude(false);
      return;
    }

    // 默认包含
    filter.setInclude(true);
  }
}
