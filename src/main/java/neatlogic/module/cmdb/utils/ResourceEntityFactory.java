/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.cmdb.utils;

import neatlogic.framework.cmdb.annotation.ResourceField;
import neatlogic.framework.cmdb.annotation.ResourceType;
import neatlogic.framework.cmdb.annotation.ResourceTypes;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityAttrVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.enums.resourcecenter.ViewType;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author linbq
 * @since 2022/2/9 14:17
 **/
public class ResourceEntityFactory {
    private static List<ResourceEntityVo> resourceEntityList = new ArrayList<>();

    static {
        Reflections ref = new Reflections("neatlogic.framework.cmdb.dto.resourcecenter.entity", new TypeAnnotationsScanner(), new SubTypesScanner(true));
        Set<Class<?>> classList = ref.getTypesAnnotatedWith(ResourceType.class, true);
        for (Class<?> c : classList) {
            ResourceEntityVo resourceEntityVo = null;
            Annotation[] classAnnotations = c.getDeclaredAnnotations();
            for (Annotation annotation : classAnnotations) {
                if (annotation instanceof ResourceType) {
                    ResourceType rt = (ResourceType) annotation;
                    resourceEntityVo = new ResourceEntityVo();
                    resourceEntityVo.setName(rt.name());
                    resourceEntityVo.setLabel(rt.label());
                }
            }
            if (resourceEntityVo == null) {
                continue;
            }
            for (Field field : c.getDeclaredFields()) {
                Annotation[] annotations = field.getDeclaredAnnotations();
                for (Annotation annotation : annotations) {
                    if (annotation instanceof ResourceField) {
                        ResourceField rf = (ResourceField) annotation;
                        if (StringUtils.isNotBlank(rf.name())) {
                            ResourceEntityAttrVo attr = new ResourceEntityAttrVo();
                            attr.setField(rf.name());
                            resourceEntityVo.addAttr(attr);
                        }
                    }
                }
            }
            resourceEntityVo.setType(ViewType.RESOURCE.getValue());
            resourceEntityList.add(resourceEntityVo);
        }
        classList = ref.getTypesAnnotatedWith(ResourceTypes.class, true);
        for (Class<?> c : classList) {
            ResourceTypes resourceTypes = c.getAnnotation(ResourceTypes.class);
            if (resourceTypes != null) {
                for (ResourceType rt : resourceTypes.value()) {
                    ResourceEntityVo resourceEntityVo = new ResourceEntityVo();
                    resourceEntityVo.setName(rt.name());
                    resourceEntityVo.setLabel(rt.label());
                    for (Field field : c.getDeclaredFields()) {
                        ResourceField rf = field.getAnnotation(ResourceField.class);
                        if (rf != null) {
                            if (StringUtils.isNotBlank(rf.name())) {
                                ResourceEntityAttrVo attr = new ResourceEntityAttrVo();
                                attr.setField(rf.name());
                                resourceEntityVo.addAttr(attr);
                            }
                        }
                    }
                    resourceEntityVo.setType(ViewType.RESOURCE.getValue());
                    resourceEntityList.add(resourceEntityVo);
                }
            }
        }
    }

    public static List<ResourceEntityVo> getResourceEntityList() {
        return resourceEntityList;
    }
}
