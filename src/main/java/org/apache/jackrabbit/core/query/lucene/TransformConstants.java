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
package org.apache.jackrabbit.core.query.lucene;

/**
 * <code>TransformConstants</code> defines constants for query processing.
 */
public interface TransformConstants {

    /**
     * No transformation is done on the term enum.
     */
    static final int TRANSFORM_NONE = 0;

    /**
     * The underlying term enum is transformed to lower case characters.
     */
    static final int TRANSFORM_LOWER_CASE = 1;

    /**
     * The underlying term enum is transformed to upper case characters.
     */
    static final int TRANSFORM_UPPER_CASE = 2;

}