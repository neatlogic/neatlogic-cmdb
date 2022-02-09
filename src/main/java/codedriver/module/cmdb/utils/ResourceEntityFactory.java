/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.utils;

import codedriver.framework.cmdb.annotation.ResourceField;
import codedriver.framework.cmdb.annotation.ResourceType;
import codedriver.framework.cmdb.annotation.ResourceTypes;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityAttrVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
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
        Reflections ref = new Reflections("codedriver.framework.cmdb.dto.resourcecenter.entity", new TypeAnnotationsScanner(), new SubTypesScanner(true));
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
                    resourceEntityList.add(resourceEntityVo);
                }
            }
        }
    }

    public static List<ResourceEntityVo> getResourceEntityList() {
        return resourceEntityList;
    }
}
