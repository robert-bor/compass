package org.compass.core.converter.basic;

import org.compass.core.accessor.AccessorUtils;
import org.compass.core.converter.ConversionException;
import org.compass.core.mapping.ResourcePropertyMapping;
import org.compass.core.mapping.osem.ClassPropertyMetaDataMapping;
import org.compass.core.marshall.MarshallingContext;

/**
 * @author kimchy
 */
public class EnumConverter extends AbstractBasicConverter {

    protected Object doFromString(String str, ResourcePropertyMapping resourcePropertyMapping, MarshallingContext context) throws ConversionException {
        ClassPropertyMetaDataMapping metaDataMapping = (ClassPropertyMetaDataMapping) resourcePropertyMapping;
        // if this is an EnumSet, get the collection parameter
        // TODO we can do better than check it every time, we should store it on the mapping level
        Class enumType = AccessorUtils.getCollectionParameter(metaDataMapping.getGetter());
        // if it is not, just use the actual type and assume it is the actual enum
        if (enumType == null) {
            enumType = (Class) metaDataMapping.getGetter().getReturnType();
        }
        return Enum.valueOf(enumType, str);
    }

    protected String doToString(Object o, ResourcePropertyMapping resourcePropertyMapping, MarshallingContext context) {
        return ((Enum) o).name();
    }
}
