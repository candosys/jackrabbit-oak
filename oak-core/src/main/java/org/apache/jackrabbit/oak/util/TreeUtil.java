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
package org.apache.jackrabbit.oak.util;

import static org.apache.jackrabbit.oak.api.Type.BOOLEAN;
import static org.apache.jackrabbit.oak.api.Type.STRINGS;

import javax.annotation.CheckForNull;

import com.google.common.collect.Iterables;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.commons.PathUtils;
import org.apache.jackrabbit.util.Text;

/**
 * Utility providing common operations for the {@code Tree} that are not provided
 * by the API.
 */
public final class TreeUtil {

    private TreeUtil() {
    }

    @CheckForNull
    public static String getPrimaryTypeName(Tree tree) {
        return getStringInternal(tree, JcrConstants.JCR_PRIMARYTYPE, Type.NAME);
    }

    @CheckForNull
    public static String[] getStrings(Tree tree, String propertyName) {
        PropertyState property = tree.getProperty(propertyName);
        if (property == null) {
            return null;
        } else {
            return Iterables.toArray(property.getValue(STRINGS), String.class);
        }
    }

    @CheckForNull
    public static String getString(Tree tree, String propertyName) {
        return getStringInternal(tree, propertyName, Type.STRING);
    }

    @CheckForNull
    private static String getStringInternal(Tree tree,
                                            String propertyName,
                                            Type<String> type) {
        PropertyState property = tree.getProperty(propertyName);
        if (property != null && !property.isArray()) {
            return property.getValue(type);
        } else {
            return null;
        }
    }

    /**
     * Returns the boolean representation of the property with the specified
     * {@code propertyName}. If the property does not exist or
     * {@link org.apache.jackrabbit.oak.api.PropertyState#isArray() is an array}
     * this method returns {@code false}.
     *
     * @param tree         The target tree.
     * @param propertyName The name of the property.
     * @return the boolean representation of the property state with the given
     *         name. This utility returns {@code false} if the property does not exist
     *         or is an multivalued property.
     */
    public static boolean getBoolean(Tree tree, String propertyName) {
        PropertyState property = tree.getProperty(propertyName);
        return property != null && !property.isArray() && property.getValue(BOOLEAN);
    }

    /**
     * Return the possibly non existing tree located at the passed {@code path} from
     * the location of the start {@code tree} or {@code null} if {@code path} results
     * in a parent of the root.
     *
     * @param tree  start tree
     * @param path  path from the start tree
     * @return  tree located at {@code path} from {@code start} or {@code null}
     */
    @CheckForNull
    public static Tree getTree(Tree tree, String path) {
        for (String element : Text.explode(path, '/', false)) {
            if (PathUtils.denotesParent(element)) {
                if (tree.isRoot()) {
                    return null;
                } else {
                    tree = tree.getParent();
                }
            } else if (!PathUtils.denotesCurrent(element)) {
                tree = tree.getChild(element);
            }  // else . -> skip to next element
        }
        return tree;
    }

}