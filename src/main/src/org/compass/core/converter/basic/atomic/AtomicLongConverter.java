/*
 * Copyright 2004-2009 the original author or authors.
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

package org.compass.core.converter.basic.atomic;

import java.text.ParseException;
import java.util.concurrent.atomic.AtomicLong;

import org.compass.core.converter.basic.AbstractNumberConverter;
import org.compass.core.converter.basic.format.Formatter;
import org.compass.core.converter.basic.format.NumberUtils;
import org.compass.core.mapping.ResourcePropertyMapping;

/**
 * @author kimchy
 */
public class AtomicLongConverter extends AbstractNumberConverter<AtomicLong> {

    protected AtomicLong defaultFromString(String str, ResourcePropertyMapping resourcePropertyMapping) {
        return new AtomicLong(Long.valueOf(str));
    }

    protected AtomicLong fromNumber(Number number) {
        return new AtomicLong(number.longValue());
    }

    protected Formatter createSortableFormatter() {
        return new Formatter() {
            public String format(Object obj) {
                long val = ((Number) obj).longValue();
                return NumberUtils.long2sortableStr(val);
            }

            public Object parse(String str) throws ParseException {
                return NumberUtils.SortableStr2long(str);
            }

            public boolean isThreadSafe() {
                return true;
            }
        };
    }
}