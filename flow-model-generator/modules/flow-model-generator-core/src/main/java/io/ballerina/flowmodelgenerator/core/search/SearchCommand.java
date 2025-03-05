/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.core.search;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.ballerina.flowmodelgenerator.core.model.Category;
import io.ballerina.flowmodelgenerator.core.model.Item;
import io.ballerina.modelgenerator.commons.SearchDatabaseManager;
import io.ballerina.modelgenerator.commons.SearchResult;
import io.ballerina.projects.Module;
import io.ballerina.tools.text.LineRange;

import java.util.List;
import java.util.Map;

/**
 * Abstract base class for search command operations that handles different types of searches in a module context. This
 * class provides the foundation for specific search implementations for the search API.
 *
 * <p>Search commands can be created using the factory method {@link #from(Kind, Module, LineRange, Map)}
 * which returns the appropriate implementation based on the specified kind. The class follows the command design
 * pattern allowing to execute various search strategies depending on the commands.</p>
 *
 * @since 2.0.0
 */
public abstract class SearchCommand {

    protected final Category.Builder rootBuilder;
    protected final Module module;
    protected final LineRange position;
    protected final String query;
    protected final int limit;
    protected final int offset;
    final SearchDatabaseManager dbManager;
    final DefaultViewHolder defaultViewHolder;

    protected static final String DATA_MAPPER_FILE_NAME = "data_mappings.bal";
    private static final Gson GSON = new Gson();

    private static final int DEFAULT_LIMIT = 20;
    private static final int DEFAULT_OFFSET = 0;

    public static SearchCommand from(Kind kind, Module module, LineRange position, Map<String, String> queryMap) {
        return switch (kind) {
            case FUNCTION -> new FunctionSearchCommand(module, position, queryMap);
            case CONNECTOR -> new ConnectorSearchCommand(module, position, queryMap);
            case TYPE -> new TypeSearchCommand(module, position, queryMap);
        };
    }

    public SearchCommand(Module module, LineRange position, Map<String, String> queryMap) {
        this.rootBuilder = new Category.Builder(null);
        this.module = module;
        this.position = position;
        this.dbManager = SearchDatabaseManager.getInstance();
        this.defaultViewHolder = DefaultViewHolder.getInstance();

        if (queryMap == null) {
            this.query = "";
            this.limit = DEFAULT_LIMIT;
            this.offset = DEFAULT_OFFSET;
        } else {
            this.query = queryMap.getOrDefault("q", "");
            this.limit = parseIntParam(queryMap.get("limit"), DEFAULT_LIMIT);
            this.offset = parseIntParam(queryMap.get("offset"), DEFAULT_OFFSET);
        }
    }

    /**
     * Returns the default view of search results.
     *
     * @return List of search results
     */
    protected abstract List<Item> defaultView();

    /**
     * Performs a search with the given query parameters.
     *
     * @return List of search results
     */
    protected abstract List<Item> search();

    /**
     * Fetches the popular items if not cached already.
     *
     * @return a list of popular search results
     */
    protected abstract Map<String, List<SearchResult>> fetchPopularItems();

    /**
     * Executes the search based on the current search parameters.
     *
     * @return List of search results
     */
    public JsonArray execute() {
        List<Item> items = query.isEmpty() ? defaultView() : search();
        return GSON.toJsonTree(items).getAsJsonArray();
    }

    /**
     * Utility method to parse string parameters to integers with default values.
     *
     * @param value        The string value to parse
     * @param defaultValue Default value to use if parsing fails
     * @return The parsed integer or default value
     */
    private static int parseIntParam(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public enum Kind {
        FUNCTION,
        CONNECTOR,
        TYPE
    }
}
