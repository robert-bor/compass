/*
 * Copyright 2004-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.compass.core.mapping;

import org.compass.core.Property;
import org.compass.core.util.Parameter;

/**
 * @author kimchy
 */
public interface ResourcePropertyMapping extends Mapping {

    public static class ReverseType extends Parameter {

        private static final long serialVersionUID = 9135849961654313364L;

        protected ReverseType(String name) {
            super(name);
        }

        public static final ReverseType NO = new ReverseType("NO");

        public static final ReverseType READER = new ReverseType("READER");

        public static final ReverseType STRING = new ReverseType("STRING");

        public static ReverseType fromString(String reverseType) {
            if ("no".equalsIgnoreCase(reverseType)) {
                return ReverseType.NO;
            } else if ("reader".equalsIgnoreCase(reverseType)) {
                return ReverseType.READER;
            } else if ("string".equalsIgnoreCase(reverseType)) {
                return ReverseType.STRING;
            }
            throw new IllegalArgumentException("Can't find reverse type for [" + reverseType + "]");
        }
    }

    public static class ExcludeFromAllType extends Parameter {

        protected ExcludeFromAllType(String name) {
            super(name);
        }

        public static final ExcludeFromAllType NO = new ExcludeFromAllType("NO");

        public static final ExcludeFromAllType YES = new ExcludeFromAllType("YES");

        public static final ExcludeFromAllType NO_ANALYZED = new ExcludeFromAllType("NO_ANALYZED");

        public static ExcludeFromAllType fromString(String excludeFromAllType) {
            if ("no".equalsIgnoreCase(excludeFromAllType) || "false".equalsIgnoreCase(excludeFromAllType)) {
                return ExcludeFromAllType.NO;
            } else if ("yes".equalsIgnoreCase(excludeFromAllType) || "true".equalsIgnoreCase(excludeFromAllType)) {
                return ExcludeFromAllType.YES;
            } else if ("no_analyzed".equalsIgnoreCase(excludeFromAllType)) {
                return ExcludeFromAllType.NO_ANALYZED;
            }
            throw new IllegalArgumentException("Can't find exclude from all type for [" + excludeFromAllType + "]");
        }

        public static String toString(ExcludeFromAllType excludeFromAllType) {
            if (excludeFromAllType == NO) {
                return "no";
            }
            if (excludeFromAllType == NO_ANALYZED) {
                return "no_analyzed";
            }
            if (excludeFromAllType == YES) {
                return "yes";
            }
            return "no";
        }
    }

    public static class SpellCheckType extends Parameter {

        protected SpellCheckType(String name) {
            super(name);
        }

        public static final SpellCheckType INCLUDE = new SpellCheckType("INCLUDE");

        public static final SpellCheckType EXCLUDE = new SpellCheckType("EXCLUDE");

        public static SpellCheckType fromString(String spellCheckType) {
            if ("include".equalsIgnoreCase(spellCheckType)) {
                return SpellCheckType.INCLUDE;
            } else if ("exclude".equalsIgnoreCase(spellCheckType)) {
                return SpellCheckType.EXCLUDE;
            }
            throw new IllegalArgumentException("Can't find spell check type for [" + spellCheckType + "]");
        }

        public static String toString(SpellCheckType spellCheckType) {
            if (spellCheckType == INCLUDE) {
                return "include";
            }
            if (spellCheckType == EXCLUDE) {
                return "exlcude";
            }
            return "exclude";
        }
    }

    /**
     * Returns the anayzer name that is associated with the property.
     * Can be <code>null</code> (i.e. not set).
     */
    String getAnalyzer();

    /**
     * Returns the root resource mapping alias name this resource property mapping belongs to.
     */
    String getRootAlias();

    /**
     * Returns <code>true</code> if this mapping is an internal one (<code>$/</code> notation).
     */
    boolean isInternal();

    float getBoost();

    boolean isOmitNorms();

    ExcludeFromAllType getExcludeFromAll();

    SpellCheckType getSpellCheck();

    Property.Store getStore();

    Property.Index getIndex();

    Property.TermVector getTermVector();

    ReverseType getReverse();

    String getNullValue();

    boolean hasNullValue();
}
