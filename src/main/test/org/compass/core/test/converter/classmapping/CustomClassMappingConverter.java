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

package org.compass.core.test.converter.classmapping;

import org.compass.core.Property;
import org.compass.core.Resource;
import org.compass.core.converter.ConversionException;
import org.compass.core.converter.mapping.osem.ClassMappingConverter;
import org.compass.core.mapping.Mapping;
import org.compass.core.marshall.MarshallingContext;

/**
 * @author kimchy
 */
public class CustomClassMappingConverter extends ClassMappingConverter {

    protected boolean doMarshall(Resource resource, Object root, Mapping mapping, MarshallingContext context) throws ConversionException {
        boolean retVal = super.doMarshall(resource, root, mapping, context);
        resource.addProperty(context.getSearchEngine().createProperty("specialprop", "test", Property.Store.YES, Property.Index.TOKENIZED));
        return retVal;
    }
}
