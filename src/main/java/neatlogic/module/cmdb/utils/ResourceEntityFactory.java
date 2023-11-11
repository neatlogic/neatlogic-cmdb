/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

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
