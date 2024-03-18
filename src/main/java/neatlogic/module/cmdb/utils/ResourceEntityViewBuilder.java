/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.*;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.enums.resourcecenter.JoinType;
import neatlogic.framework.cmdb.enums.resourcecenter.Status;
import neatlogic.framework.cmdb.enums.resourcecenter.ViewType;
import neatlogic.framework.cmdb.exception.attr.AttrNotFoundException;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterConfigIrregularException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterResourceFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterViewConfigException;
import neatlogic.framework.cmdb.utils.SceneEntityGenerateSqlUtil;
import neatlogic.framework.dao.mapper.DataBaseViewInfoMapper;
import neatlogic.framework.dao.mapper.SchemaMapper;
import neatlogic.framework.dto.DataBaseViewInfoVo;
import neatlogic.framework.transaction.core.EscapeTransactionJob;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.service.ci.CiService;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.SelectUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Deprecated
@Component
public class ResourceEntityViewBuilder {
    private final static Logger logger = LoggerFactory.getLogger(ResourceEntityViewBuilder.class);

    private final static List<String> defaultAttrList = Arrays.asList("_id", "_uuid", "_name", "_fcu", "_fcd", "_lcu", "_lcd", "_inspectStatus", "_inspectTime", "_monitorStatus", "_monitorTime", "_typeId", "_typeName", "_typeLabel");

    private ResourceEntityVo resourceEntityVo;

    private SceneEntityVo sceneEntityVo;

    private String type;

    private final Map<String, CiVo> ciMap = new HashMap<>();
    private static SchemaMapper schemaMapper;
    private static DataBaseViewInfoMapper dataBaseViewInfoMapper;
    private static CiService ciService;
    private static ResourceEntityMapper resourceEntityMapper;


    @Autowired
    public ResourceEntityViewBuilder(
            SchemaMapper _schemaMapper,
            CiService _ciService,
            ResourceEntityMapper _resourceEntityMapper,
            DataBaseViewInfoMapper _dataBaseViewInfoMapper) {
        schemaMapper = _schemaMapper;
        resourceEntityMapper = _resourceEntityMapper;
        ciService = _ciService;
        dataBaseViewInfoMapper = _dataBaseViewInfoMapper;
    }

    private CiVo getCiByName(String ciName) {
        if (!ciMap.containsKey(ciName)) {
            CiVo ciVo = ciService.getCiByName(ciName);
            ciMap.put(ciName, ciVo);
        }
        return ciMap.get(ciName);
    }


    private List<Element> getAllChildElement(Element fromElement, String elementName) {
        List<Element> elementList = fromElement.elements();
        List<Element> returnList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(elementList)) {
            for (Element element : elementList) {
                if (element.getName().equalsIgnoreCase(elementName)) {
                    returnList.add(element);
                }
                List<Element> tmpElementList = getAllChildElement(element, elementName);
                if (CollectionUtils.isNotEmpty(tmpElementList)) {
                    returnList.addAll(tmpElementList);
                }
            }
        }
        return returnList;
    }

    public ResourceEntityViewBuilder(ResourceEntityVo resourceEntity) {
        try {
            String name = resourceEntity.getName();
            String xml = "";
            String description = resourceEntity.getDescription();
            this.type = "";
            Document document = DocumentHelper.parseText(xml);
            Element root = document.getRootElement();
            if (Objects.equals(type, ViewType.RESOURCE.getValue())) {
                List<ResourceEntityVo> resourceEntityVoList = ResourceEntityFactory.getResourceEntityList();
                for (ResourceEntityVo entity : resourceEntityVoList) {
                    if (Objects.equals(entity.getName(), name)) {
                        resourceEntityVo = createResourceEntityVo(entity);
//                        resourceEntityVo.setXml(xml);
                    }
                }
                if (resourceEntityVo == null) {
                    throw new ResourceCenterResourceFoundException(name);
                }
                convertToResourceEntityVo(root);
            } else if (Objects.equals(type, ViewType.SCENE.getValue())) {
                List<SceneEntityVo> sceneEntityList = ResourceEntityFactory.getSceneEntityList();
                for (SceneEntityVo sceneEntity : sceneEntityList) {
                    if (Objects.equals(sceneEntity.getName(), name)) {
                        sceneEntityVo = new SceneEntityVo();
                        sceneEntityVo.setName(name);
                        sceneEntityVo.setLabel(sceneEntity.getLabel());
                        resourceEntityVo = new ResourceEntityVo();
                        resourceEntityVo.setName(name);
                        resourceEntityVo.setDescription(description);
//                        resourceEntityVo.setXml(xml);
                    }
                }
                if (sceneEntityVo == null) {
                    throw new ResourceCenterResourceFoundException(name);
                }
                convertToSceneEntityVo(root);
            }
        } catch (DocumentException e) {
            throw new ResourceCenterConfigIrregularException(e);
        }
    }

    /**
     * 将<scene></scene>标签内容转换成ResourceEntityVo
     * @param resourceElement
     * @return
     */
    private void convertToResourceEntityVo(Element resourceElement) {
        Map<String, List<Element>> elementMap = new HashMap<>();
        try {
            String viewName = resourceEntityVo.getName();
            String ciName = resourceElement.attributeValue("ci");
            if (StringUtils.isBlank(ciName)) {
                throw new ResourceCenterConfigIrregularException(viewName, "ci");
            }
            CiVo ciVo = getCiByName(ciName);
            if (ciVo == null) {
                throw new CiNotFoundException(ciName);
            }
            resourceEntityVo.setCi(ciVo);
            resourceEntityVo.setJoinList(null);
            if (CollectionUtils.isNotEmpty(resourceEntityVo.getAttrList())) {
                //分析属性
                for (ResourceEntityAttrVo attr : resourceEntityVo.getAttrList()) {
                    if (!elementMap.containsKey(viewName + "_attr")) {
                        elementMap.put(viewName + "_attr", getAllChildElement(resourceElement, "attr"));
                    }
                    List<Element> attrElementList = elementMap.get(viewName + "_attr");
                    Optional<Element> attrOp = attrElementList.stream().filter(e -> e.attributeValue("field").equalsIgnoreCase(attr.getField())).findFirst();
                    if (attrOp.isPresent()) {
                        Element attrElement = attrOp.get();
                        String attrName = attrElement.attributeValue("attr");
                        String attrCiName = attrElement.attributeValue("ci");
                        CiVo attrCiVo = null;
                        if (StringUtils.isNotBlank(attrCiName)) {
                            attrCiVo = getCiByName(attrCiName);
                            if (attrCiVo == null) {
                                throw new CiNotFoundException(attrCiName);
                            }
                        }
                        if (StringUtils.isNotBlank(attrName)) {
                            attr.setAttr(attrName);
                            if (attrCiVo == null) {
                                checkAttrIsExists(ciVo, attrName);
                                attr.setCiId(ciVo.getId());
                                attr.setCiName(ciName);
                                attr.setTableAlias(viewName);
                            } else {
                                checkAttrIsExists(attrCiVo, attrName);
                                attr.setCiName(attrCiName);
                                attr.setCiId(attrCiVo.getId());
                                attr.setCi(attrCiVo);
                                attr.setTableAlias("target_cientity_" + attrCiName.toLowerCase(Locale.ROOT));
                            }

                            if (!attrName.startsWith("_")) {
                                AttrVo attrVo = getCiByName(attr.getCiName()).getAttrByName(attrName);
                                if (attrVo == null) {
                                    throw new AttrNotFoundException(attr.getCiName(), attrName);
                                }
                                attr.setAttrId(attrVo.getId());
                            }
                        } else if (attrElement.getParent().getName().equalsIgnoreCase("join")) {
                            if (attrCiVo != null) {
                                attr.setAttr("_id");
                                attr.setCiId(attrCiVo.getId());
                                attr.setCi(attrCiVo);
                                attr.setCiName(attrCiName);
                                attr.setTableAlias("target_cientity_" + attrCiName.toLowerCase(Locale.ROOT));
                            } else {
                                throw new ResourceCenterConfigIrregularException(viewName, "attr", attr.getField(), "ci");
                            }
                        } else {
                            throw new ResourceCenterConfigIrregularException(viewName, "attr", attr.getField(), "attr");
                        }
                    } else {
                        if (!elementMap.containsKey(viewName + "_rel")) {
                            elementMap.put(viewName + "_rel", getAllChildElement(resourceElement, "rel"));
                        }
                        List<Element> relElementList = elementMap.get(viewName + "_rel");
                        Optional<Element> relOp = relElementList.stream().filter(e -> e.attributeValue("field").equalsIgnoreCase(attr.getField())).findFirst();
                        if (relOp.isPresent()) {
                            Element attrElement = relOp.get();
                            String attrCiName = attrElement.attributeValue("ci");
                            CiVo attrCiVo = null;
                            if (StringUtils.isNotBlank(attrCiName)) {
                                attrCiVo = getCiByName(attrCiName);
                                if (attrCiVo == null) {
                                    throw new CiNotFoundException(attrCiName);
                                }
                            }
                            if (attrCiVo != null) {
                                attr.setAttr("_id");
                                attr.setCiId(attrCiVo.getId());
                                attr.setCi(attrCiVo);
                                attr.setCiName(attrCiName);
                                attr.setTableAlias("target_cientity_" + attrCiName.toLowerCase(Locale.ROOT));
                            } else {
                                throw new ResourceCenterConfigIrregularException(viewName, "rel", attr.getField(), "ci");
                            }
                        } else {
                            throw new ResourceCenterConfigIrregularException(viewName, "attr或rel", attr.getField());
                        }
                    }
                }
                //分析连接查询
                Element joinElement = resourceElement.element("join");
                if (joinElement != null) {
                    List<Element> attrElementList = joinElement.elements("attr");
                    if (CollectionUtils.isNotEmpty(attrElementList)) {
                        for (Element attrElement : attrElementList) {
                            String attrCiName = attrElement.attributeValue("ci");
                            String attrFieldName = attrElement.attributeValue("field");
                            String joinAttrName = attrElement.attributeValue("joinAttrName");
                            if (StringUtils.isBlank(joinAttrName)) {
                                throw new ResourceCenterConfigIrregularException(viewName, "attr", attrFieldName, "joinAttrName");
                            }
                            checkAttrIsExists(ciVo, joinAttrName);
                            if (StringUtils.isNotBlank(attrCiName)) {
                                CiVo joinCiVo = getCiByName(attrCiName);
                                if (joinCiVo == null) {
                                    throw new CiNotFoundException(attrCiName);
                                }
                                ResourceEntityJoinVo joinVo = new ResourceEntityJoinVo(JoinType.ATTR);
                                joinVo.setCi(joinCiVo);
                                joinVo.setCiName(joinCiVo.getName());
                                joinVo.setField(attrFieldName);
                                joinVo.setJoinAttrName(joinAttrName);
                                resourceEntityVo.addJoin(joinVo);
                            }
                        }
                    }

                    List<Element> relElementList = joinElement.elements("rel");
                    if (CollectionUtils.isNotEmpty(relElementList)) {
                        for (Element relElement : relElementList) {
                            String relCiName = relElement.attributeValue("ci");
                            String relFieldName = relElement.attributeValue("field");
                            String relDirection = relElement.attributeValue("direction");
                            if (StringUtils.isNotBlank(relCiName)) {
                                CiVo joinCiVo = getCiByName(relCiName);
                                if (joinCiVo == null) {
                                    throw new CiNotFoundException(relCiName);
                                }
                                ResourceEntityJoinVo joinVo = new ResourceEntityJoinVo(JoinType.REL);
                                joinVo.setCi(joinCiVo);
                                joinVo.setCiName(joinCiVo.getName());
                                joinVo.setField(relFieldName);
                                if (StringUtils.isNotBlank(relDirection)) {
                                    joinVo.setDirection(relDirection);
                                }
                                resourceEntityVo.addJoin(joinVo);
                            }
                        }
                    }
                }
            }
            resourceEntityVo.setStatus(Status.PENDING.getValue());
        } catch (Exception ex) {
            resourceEntityVo.setStatus(Status.ERROR.getValue());
            resourceEntityVo.setError(ex.getMessage());
        }
    }
//    /**
//     * 将<scene></scene>标签内容转换成SceneEntityVo
//     * @param resourceElement
//     * @return
//     */
//    private void convertToSceneEntityVo(Element resourceElement) {
//        Map<String, List<Element>> elementMap = new HashMap<>();
//        try {
//            String viewName = sceneEntityVo.getName();
//            String ciName = resourceElement.attributeValue("ci");
//            if (StringUtils.isBlank(ciName)) {
//                throw new ResourceCenterConfigIrregularException(viewName, "ci");
//            }
//            CiVo ciVo = getCiByName(ciName);
//            if (ciVo == null) {
//                throw new CiNotFoundException(ciName);
//            }
//            sceneEntityVo.setCi(ciVo);
//            resourceEntityVo.setCiId(ciVo.getId());
//            Map<String, SceneEntityJoinVo> attToCiJoinMap = new HashMap<>();
//            Map<String, SceneEntityJoinVo> relFromCiJoinMap = new HashMap<>();
//            Map<String, SceneEntityJoinVo> relToCiJoinMap = new HashMap<>();
//            //分析连接查询
//            List<SceneEntityJoinVo> sceneEntityJoinList = new ArrayList<>();
//            sceneEntityVo.setJoinList(sceneEntityJoinList);
//            //先分析<join>标签内容，因为后面分析<attr>和<rel>标签时需要从<join>标签中补充默认信息
//            List<Element> joinElementList = resourceElement.elements("join");
//            for (Element joinElement : joinElementList) {
//                List<Element> attrElementList = joinElement.elements("attr");
//                if (CollectionUtils.isNotEmpty(attrElementList)) {
//                    for (Element attrElement : attrElementList) {
//                        SceneEntityJoinVo joinVo = convertToSceneEntityJoinVo(viewName, attrElement, JoinType.ATTR);
//                        String toCi = joinVo.getToCi();
//                        if (StringUtils.isBlank(toCi)) {
//                            throw new ResourceCenterConfigIrregularException(viewName, "join->attr", joinVo.getField(), "toCi");
//                        }
//                        CiVo toCiVo = getCiByName(toCi);
//                        if (toCiVo == null) {
//                            throw new CiNotFoundException(toCi);
//                        }
//                        joinVo.setToCiVo(toCiVo);
//                        String fromCi = joinVo.getFromCi();
//                        if (StringUtils.isBlank(fromCi)) {
//                            fromCi = ciName;
//                        }
//                        CiVo fromCiVo = getCiByName(fromCi);
//                        if (fromCiVo == null) {
//                            throw new CiNotFoundException(ciName);
//                        }
//                        joinVo.setFromCiVo(fromCiVo);
//                        String fromAttr = joinVo.getFromAttr();
//                        if (StringUtils.isNotBlank(fromAttr)) {
//                            if (!fromAttr.startsWith("_")) {
//                                AttrVo attrVo = fromCiVo.getAttrByName(fromAttr);
//                                if (attrVo == null) {
//                                    throw new AttrNotFoundException(fromCi, fromAttr);
//                                }
//                                joinVo.setFromAttrVo(attrVo);
//                            }
//                        } else if (attrElement.getParent().getName().equalsIgnoreCase("join")) {
//                            joinVo.setFromAttr("_id");
//                        } else {
//                            throw new ResourceCenterConfigIrregularException(viewName, "attr", joinVo.getField(), "fromAttr");
//                        }
//                        if (!sceneEntityJoinList.contains(joinVo)) {
//                            sceneEntityJoinList.add(joinVo);
//                        }
//                        attToCiJoinMap.put(toCi, joinVo);
//                    }
//                }
//
//                List<Element> relElementList = joinElement.elements("rel");
//                if (CollectionUtils.isNotEmpty(relElementList)) {
//                    for (Element relElement : relElementList) {
//                        SceneEntityJoinVo joinVo = convertToSceneEntityJoinVo(viewName, relElement, JoinType.REL);
//                        if (Objects.equals(joinVo.getDirection(), RelDirectionType.FROM.getValue())) {
//                            String toCi = joinVo.getToCi();
//                            if (StringUtils.isBlank(toCi)) {
//                                throw new ResourceCenterConfigIrregularException(viewName, "join->rel", joinVo.getField(), "toCi");
//                            }
//                            CiVo toCiVo = getCiByName(toCi);
//                            if (toCiVo == null) {
//                                throw new CiNotFoundException(toCi);
//                            }
//                            joinVo.setToCiVo(toCiVo);
//                            String fromCi = joinVo.getFromCi();
//                            if (StringUtils.isBlank(fromCi)) {
//                                fromCi = ciName;
//                            }
//                            CiVo fromCiVo = getCiByName(fromCi);
//                            if (fromCiVo == null) {
//                                throw new CiNotFoundException(fromCi);
//                            }
//                            joinVo.setFromCiVo(fromCiVo);
//                            relToCiJoinMap.put(toCi, joinVo);
//                        } else {
//                            String fromCi = joinVo.getFromCi();
//                            if (StringUtils.isBlank(fromCi)) {
//                                throw new ResourceCenterConfigIrregularException(viewName, "join->rel", joinVo.getField(), "fromCi");
//                            }
//                            CiVo fromCiVo = getCiByName(fromCi);
//                            if (fromCiVo == null) {
//                                throw new CiNotFoundException(fromCi);
//                            }
//                            joinVo.setFromCiVo(fromCiVo);
//                            String toCi = joinVo.getToCi();
//                            if (StringUtils.isBlank(toCi)) {
//                                toCi = ciName;
//                            }
//                            CiVo toCiVo = getCiByName(toCi);
//                            if (toCiVo == null) {
//                                throw new CiNotFoundException(toCi);
//                            }
//                            joinVo.setToCiVo(toCiVo);
//                            relFromCiJoinMap.put(fromCi, joinVo);
//                        }
//                        if (!sceneEntityJoinList.contains(joinVo)) {
//                            sceneEntityJoinList.add(joinVo);
//                        }
//                    }
//                }
//            }
//            //分析属性
//            {
//                List<Element> attrElementList = elementMap.get(viewName + "_attr");
//                if (attrElementList == null) {
//                    attrElementList = getAllChildElement(resourceElement, "attr");
//                    elementMap.put(viewName + "_attr", attrElementList);
//                }
//                List<SceneEntityAttrVo> sceneEntityAttrList = new ArrayList<>();
//                sceneEntityVo.setAttrList(sceneEntityAttrList);
//                for (Element attrElement : attrElementList) {
//                    SceneEntityAttrVo entityAttrVo = convertToSceneEntityAttrVo(viewName, attrElement, JoinType.ATTR);
//                    String fromCi = entityAttrVo.getFromCi();
//                    if (StringUtils.isBlank(fromCi)) {
//                        fromCi = ciName;
//                    }
//                    CiVo fromCiVo = getCiByName(fromCi);
//                    if (fromCiVo == null) {
//                        throw new CiNotFoundException(fromCi);
//                    }
//                    entityAttrVo.setFromCiVo(fromCiVo);
//                    String fromAttr = entityAttrVo.getFromAttr();
//                    if (StringUtils.isNotBlank(fromAttr)) {
//                        if (!fromAttr.startsWith("_")) {
//                            AttrVo attrVo = fromCiVo.getAttrByName(fromAttr);
//                            if (attrVo == null) {
//                                throw new AttrNotFoundException(fromCi, fromAttr);
//                            }
//                            entityAttrVo.setFromAttrVo(attrVo);
//                        }
//                    } else if (attrElement.getParent().getName().equalsIgnoreCase("join")) {
//                        entityAttrVo.setFromAttr("_id");
//                    }
//                    String toCi = entityAttrVo.getToCi();
//                    if (StringUtils.isNotBlank(toCi)) {
//                        entityAttrVo.setJoinType(JoinType.ATTR);
//                        CiVo toCiVo = getCiByName(toCi);
//                        if (toCiVo == null) {
//                            throw new CiNotFoundException(toCi);
//                        }
//                        entityAttrVo.setToCiVo(toCiVo);
//                        String toAttr = entityAttrVo.getToAttr();
//                        if (StringUtils.isNotBlank(toAttr)) {
//                            if (!toAttr.startsWith("_")) {
//                                AttrVo attrVo = toCiVo.getAttrByName(toAttr);
//                                if (attrVo == null) {
//                                    throw new AttrNotFoundException(toCi, toAttr);
//                                }
//                                entityAttrVo.setToAttrVo(attrVo);
//                            }
//                        } else if (attrElement.getParent().getName().equalsIgnoreCase("join")) {
//                            entityAttrVo.setToAttr("_id");
//                        } else {
//                            throw new ResourceCenterConfigIrregularException(viewName, "attr", entityAttrVo.getField(), "toAttr");
//                        }
//                        Integer toCiIsVirtual = toCiVo.getIsVirtual();
//                        if (Objects.equals(toCiIsVirtual, 1)) {
//                            entityAttrVo.setToAttrCiId(toCiVo.getId());
//                            entityAttrVo.setToAttrCiName(toCiVo.getName());
//                        }
//                        if (StringUtils.isBlank(fromAttr)) {
//                            SceneEntityJoinVo joinVo = attToCiJoinMap.get(toCi);
//                            if (joinVo != null) {
//                                entityAttrVo.setFromCiVo(joinVo.getFromCiVo());
//                                entityAttrVo.setFromAttrVo(joinVo.getFromAttrVo());
//                            }
//                        }
//                    }
//                    if (!sceneEntityAttrList.contains(entityAttrVo)) {
//                        sceneEntityAttrList.add(entityAttrVo);
//                    }
//                }
//                List<Element> relElementList = elementMap.get(viewName + "_rel");
//                if (relElementList == null) {
//                    relElementList = getAllChildElement(resourceElement, "rel");
//                    elementMap.put(viewName + "_rel", relElementList);
//                }
//                for (Element relElement : relElementList) {
//                    SceneEntityAttrVo entityAttrVo = convertToSceneEntityAttrVo(viewName, relElement, JoinType.REL);
//                    entityAttrVo.setJoinType(JoinType.REL);
//                    String fromCi = entityAttrVo.getFromCi();
//                    String toCi = entityAttrVo.getToCi();
//                    if (StringUtils.isBlank(fromCi) && StringUtils.isBlank(toCi)) {
//                        throw new ResourceCenterConfigIrregularException(viewName, "rel", entityAttrVo.getField(), "fromCi或toCi");
//                    }
//                    String direction = entityAttrVo.getDirection();
//                    if (StringUtils.isBlank(direction)) {
//                        if (StringUtils.isNotBlank(fromCi) && StringUtils.isNotBlank(toCi)) {
//                            direction = RelDirectionType.FROM.getValue();
//                        } else if (StringUtils.isNotBlank(fromCi)) {
//                            direction = RelDirectionType.TO.getValue();
//                        } else if (StringUtils.isNotBlank(toCi)) {
//                            direction = RelDirectionType.FROM.getValue();
//                        }
//                        entityAttrVo.setDirection(direction);
//                    }
//                    if (Objects.equals(RelDirectionType.FROM.getValue(), direction)) {
//                        if (StringUtils.isBlank(toCi)) {
//                            throw new ResourceCenterConfigIrregularException(viewName, "rel", entityAttrVo.getField(), "toCi");
//                        }
//                        CiVo toCiVo = getCiByName(toCi);
//                        if (toCiVo == null) {
//                            throw new CiNotFoundException(toCi);
//                        }
//                        entityAttrVo.setToCiVo(toCiVo);
//                        if (StringUtils.isBlank(fromCi)) {
//                            SceneEntityJoinVo joinVo = relToCiJoinMap.get(toCi);
//                            entityAttrVo.setFromCiVo(joinVo.getFromCiVo());
//                        } else {
//                            CiVo fromCiVo = getCiByName(fromCi);
//                            if (fromCiVo == null) {
//                                throw new CiNotFoundException(fromCi);
//                            }
//                            entityAttrVo.setFromCiVo(fromCiVo);
//                        }
//                    } else if (Objects.equals(RelDirectionType.TO.getValue(), direction)) {
//                        if (StringUtils.isBlank(fromCi)) {
//                            throw new ResourceCenterConfigIrregularException(viewName, "rel", entityAttrVo.getField(), "fromCi");
//                        }
//                        CiVo fromCiVo = getCiByName(fromCi);
//                        if (fromCiVo == null) {
//                            throw new CiNotFoundException(fromCi);
//                        }
//                        entityAttrVo.setFromCiVo(fromCiVo);
//                        if (StringUtils.isBlank(toCi)) {
//                            SceneEntityJoinVo joinVo = relFromCiJoinMap.get(fromCi);
//                            entityAttrVo.setToCiVo(joinVo.getToCiVo());
//                        } else {
//                            CiVo toCiVo = getCiByName(toCi);
//                            if (toCiVo == null) {
//                                throw new CiNotFoundException(toCi);
//                            }
//                            entityAttrVo.setToCiVo(toCiVo);
//                        }
//                    }
//
//                    if (Objects.equals(RelDirectionType.TO.getValue(), direction)) {
//                        String fromAttr = entityAttrVo.getFromAttr();
//                        if (StringUtils.isNotBlank(fromAttr)) {
//                            if (!fromAttr.startsWith("_")) {
//                                CiVo fromCiVo = entityAttrVo.getFromCiVo();
//                                AttrVo attrVo = fromCiVo.getAttrByName(fromAttr);
//                                if (attrVo == null) {
//                                    throw new AttrNotFoundException(fromCi, fromAttr);
//                                }
//                                entityAttrVo.setFromAttrVo(attrVo);
//                            }
//                        } else if (relElement.getParent().getName().equalsIgnoreCase("join")) {
//                            entityAttrVo.setFromAttr("_id");
//                        } else {
//                            throw new ResourceCenterConfigIrregularException(viewName, "rel", entityAttrVo.getField(), "fromAttr");
//                        }
//                    } else {
//                        String toAttr = entityAttrVo.getToAttr();
//                        if (StringUtils.isNotBlank(toAttr)) {
//                            if (!toAttr.startsWith("_")) {
//                                CiVo toCiVo = entityAttrVo.getToCiVo();
//                                AttrVo attrVo = toCiVo.getAttrByName(toAttr);
//                                if (attrVo == null) {
//                                    throw new AttrNotFoundException(toCi, toAttr);
//                                }
//                                entityAttrVo.setToAttrVo(attrVo);
//                            }
//                        } else if (relElement.getParent().getName().equalsIgnoreCase("join")) {
//                            entityAttrVo.setToAttr("_id");
//                        } else {
//                            throw new ResourceCenterConfigIrregularException(viewName, "rel", entityAttrVo.getField(), "toAttr");
//                        }
//                    }
//                    if (!sceneEntityAttrList.contains(entityAttrVo)) {
//                        sceneEntityAttrList.add(entityAttrVo);
//                    }
//                }
//            }
//            sceneEntityVo.setStatus(Status.PENDING.getValue());
//        } catch (Exception ex) {
//            sceneEntityVo.setStatus(Status.ERROR.getValue());
//            sceneEntityVo.setError(ex.getMessage());
//        }
//    }

    /**
     * 解析join->attr元素
     * @param attrElement 元素信息
     * @param viewName 视图名称
     * @param ciName ci模型名称
     * @return 返回SceneEntityJoinVo对象
     */
    private SceneEntityJoinVo parseJoinAttrElement(Element attrElement, String viewName, String ciName) {
        SceneEntityJoinVo joinVo = convertToSceneEntityJoinVo(viewName, attrElement, JoinType.ATTR);
        String toCi = joinVo.getToCi();
        if (StringUtils.isBlank(toCi)) {
            throw new ResourceCenterConfigIrregularException(viewName, "join->attr", joinVo.getField(), "toCi");
        }
        CiVo toCiVo = getCiByName(toCi);
        if (toCiVo == null) {
            throw new CiNotFoundException(toCi);
        }
        joinVo.setToCiVo(toCiVo);
        String fromCi = joinVo.getFromCi();
        if (StringUtils.isBlank(fromCi)) {
            fromCi = ciName;
        }
        CiVo fromCiVo = getCiByName(fromCi);
        if (fromCiVo == null) {
            throw new CiNotFoundException(ciName);
        }
        joinVo.setFromCiVo(fromCiVo);
        String fromAttr = joinVo.getFromAttr();
        if (StringUtils.isNotBlank(fromAttr)) {
            if (!fromAttr.startsWith("_")) {
                AttrVo attrVo = fromCiVo.getAttrByName(fromAttr);
                if (attrVo == null) {
                    throw new AttrNotFoundException(fromCi, fromAttr);
                }
                joinVo.setFromAttrVo(attrVo);
            }
        } else if (attrElement.getParent().getName().equalsIgnoreCase("join")) {
            joinVo.setFromAttr("_id");
        } else {
            throw new ResourceCenterConfigIrregularException(viewName, "attr", joinVo.getField(), "fromAttr");
        }
        return joinVo;
    }

    /**
     * 解析join->rel元素
     * @param relElement 元素信息
     * @param viewName 视图名称
     * @param ciName ci模型名称
     * @return 返回SceneEntityJoinVo对象
     */
    private SceneEntityJoinVo parseJoinRelElement(Element relElement, String viewName, String ciName) {
        SceneEntityJoinVo joinVo = convertToSceneEntityJoinVo(viewName, relElement, JoinType.REL);
        if (Objects.equals(joinVo.getDirection(), RelDirectionType.FROM.getValue())) {
            String toCi = joinVo.getToCi();
            if (StringUtils.isBlank(toCi)) {
                throw new ResourceCenterConfigIrregularException(viewName, "join->rel", joinVo.getField(), "toCi");
            }
            CiVo toCiVo = getCiByName(toCi);
            if (toCiVo == null) {
                throw new CiNotFoundException(toCi);
            }
            joinVo.setToCiVo(toCiVo);
            String fromCi = joinVo.getFromCi();
            if (StringUtils.isBlank(fromCi)) {
                fromCi = ciName;
            }
            CiVo fromCiVo = getCiByName(fromCi);
            if (fromCiVo == null) {
                throw new CiNotFoundException(fromCi);
            }
            joinVo.setFromCiVo(fromCiVo);
        } else {
            String fromCi = joinVo.getFromCi();
            if (StringUtils.isBlank(fromCi)) {
                throw new ResourceCenterConfigIrregularException(viewName, "join->rel", joinVo.getField(), "fromCi");
            }
            CiVo fromCiVo = getCiByName(fromCi);
            if (fromCiVo == null) {
                throw new CiNotFoundException(fromCi);
            }
            joinVo.setFromCiVo(fromCiVo);
            String toCi = joinVo.getToCi();
            if (StringUtils.isBlank(toCi)) {
                toCi = ciName;
            }
            CiVo toCiVo = getCiByName(toCi);
            if (toCiVo == null) {
                throw new CiNotFoundException(toCi);
            }
            joinVo.setToCiVo(toCiVo);
        }
        return joinVo;
    }

    /**
     * 解析attr元素
     * @param attrElement 元素信息
     * @param viewName 视图名称
     * @param ciName ci模型名称
     * @param attToCiJoinMap 属性join信息映射
     * @return 返回SceneEntityAttrVo对象
     */
    private SceneEntityAttrVo parseAttrElement(Element attrElement, String viewName, String ciName, Map<String, SceneEntityJoinVo> attToCiJoinMap) {
        SceneEntityAttrVo entityAttrVo = convertToSceneEntityAttrVo(viewName, attrElement, JoinType.ATTR);
        String fromCi = entityAttrVo.getFromCi();
        if (StringUtils.isBlank(fromCi)) {
            fromCi = ciName;
        }
        CiVo fromCiVo = getCiByName(fromCi);
        if (fromCiVo == null) {
            throw new CiNotFoundException(fromCi);
        }
        entityAttrVo.setFromCiVo(fromCiVo);
        String fromAttr = entityAttrVo.getFromAttr();
        if (StringUtils.isNotBlank(fromAttr)) {
            if (!fromAttr.startsWith("_")) {
                AttrVo attrVo = fromCiVo.getAttrByName(fromAttr);
                if (attrVo == null) {
                    throw new AttrNotFoundException(fromCi, fromAttr);
                }
                entityAttrVo.setFromAttrVo(attrVo);
            }
        } else if (attrElement.getParent().getName().equalsIgnoreCase("join")) {
            entityAttrVo.setFromAttr("_id");
        }
        String toCi = entityAttrVo.getToCi();
        if (StringUtils.isNotBlank(toCi)) {
            entityAttrVo.setJoinType(JoinType.ATTR);
            CiVo toCiVo = getCiByName(toCi);
            if (toCiVo == null) {
                throw new CiNotFoundException(toCi);
            }
            entityAttrVo.setToCiVo(toCiVo);
            String toAttr = entityAttrVo.getToAttr();
            if (StringUtils.isNotBlank(toAttr)) {
                if (!toAttr.startsWith("_")) {
                    AttrVo attrVo = toCiVo.getAttrByName(toAttr);
                    if (attrVo == null) {
                        throw new AttrNotFoundException(toCi, toAttr);
                    }
                    entityAttrVo.setToAttrVo(attrVo);
                }
            } else if (attrElement.getParent().getName().equalsIgnoreCase("join")) {
                entityAttrVo.setToAttr("_id");
            } else {
                throw new ResourceCenterConfigIrregularException(viewName, "attr", entityAttrVo.getField(), "toAttr");
            }
            Integer toCiIsVirtual = toCiVo.getIsVirtual();
            if (Objects.equals(toCiIsVirtual, 1)) {
                entityAttrVo.setToAttrCiId(toCiVo.getId());
                entityAttrVo.setToAttrCiName(toCiVo.getName());
            }
            if (StringUtils.isBlank(fromAttr)) {
                SceneEntityJoinVo joinVo = attToCiJoinMap.get(toCi);
                if (joinVo == null) {
                    throw new ResourceCenterConfigIrregularException(viewName, "attr", entityAttrVo.getField(), "fromAttr");
                }
                entityAttrVo.setFromCiVo(joinVo.getFromCiVo());
                entityAttrVo.setFromAttrVo(joinVo.getFromAttrVo());
            }
        }
        return entityAttrVo;
    }

    /**
     * 解析attr元素
     * @param relElement 元素信息
     * @param viewName 视图名称
     * @param relFromCiJoinMap 上游关系join信息映射
     * @param relToCiJoinMap 下游关系join信息映射
     * @return 返回SceneEntityAttrVo对象
     */
    private SceneEntityAttrVo parseRelElement(Element relElement, String viewName, Map<String, SceneEntityJoinVo> relFromCiJoinMap, Map<String, SceneEntityJoinVo> relToCiJoinMap) {
        SceneEntityAttrVo entityAttrVo = convertToSceneEntityAttrVo(viewName, relElement, JoinType.REL);
        entityAttrVo.setJoinType(JoinType.REL);
        String fromCi = entityAttrVo.getFromCi();
        String toCi = entityAttrVo.getToCi();
        if (StringUtils.isBlank(fromCi) && StringUtils.isBlank(toCi)) {
            throw new ResourceCenterConfigIrregularException(viewName, "rel", entityAttrVo.getField(), "fromCi或toCi");
        }
        String direction = entityAttrVo.getDirection();
        if (StringUtils.isBlank(direction)) {
            if (StringUtils.isNotBlank(fromCi) && StringUtils.isNotBlank(toCi)) {
                direction = RelDirectionType.FROM.getValue();
            } else if (StringUtils.isNotBlank(fromCi)) {
                direction = RelDirectionType.TO.getValue();
            } else if (StringUtils.isNotBlank(toCi)) {
                direction = RelDirectionType.FROM.getValue();
            }
            entityAttrVo.setDirection(direction);
        }
        if (Objects.equals(RelDirectionType.FROM.getValue(), direction)) {
            if (StringUtils.isBlank(toCi)) {
                throw new ResourceCenterConfigIrregularException(viewName, "rel", entityAttrVo.getField(), "toCi");
            }
            CiVo toCiVo = getCiByName(toCi);
            if (toCiVo == null) {
                throw new CiNotFoundException(toCi);
            }
            entityAttrVo.setToCiVo(toCiVo);
            if (StringUtils.isBlank(fromCi)) {
                SceneEntityJoinVo joinVo = relToCiJoinMap.get(toCi);
                entityAttrVo.setFromCiVo(joinVo.getFromCiVo());
            } else {
                CiVo fromCiVo = getCiByName(fromCi);
                if (fromCiVo == null) {
                    throw new CiNotFoundException(fromCi);
                }
                entityAttrVo.setFromCiVo(fromCiVo);
            }
        } else if (Objects.equals(RelDirectionType.TO.getValue(), direction)) {
            if (StringUtils.isBlank(fromCi)) {
                throw new ResourceCenterConfigIrregularException(viewName, "rel", entityAttrVo.getField(), "fromCi");
            }
            CiVo fromCiVo = getCiByName(fromCi);
            if (fromCiVo == null) {
                throw new CiNotFoundException(fromCi);
            }
            entityAttrVo.setFromCiVo(fromCiVo);
            if (StringUtils.isBlank(toCi)) {
                SceneEntityJoinVo joinVo = relFromCiJoinMap.get(fromCi);
                entityAttrVo.setToCiVo(joinVo.getToCiVo());
            } else {
                CiVo toCiVo = getCiByName(toCi);
                if (toCiVo == null) {
                    throw new CiNotFoundException(toCi);
                }
                entityAttrVo.setToCiVo(toCiVo);
            }
        }

        if (Objects.equals(RelDirectionType.TO.getValue(), direction)) {
            String fromAttr = entityAttrVo.getFromAttr();
            if (StringUtils.isNotBlank(fromAttr)) {
                if (!fromAttr.startsWith("_")) {
                    CiVo fromCiVo = entityAttrVo.getFromCiVo();
                    AttrVo attrVo = fromCiVo.getAttrByName(fromAttr);
                    if (attrVo == null) {
                        throw new AttrNotFoundException(fromCi, fromAttr);
                    }
                    entityAttrVo.setFromAttrVo(attrVo);
                }
            } else if (relElement.getParent().getName().equalsIgnoreCase("join")) {
                entityAttrVo.setFromAttr("_id");
            } else {
                throw new ResourceCenterConfigIrregularException(viewName, "rel", entityAttrVo.getField(), "fromAttr");
            }
        } else {
            String toAttr = entityAttrVo.getToAttr();
            if (StringUtils.isNotBlank(toAttr)) {
                if (!toAttr.startsWith("_")) {
                    CiVo toCiVo = entityAttrVo.getToCiVo();
                    AttrVo attrVo = toCiVo.getAttrByName(toAttr);
                    if (attrVo == null) {
                        throw new AttrNotFoundException(toCi, toAttr);
                    }
                    entityAttrVo.setToAttrVo(attrVo);
                }
            } else if (relElement.getParent().getName().equalsIgnoreCase("join")) {
                entityAttrVo.setToAttr("_id");
            } else {
                throw new ResourceCenterConfigIrregularException(viewName, "rel", entityAttrVo.getField(), "toAttr");
            }
        }
        return entityAttrVo;
    }

    /**
     * 按顺序将<scene></scene>标签内容转换成SceneEntityVo
     * @param sceneElement
     * @return
     */
    private void convertToSceneEntityVo(Element sceneElement) {
        try {
            String viewName = sceneEntityVo.getName();
            String ciName = sceneElement.attributeValue("ci");
            if (StringUtils.isBlank(ciName)) {
                throw new ResourceCenterConfigIrregularException(viewName, "ci");
            }
            CiVo ciVo = getCiByName(ciName);
            if (ciVo == null) {
                throw new CiNotFoundException(ciName);
            }
            sceneEntityVo.setCi(ciVo);
            resourceEntityVo.setCiId(ciVo.getId());
            List<SceneEntityAttrVo> sceneEntityAttrList = new ArrayList<>();
            sceneEntityVo.setAttrList(sceneEntityAttrList);
            Map<String, SceneEntityJoinVo> attToCiJoinMap = new HashMap<>();
            Map<String, SceneEntityJoinVo> relFromCiJoinMap = new HashMap<>();
            Map<String, SceneEntityJoinVo> relToCiJoinMap = new HashMap<>();
            List<Element> elementList = sceneElement.elements();
            for (Element element : elementList) {
                String name = element.getName();
                if ("join".equals(name)) {
                    List<Element> attrElementList = element.elements("attr");
                    if (CollectionUtils.isNotEmpty(attrElementList)) {
                        for (Element attrElement : attrElementList) {
                            SceneEntityJoinVo joinVo = parseJoinAttrElement(attrElement, viewName, ciName);
                            attToCiJoinMap.put(joinVo.getToCi(), joinVo);
                            SceneEntityAttrVo entityAttrVo = parseAttrElement(attrElement, viewName, ciName, attToCiJoinMap);
                            if (!sceneEntityAttrList.contains(entityAttrVo)) {
                                sceneEntityAttrList.add(entityAttrVo);
                            }
                        }
                    }
                    List<Element> relElementList = element.elements("rel");
                    if (CollectionUtils.isNotEmpty(relElementList)) {
                        for (Element relElement : relElementList) {
                            SceneEntityJoinVo joinVo = parseJoinRelElement(relElement, viewName, ciName);
                            if (Objects.equals(joinVo.getDirection(), RelDirectionType.FROM.getValue())) {
                                relToCiJoinMap.put(joinVo.getToCi(), joinVo);
                            } else {
                                relFromCiJoinMap.put(joinVo.getFromCi(), joinVo);
                            }
                            SceneEntityAttrVo entityAttrVo = parseRelElement(relElement, viewName, relFromCiJoinMap, relToCiJoinMap);
                            if (!sceneEntityAttrList.contains(entityAttrVo)) {
                                sceneEntityAttrList.add(entityAttrVo);
                            }
                        }
                    }
                } else if ("attr".equals(name)) {
                    SceneEntityAttrVo entityAttrVo = parseAttrElement(element, viewName, ciName, attToCiJoinMap);
                    if (!sceneEntityAttrList.contains(entityAttrVo)) {
                        sceneEntityAttrList.add(entityAttrVo);
                    }
                } else if ("rel".equals(name)) {
                    SceneEntityAttrVo entityAttrVo = parseRelElement(element, viewName, relFromCiJoinMap, relToCiJoinMap);
                    if (!sceneEntityAttrList.contains(entityAttrVo)) {
                        sceneEntityAttrList.add(entityAttrVo);
                    }
                }
            }
            List<String> definedFieldList = sceneEntityAttrList.stream().map(SceneEntityAttrVo::getField).collect(Collectors.toList());
            List<String> declaredFieldList = ResourceEntityFactory.getFieldNameListByViewName(viewName);
            List<String> undefinedFieldList = ListUtils.removeAll(declaredFieldList, definedFieldList);
            if (CollectionUtils.isNotEmpty(undefinedFieldList)) {
                throw new ResourceCenterViewConfigException(viewName, String.join(",", undefinedFieldList));
            }
            sceneEntityVo.setStatus(Status.PENDING.getValue());
        } catch (Exception ex) {
            sceneEntityVo.setStatus(Status.ERROR.getValue());
            sceneEntityVo.setError(ex.getMessage());
        }
    }

    /**
     * 将<attr></attr>和<rel></rel>标签内容转换成SceneEntityAttrVo
     * @param element
     * @return
     */
    private SceneEntityAttrVo convertToSceneEntityAttrVo(String viewName, Element element, JoinType joinType) {
        String field = element.attributeValue("field");
//        String resource = element.attributeValue("resource");
        if (StringUtils.isBlank(field)) {
            throw new ResourceCenterConfigIrregularException(viewName, joinType, "field");
        }
        List<String> fieldList = ResourceEntityFactory.getFieldNameListByViewName(viewName);
        if (!fieldList.contains(field)) {
            throw new ResourceCenterResourceFoundException(viewName, field);
        }
//        if (StringUtils.isBlank(resource)) {
//            throw new ResourceCenterConfigIrregularException(viewName, joinType, "resource");
//        }
//        checkResourceAndField(resource, field);
        String direction = element.attributeValue("direction");
        String fromCi = element.attributeValue("fromCi");
        String toCi = element.attributeValue("toCi");
        String fromAttr = element.attributeValue("fromAttr");
        String toAttr = element.attributeValue("toAttr");
        SceneEntityAttrVo attrVo = new SceneEntityAttrVo();
        attrVo.setField(field);
//        attrVo.setResource(resource);
        attrVo.setDirection(direction);
        attrVo.setFromCi(fromCi);
        attrVo.setToCi(toCi);
        attrVo.setFromAttr(fromAttr);
        attrVo.setToAttr(toAttr);
        return attrVo;
    }

    /**
     * 将<join></join>标签内容转换成SceneEntityJoinVo
     * @param viewName
     * @param element
     * @param joinType
     * @return
     */
    private SceneEntityJoinVo convertToSceneEntityJoinVo(String viewName, Element element, JoinType joinType) {
        String field = element.attributeValue("field");
//        String resource = element.attributeValue("resource");
        if (StringUtils.isBlank(field)) {
            throw new ResourceCenterConfigIrregularException(viewName, joinType, "field");
        }
        List<String> fieldList = ResourceEntityFactory.getFieldNameListByViewName(viewName);
        if (!fieldList.contains(field)) {
            throw new ResourceCenterResourceFoundException(viewName, field);
        }
//        if (StringUtils.isBlank(resource)) {
//            throw new ResourceCenterConfigIrregularException(viewName, joinType, "resource");
//        }
//        checkResourceAndField(resource, field);
        String direction = element.attributeValue("direction");
        String fromCi = element.attributeValue("fromCi");
        String toCi = element.attributeValue("toCi");
        String fromAttr = element.attributeValue("fromAttr");
        String toAttr = element.attributeValue("toAttr");
        SceneEntityJoinVo joinVo = new SceneEntityJoinVo(joinType);
        joinVo.setField(field);
//        joinVo.setResource(resource);
        joinVo.setDirection(direction);
        joinVo.setFromCi(fromCi);
        joinVo.setToCi(toCi);
        joinVo.setFromAttr(fromAttr);
        joinVo.setToAttr(toAttr);
        return joinVo;
    }

    /**
     * 检查resource和field是否合格
     * @param resource
     * @param field
     */
    @Deprecated
    private void checkResourceAndField(String resource, String field) {
        List<ResourceEntityVo> resourceEntityList = ResourceEntityFactory.getResourceEntityList();
        Optional<ResourceEntityVo> optionalEntity = resourceEntityList.stream().filter(e -> Objects.equals(e.getName(), resource)).findFirst();
        if (!optionalEntity.isPresent()) {
            throw new ResourceCenterResourceFoundException(resource);
        }
        ResourceEntityVo resourceEntityVo = optionalEntity.get();
        Set<ResourceEntityAttrVo> attrList = resourceEntityVo.getAttrList();
        if (CollectionUtils.isEmpty(attrList)) {
            throw new ResourceCenterResourceFoundException(resource, field);
        }
        Optional<ResourceEntityAttrVo> optionalAttr = attrList.stream().filter(e -> Objects.equals(e.getField(), field)).findFirst();
        if (!optionalAttr.isPresent()) {
            throw new ResourceCenterResourceFoundException(resource, field);
        }
    }
    private ResourceEntityVo createResourceEntityVo(ResourceEntityVo resourceEntityVo) {
        ResourceEntityVo resourceEntity = new ResourceEntityVo();
        resourceEntity.setName(resourceEntityVo.getName());
        resourceEntity.setLabel(resourceEntityVo.getLabel());
        Set<ResourceEntityAttrVo> attrList = resourceEntityVo.getAttrList();
        if (CollectionUtils.isNotEmpty(attrList)) {
            for (ResourceEntityAttrVo resourceEntityAttrVo : attrList) {
                ResourceEntityAttrVo attrVo = new ResourceEntityAttrVo();
                attrVo.setField(resourceEntityAttrVo.getField());
                resourceEntity.addAttr(attrVo);
            }
        }
        return resourceEntity;
    }
    /**
     * 检查模型中是否存在对应属性
     * @param ciVo
     * @param attrName
     */
    private boolean checkAttrIsExists(CiVo ciVo, String attrName) {
        if (defaultAttrList.contains(attrName)) {
            return true;
        }
        List<AttrVo> attrList = ciVo.getAttrList();
        if (CollectionUtils.isNotEmpty(attrList)) {
            for (AttrVo attrVo : attrList) {
                if (Objects.equals(attrVo.getName(), attrName)) {
                    return true;
                }
            }
        }
        throw new AttrNotFoundException(ciVo.getName(), attrName);
    }


    public void buildView() {
        if (Objects.equals(type, ViewType.RESOURCE.getValue())) {
            buildResource();
        } else if (Objects.equals(type, ViewType.SCENE.getValue())) {
            buildScene();
        }
    }

    private void buildResource() {
        if (StringUtils.isNotBlank(resourceEntityVo.getError())) {
            resourceEntityVo.setStatus(Status.ERROR.getValue());
            resourceEntityMapper.updateResourceEntityStatusAndError(resourceEntityVo);
            return;
        }
        Table mainTable = new Table();
        mainTable.setSchemaName(TenantContext.get().getDbName());
        mainTable.setName("cmdb_cientity");
        mainTable.setAlias(new Alias("ci_base"));
        Select select = SelectUtils.buildSelectFromTableAndSelectItems(mainTable);
        SelectBody selectBody = select.getSelectBody();
        PlainSelect plainSelect = (PlainSelect) selectBody;


        plainSelect.addJoins(new Join()
                .withRightItem(new SubSelect()
                        .withSelectBody(buildSubSelectForCi(resourceEntityVo.getCi()).getSelectBody())
                        .withAlias(new Alias(resourceEntityVo.getName().toLowerCase(Locale.ROOT))))
                .addOnExpression(new EqualsTo().withLeftExpression(new Column()
                                .withTable(new Table("ci_base"))
                                .withColumnName("id"))
                        .withRightExpression(new Column()
                                .withTable(new Table(resourceEntityVo.getName()))
                                .withColumnName("id"))));

        if (CollectionUtils.isNotEmpty(resourceEntityVo.getAttrList())) {
            for (ResourceEntityAttrVo entityAttr : resourceEntityVo.getAttrList()) {
                SelectExpressionItem selectItem = new SelectExpressionItem();
                if (entityAttr.getAttr().startsWith("_")) {
                    selectItem.setExpression(new Column(entityAttr.getTableAlias() + "." + entityAttr.getAttr().substring(1)));
                } else if (entityAttr.getAttrId() != null) {
                    selectItem.setExpression(new Column(entityAttr.getTableAlias() + ".`" + entityAttr.getAttrId() + "`"));
                }
                selectItem.setAlias(new Alias(entityAttr.getField().toLowerCase(Locale.ROOT)));
                plainSelect.addSelectItems(selectItem);
            }
        }

        if (CollectionUtils.isNotEmpty(resourceEntityVo.getJoinList())) {
            List<Join> joinList = new ArrayList<>();
            for (ResourceEntityJoinVo entityJoin : resourceEntityVo.getJoinList()) {
                if (entityJoin.getJoinType() == JoinType.ATTR) {
                    plainSelect.addJoins(new Join()
                            //.withLeft(true)
                            .withRightItem(new Table()
                                    .withName("cmdb_attrentity")
                                    .withSchemaName(TenantContext.get().getDbName())
                                    .withAlias(new Alias("cmdb_attrentity_" + entityJoin.getField().toLowerCase(Locale.ROOT))))
                            .addOnExpression(new EqualsTo()
                                    .withLeftExpression(new Column()
                                            .withTable(new Table("ci_base"))
                                            .withColumnName("id"))
                                    .withRightExpression(new Column()
                                            .withTable(new Table("cmdb_attrentity_" + entityJoin.getField().toLowerCase(Locale.ROOT)))
                                            .withColumnName("from_cientity_id"))));
                    if (StringUtils.isNotBlank(entityJoin.getJoinAttrName())) {
                        plainSelect.addJoins(new Join()
                                .withRightItem(new Table()
                                        .withName("cmdb_attr")
                                        .withSchemaName(TenantContext.get().getDbName())
                                        .withAlias(new Alias("cmdb_attr_" + entityJoin.getJoinAttrName().toLowerCase(Locale.ROOT)))
                                ).addOnExpression(new AndExpression()
                                        .withLeftExpression(new EqualsTo()
                                                .withLeftExpression(new Column()
                                                        .withTable(new Table("cmdb_attr_" + entityJoin.getJoinAttrName().toLowerCase(Locale.ROOT)))
                                                        .withColumnName("id"))
                                                .withRightExpression(new Column()
                                                        .withTable(new Table("cmdb_attrentity_" + entityJoin.getField().toLowerCase(Locale.ROOT)))
                                                        .withColumnName("attr_id")))
                                        .withRightExpression(new EqualsTo()
                                                .withLeftExpression(new Column()
                                                        .withTable(new Table("cmdb_attr_" + entityJoin.getJoinAttrName().toLowerCase(Locale.ROOT)))
                                                        .withColumnName("name"))
                                                .withRightExpression(new StringValue(entityJoin.getJoinAttrName()))))
                        );
                    }
                    plainSelect.addJoins(new Join()
                            //.withLeft(true)
                            .withRightItem(new SubSelect()
                                    .withSelectBody(buildSubSelectForCi(entityJoin.getCi()).getSelectBody())
                                    .withAlias(new Alias("target_cientity_" + entityJoin.getCi().getName().toLowerCase(Locale.ROOT)))
                            ).addOnExpression(new EqualsTo()
                                    .withLeftExpression(new Column()
                                            .withTable(new Table("cmdb_attrentity_" + entityJoin.getField()))
                                            .withColumnName("to_cientity_id"))
                                    .withRightExpression(new Column()
                                            .withTable(new Table("target_cientity_" + entityJoin.getCi().getName().toLowerCase(Locale.ROOT)))
                                            .withColumnName("id"))));
                } else if (entityJoin.getJoinType() == JoinType.REL) {
                    plainSelect.addJoins(new Join()
                            .withRightItem(new Table()
                                    .withName("cmdb_relentity")
                                    .withSchemaName(TenantContext.get().getDbName())
                                    .withAlias(new Alias("cmdb_relentity_" + entityJoin.getField().toLowerCase(Locale.ROOT))))
                            .addOnExpression(new EqualsTo()
                                    .withLeftExpression(new Column()
                                            .withTable(new Table("ci_base"))
                                            .withColumnName("id"))
                                    .withRightExpression(new Column()
                                            .withTable(new Table("cmdb_relentity_" + entityJoin.getField().toLowerCase(Locale.ROOT)))
                                            .withColumnName(entityJoin.getDirection().equals(RelDirectionType.FROM.getValue()) ? "from_cientity_id" : "to_cientity_id"))));

                    plainSelect.addJoins(new Join()
                            .withRightItem(new SubSelect()
                                    .withSelectBody(buildSubSelectForCi(entityJoin.getCi()).getSelectBody())
                                    .withAlias(new Alias("target_cientity_" + entityJoin.getCi().getName().toLowerCase(Locale.ROOT)))
                            ).addOnExpression(new EqualsTo()
                                    .withLeftExpression(new Column()
                                            .withTable(new Table("cmdb_relentity_" + entityJoin.getField().toLowerCase(Locale.ROOT)))
                                            .withColumnName(entityJoin.getDirection().equals(RelDirectionType.FROM.getValue()) ? "to_cientity_id" : "from_cientity_id"))
                                    .withRightExpression(new Column()
                                            .withTable(new Table("target_cientity_" + entityJoin.getCi().getName().toLowerCase(Locale.ROOT)))
                                            .withColumnName("id"))));
                }
            }
            if (CollectionUtils.isNotEmpty(joinList)) {
                plainSelect.addJoins(joinList);
            }
        }

        String viewName = resourceEntityVo.getName();
        String selectSql = select.toString();
        String md5 = Md5Util.encryptMD5(selectSql);
        String tableType = schemaMapper.checkTableOrViewIsExists(TenantContext.get().getDataDbName(), viewName);
        if (tableType != null) {
            if (Objects.equals(tableType, "SYSTEM VIEW")) {
                return;
            } else if (Objects.equals(tableType, "VIEW")) {
                DataBaseViewInfoVo dataBaseViewInfoVo = dataBaseViewInfoMapper.getDataBaseViewInfoByViewName(viewName);
                if (dataBaseViewInfoVo != null) {
                    if (Objects.equals(md5, dataBaseViewInfoVo.getMd5())) {
                        return;
                    }
                }
            }
        }

        try {
            EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                if (Objects.equals(tableType, "BASE TABLE")) {
                    schemaMapper.deleteTable(TenantContext.get().getDataDbName() + "." + viewName);
                }
                String sql = "CREATE OR REPLACE VIEW " + TenantContext.get().getDataDbName() + "." + viewName + " AS " + selectSql;
                if (logger.isDebugEnabled()) {
                    logger.debug(sql);
                }
                schemaMapper.insertView(sql);
            }).execute();
            if (s.isSucceed()) {
                resourceEntityVo.setError("");
                resourceEntityVo.setStatus(Status.READY.getValue());
                DataBaseViewInfoVo dataBaseViewInfoVo = new DataBaseViewInfoVo();
                dataBaseViewInfoVo.setViewName(viewName);
                dataBaseViewInfoVo.setMd5(md5);
                dataBaseViewInfoVo.setLcu(UserContext.get().getUserUuid());
                dataBaseViewInfoMapper.insertDataBaseViewInfo(dataBaseViewInfoVo);
            } else {
                resourceEntityVo.setError(s.getError());
                resourceEntityVo.setStatus(Status.ERROR.getValue());
            }
        } catch (Exception ex) {
            resourceEntityVo.setError(ex.getMessage());
            resourceEntityVo.setStatus(Status.ERROR.getValue());
            Table table = new Table();
            table.setName(viewName);
            table.setSchemaName(TenantContext.get().getDataDbName());
            List<ColumnDefinition> columnDefinitions = new ArrayList<>();
            Set<ResourceEntityAttrVo> attrList = resourceEntityVo.getAttrList();
            for (ResourceEntityAttrVo attrVo : attrList) {
                ColumnDefinition columnDefinition = new ColumnDefinition();
                columnDefinition.setColumnName(attrVo.getField());
                columnDefinition.setColDataType(new ColDataType("int"));
                columnDefinitions.add(columnDefinition);
            }
            CreateTable createTable = new CreateTable();
            createTable.setTable(table);
            createTable.setColumnDefinitions(columnDefinitions);
            createTable.setIfNotExists(true);
            EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                schemaMapper.insertView(createTable.toString());
            }).execute();
        } finally {
            resourceEntityMapper.updateResourceEntityStatusAndError(resourceEntityVo);
        }
    }
    private void buildScene() {
        //创建场景视图
        ResourceEntityVo resourceEntityVo = new ResourceEntityVo();
        resourceEntityVo.setName(sceneEntityVo.getName());
        resourceEntityVo.setLabel(sceneEntityVo.getLabel());
        resourceEntityVo.setCiId(sceneEntityVo.getCiId());
        if (StringUtils.isNotBlank(sceneEntityVo.getError())) {
            resourceEntityVo.setError(sceneEntityVo.getError());
            resourceEntityVo.setStatus(Status.ERROR.getValue());
            resourceEntityMapper.updateResourceEntityStatusAndError(resourceEntityVo);
            return;
        }
        String viewName = sceneEntityVo.getName();
        SceneEntityGenerateSqlUtil sceneEntityGenerateSqlUtil = new SceneEntityGenerateSqlUtil(sceneEntityVo);
        String selectSql = sceneEntityGenerateSqlUtil.getSql();
        String md5 = Md5Util.encryptMD5(selectSql);
        String tableType = schemaMapper.checkTableOrViewIsExists(TenantContext.get().getDataDbName(), viewName);
        if (tableType != null) {
            if (Objects.equals(tableType, "SYSTEM VIEW")) {
                return;
            } else if (Objects.equals(tableType, "VIEW")) {
                DataBaseViewInfoVo dataBaseViewInfoVo = dataBaseViewInfoMapper.getDataBaseViewInfoByViewName(viewName);
                if (dataBaseViewInfoVo != null) {
                    // md5相同就不用更新视图了
                    if (Objects.equals(md5, dataBaseViewInfoVo.getMd5())) {
                        return;
                    }
                }
            }
        }
        try {

            EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                if (Objects.equals(tableType, "BASE TABLE")) {
                    schemaMapper.deleteTable(TenantContext.get().getDataDbName() + "." + viewName);
                }
                String sql = "CREATE OR REPLACE VIEW " + TenantContext.get().getDataDbName() + "." + viewName + " AS " + selectSql;
                if (logger.isDebugEnabled()) {
                    logger.debug(sql);
                }
                schemaMapper.insertView(sql);
            }).execute();
            if (s.isSucceed()) {
                resourceEntityVo.setError("");
                resourceEntityVo.setStatus(Status.READY.getValue());
                DataBaseViewInfoVo dataBaseViewInfoVo = new DataBaseViewInfoVo();
                dataBaseViewInfoVo.setViewName(viewName);
                dataBaseViewInfoVo.setMd5(md5);
                dataBaseViewInfoVo.setLcu(UserContext.get().getUserUuid());
                dataBaseViewInfoMapper.insertDataBaseViewInfo(dataBaseViewInfoVo);
            } else {
                resourceEntityVo.setError(s.getError());
                resourceEntityVo.setStatus(Status.ERROR.getValue());
            }

        } catch (Exception ex) {
            resourceEntityVo.setError(ex.getMessage());
            resourceEntityVo.setStatus(Status.ERROR.getValue());
            if (Objects.equals(tableType, "VIEW")) {
                schemaMapper.deleteView(TenantContext.get().getDataDbName() + "." + viewName);
            }
            List<String> fieldNameList = ResourceEntityFactory.getFieldNameListByViewName(viewName);
            Table table = new Table();
            table.setName(viewName);
            table.setSchemaName(TenantContext.get().getDataDbName());
            List<ColumnDefinition> columnDefinitions = new ArrayList<>();
            for (String columnName : fieldNameList) {
                ColumnDefinition columnDefinition = new ColumnDefinition();
                columnDefinition.setColumnName(columnName);
                columnDefinition.setColDataType(new ColDataType("int"));
                columnDefinitions.add(columnDefinition);
            }
            CreateTable createTable = new CreateTable();
            createTable.setTable(table);
            createTable.setColumnDefinitions(columnDefinitions);
            createTable.setIfNotExists(true);
            EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                schemaMapper.insertView(createTable.toString());
            }).execute();
        } finally {
            resourceEntityMapper.updateResourceEntityStatusAndError(resourceEntityVo);
        }
    }
    private Select buildSubSelectForCi(CiVo ciVo) {
        if (CollectionUtils.isNotEmpty(ciVo.getUpwardCiList())) {
            Table mainTable = new Table();
            mainTable.setSchemaName(TenantContext.get().getDbName());
            mainTable.setName("cmdb_cientity");
            mainTable.setAlias(new Alias("ci_base"));
            Select select = SelectUtils.buildSelectFromTableAndSelectItems(mainTable);
            SelectBody selectBody = select.getSelectBody();
            PlainSelect plainSelect = (PlainSelect) selectBody;
            //内置属性统一在这里添加
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("id").withTable(new Table("ci_base"))));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("uuid").withTable(new Table("ci_base"))));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("name").withTable(new Table("ci_base"))));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("fcu").withTable(new Table("ci_base"))));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("fcd").withTable(new Table("ci_base"))));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("lcu").withTable(new Table("ci_base"))));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("lcd").withTable(new Table("ci_base"))));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("inspect_status").withTable(new Table("ci_base"))).withAlias(new Alias("inspectStatus")));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("inspect_time").withTable(new Table("ci_base"))).withAlias(new Alias("inspectTime")));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("monitor_status").withTable(new Table("ci_base"))).withAlias(new Alias("monitorStatus")));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("monitor_time").withTable(new Table("ci_base"))).withAlias(new Alias("monitorTime")));


            plainSelect.addSelectItems(new SelectExpressionItem(new Column("id").withTable(new Table("ci_info"))).withAlias(new Alias("typeId")));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("name").withTable(new Table("ci_info"))).withAlias(new Alias("typeName")));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("label").withTable(new Table("ci_info"))).withAlias(new Alias("typeLabel")));
            for (AttrVo attrVo : ciVo.getAttrList()) {
                if (attrVo.getTargetCiId() == null) {
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("`" + attrVo.getId() + "`").withTable(new Table("cmdb_" + attrVo.getCiId()))));
                }
            }

            plainSelect.addJoins(new Join()
                    .withRightItem(new Table()
                            .withName("cmdb_ci")
                            .withAlias(new Alias("ci_info"))
                            .withSchemaName(TenantContext.get().getDbName()))
                    .withOnExpression(new EqualsTo()
                            .withLeftExpression(new Column()
                                    .withTable(new Table("ci_base"))
                                    .withColumnName("ci_id"))
                            .withRightExpression(new Column()
                                    .withTable(new Table("ci_info"))
                                    .withColumnName("id"))));

            //生成主SQL，需要join所有父模型数据表
            for (CiVo ci : ciVo.getUpwardCiList()) {
                plainSelect.addJoins(new Join()
                        .withRightItem(new Table()
                                .withName("cmdb_" + ci.getId())
                                .withSchemaName(TenantContext.get().getDataDbName())
                                .withAlias(new Alias("cmdb_" + ci.getId())))
                        .withOnExpression(new EqualsTo()
                                .withLeftExpression(new Column()
                                        .withTable(new Table("ci_base"))
                                        .withColumnName("id"))
                                .withRightExpression(new Column()
                                        .withTable(new Table("cmdb_" + ci.getId()))
                                        .withColumnName("cientity_id"))));

            }
            return select;
        } else {
            Table mainTable = new Table();
            mainTable.setSchemaName(TenantContext.get().getDataDbName());
            mainTable.setName("cmdb_" + ciVo.getId());
            return SelectUtils.buildSelectFromTable(mainTable);
        }
    }
}
