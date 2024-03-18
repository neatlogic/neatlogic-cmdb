/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.utils;

import neatlogic.framework.cmdb.annotation.ResourceField;
import neatlogic.framework.cmdb.annotation.ResourceType;
import neatlogic.framework.cmdb.annotation.ResourceTypes;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.EntityField;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

/**
 * @author linbq
 * @since 2022/2/9 14:17
 **/
public class ResourceEntityFactory {
    private static Logger logger = LoggerFactory.getLogger(ResourceEntityFactory.class);
    /**
     * 视图名称与字段列表映射关系
     */
    private static Map<String, List<ValueTextVo>> fieldMap = new HashMap<>();
    private static List<ResourceEntityVo> resourceEntityList = new ArrayList<>();
    /**
     * 视图信息列表
     */
    private static List<SceneEntityVo> sceneEntityList = new ArrayList<>();
    /**
     * 视图名称列表
     */
    private static List<String> viewNameList = new ArrayList<>();

//    static {
//        Reflections ref = new Reflections("neatlogic.framework.cmdb.dto.resourcecenter.entity", new TypeAnnotationsScanner(), new SubTypesScanner(true));
//        Set<Class<?>> classList = ref.getTypesAnnotatedWith(ResourceType.class, true);
//        for (Class<?> c : classList) {
//            ResourceEntityVo resourceEntityVo = null;
//            Annotation[] classAnnotations = c.getDeclaredAnnotations();
//            for (Annotation annotation : classAnnotations) {
//                if (annotation instanceof ResourceType) {
//                    ResourceType rt = (ResourceType) annotation;
//                    resourceEntityVo = new ResourceEntityVo();
//                    resourceEntityVo.setName(rt.name());
//                    resourceEntityVo.setLabel(rt.label());
//                }
//            }
//            if (resourceEntityVo == null) {
//                continue;
//            }
//            for (Field field : c.getDeclaredFields()) {
//                Annotation[] annotations = field.getDeclaredAnnotations();
//                for (Annotation annotation : annotations) {
//                    if (annotation instanceof ResourceField) {
//                        ResourceField rf = (ResourceField) annotation;
//                        if (StringUtils.isNotBlank(rf.name())) {
//                            ResourceEntityAttrVo attr = new ResourceEntityAttrVo();
//                            attr.setField(rf.name());
//                            resourceEntityVo.addAttr(attr);
//                        }
//                    }
//                }
//            }
//            resourceEntityVo.setType(ViewType.RESOURCE.getValue());
//            resourceEntityList.add(resourceEntityVo);
//        }
//        classList = ref.getTypesAnnotatedWith(ResourceTypes.class, true);
//        for (Class<?> c : classList) {
//            ResourceTypes resourceTypes = c.getAnnotation(ResourceTypes.class);
//            if (resourceTypes != null) {
//                for (ResourceType rt : resourceTypes.value()) {
//                    ResourceEntityVo resourceEntityVo = new ResourceEntityVo();
//                    resourceEntityVo.setName(rt.name());
//                    resourceEntityVo.setLabel(rt.label());
//                    for (Field field : c.getDeclaredFields()) {
//                        ResourceField rf = field.getAnnotation(ResourceField.class);
//                        if (rf != null) {
//                            if (StringUtils.isNotBlank(rf.name())) {
//                                ResourceEntityAttrVo attr = new ResourceEntityAttrVo();
//                                attr.setField(rf.name());
//                                resourceEntityVo.addAttr(attr);
//                            }
//                        }
//                    }
//                    resourceEntityVo.setType(ViewType.RESOURCE.getValue());
//                    resourceEntityList.add(resourceEntityVo);
//                }
//            }
//        }
//    }
    static {
        Reflections ref = new Reflections("neatlogic.framework.cmdb.dto.resourcecenter.sceneviewfielddeclare");
        Set<Class<?>> classList = ref.getTypesAnnotatedWith(ResourceType.class, true);
        for (Class<?> c : classList) {
            SceneEntityVo sceneEntityVo = null;
            Annotation[] classAnnotations = c.getDeclaredAnnotations();
            for (Annotation annotation : classAnnotations) {
                if (annotation instanceof ResourceType) {
                    ResourceType rt = (ResourceType) annotation;
                    if (viewNameList.contains(rt.name())) {
                        logger.error("view '" + rt.name() + "' repeats the declaration");
                        System.exit(1);
                    }
                    viewNameList.add(rt.name());
                    sceneEntityVo = new SceneEntityVo();
                    sceneEntityVo.setName(rt.name());
                    sceneEntityVo.setLabel(rt.label());
                    sceneEntityVo.setDescription(String.join("；", rt.functionPathList()));
                }
            }
            if (sceneEntityVo == null) {
                continue;
            }
            for (Field field : c.getDeclaredFields()) {
                ResourceField rf = field.getAnnotation(ResourceField.class);
                if (rf != null) {
                    if (StringUtils.isNotBlank(rf.name())) {
                        EntityField ef = field.getAnnotation(EntityField.class);
                        fieldMap.computeIfAbsent(sceneEntityVo.getName(), key -> new ArrayList<>()).add(new ValueTextVo(rf.name(), ef.name()));
                    }
                }
//                Annotation[] annotations = field.getDeclaredAnnotations();
//                for (Annotation annotation : annotations) {
//                    if (annotation instanceof ResourceField) {
//                        ResourceField rf = (ResourceField) annotation;
//                        if (StringUtils.isNotBlank(rf.name())) {
//                            fieldMap.computeIfAbsent(sceneEntityVo.getName(), key -> new ArrayList<>()).add(new ValueTextVo(rf.name(), "aaa"));
//                        }
//                    }
//                }
            }
            sceneEntityList.add(sceneEntityVo);
        }
        classList = ref.getTypesAnnotatedWith(ResourceTypes.class, true);
        for (Class<?> c : classList) {
            ResourceTypes resourceTypes = c.getAnnotation(ResourceTypes.class);
            if (resourceTypes != null) {
                for (ResourceType rt : resourceTypes.value()) {
                    if (viewNameList.contains(rt.name())) {
                        logger.error("view '" + rt.name() + "' repeats the declaration");
                        System.exit(1);
                    }
                    viewNameList.add(rt.name());
                    SceneEntityVo sceneEntityVo = new SceneEntityVo();
                    sceneEntityVo.setName(rt.name());
                    sceneEntityVo.setLabel(rt.label());
                    sceneEntityVo.setDescription(String.join("；", rt.functionPathList()));
                    for (Field field : c.getDeclaredFields()) {
                        ResourceField rf = field.getAnnotation(ResourceField.class);
                        if (rf != null) {
                            if (StringUtils.isNotBlank(rf.name())) {
                                EntityField ef = field.getAnnotation(EntityField.class);
                                fieldMap.computeIfAbsent(sceneEntityVo.getName(), key -> new ArrayList<>()).add(new ValueTextVo(rf.name(), ef.name()));
                            }
                        }
                    }
                    sceneEntityList.add(sceneEntityVo);
                }
            }
        }
        String[] viewNameArray = new String[viewNameList.size()];
        viewNameList.toArray(viewNameArray);
        Arrays.sort(viewNameArray);
        viewNameList = Arrays.asList(viewNameArray);
        sceneEntityList.sort(Comparator.comparingInt(e -> viewNameList.indexOf(e.getName())));
    }

    public static List<ResourceEntityVo> getResourceEntityList() {
        return resourceEntityList;
    }

    public static List<SceneEntityVo> getSceneEntityList() {
        return sceneEntityList;
    }

    public static SceneEntityVo getSceneEntityByViewName(String viewName) {
        SceneEntityVo sceneEntityVo = null;
        for (SceneEntityVo sceneEntity : sceneEntityList) {
            if (Objects.equals(viewName, sceneEntity.getName())) {
                sceneEntityVo = new SceneEntityVo();
                sceneEntityVo.setName(sceneEntity.getName());
                sceneEntityVo.setLabel(sceneEntity.getLabel());
                sceneEntityVo.setDescription(sceneEntity.getDescription());
            }
        }
        return sceneEntityVo;
    }
    public static List<String> getFieldNameListByViewName(String viewName) {
        List<ValueTextVo> fieldList = fieldMap.get(viewName);
        if (fieldList == null) {
            return new ArrayList<>();
        }
        List<String> fieldNameList = new ArrayList<>();
        for (ValueTextVo valueTextVo : fieldList) {
            fieldNameList.add(valueTextVo.getValue().toString());
        }
        return fieldNameList;
    }

    public static List<ValueTextVo> getFieldListByViewName(String viewName) {
        List<ValueTextVo> fieldList = fieldMap.get(viewName);
        if (fieldList == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(fieldList);
    }

    public static List<String> getViewNameList() {
        return new ArrayList<>(viewNameList);
    }
}
