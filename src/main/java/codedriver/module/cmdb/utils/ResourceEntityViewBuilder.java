/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.utils;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.*;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.enums.resourcecenter.JoinType;
import codedriver.framework.cmdb.enums.resourcecenter.ScenceView;
import codedriver.framework.cmdb.enums.resourcecenter.Status;
import codedriver.framework.cmdb.exception.attr.AttrNotFoundException;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterConfigIrregularException;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterResourceFoundException;
import codedriver.framework.cmdb.utils.SceneEntityGenerateSqlUtil;
import codedriver.framework.dao.mapper.SchemaMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import codedriver.module.cmdb.service.ci.CiService;
import com.alibaba.fastjson.JSONObject;
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
import org.apache.commons.lang3.StringUtils;
import org.dom4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xlsx4j.sml.Col;

import java.util.*;

@Component
public class ResourceEntityViewBuilder {
    private final static Logger logger = LoggerFactory.getLogger(ResourceEntityViewBuilder.class);

    private final static List<String> defaultAttrList = Arrays.asList("_id", "_uuid", "_name", "_fcu", "_fcd", "_lcu", "_lcd", "_inspectStatus", "_inspectTime", "_monitorStatus", "_monitorTime", "_typeId", "_typeName", "_typeLabel");

    private final List<ResourceEntityVo> resourceEntityList = new ArrayList<>();

    private final List<SceneEntityVo> sceneEntityList = new ArrayList<>();

    public List<ResourceEntityVo> getResourceEntityList() {
        return resourceEntityList;
    }

    public List<SceneEntityVo> getSceneEntityList() {
        return sceneEntityList;
    }

    private final Map<String, CiVo> ciMap = new HashMap<>();
    private static SchemaMapper schemaMapper;
    private static CiService ciService;
    private static ResourceEntityMapper resourceEntityMapper;


    @Autowired
    public ResourceEntityViewBuilder(SchemaMapper _schemaMapper, CiService _ciService, ResourceEntityMapper _resourceEntityMapper) {
        schemaMapper = _schemaMapper;
        resourceEntityMapper = _resourceEntityMapper;
        ciService = _ciService;
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

    public ResourceEntityViewBuilder(String xml) {
        try {
            Map<String, List<Element>> elementMap = new HashMap<>();
            List<ResourceEntityVo> resourceEntityVoList = ResourceEntityFactory.getResourceEntityList();
            if (CollectionUtils.isNotEmpty(resourceEntityVoList)) {
                Document document = DocumentHelper.parseText(xml);
                Element root = document.getRootElement();
                for (ResourceEntityVo resourceEntity : resourceEntityVoList) {
                    ResourceEntityVo resourceEntityVo = createResourceEntityVo(resourceEntity);
                    this.resourceEntityList.add(resourceEntityVo);
                    try {
                        Optional<Element> resourceOp = root.elements("resource").stream().filter(e -> e.attributeValue("id").equalsIgnoreCase(resourceEntityVo.getName())).findFirst();
                        if (resourceOp.isPresent()) {
                            Element resourceElement = resourceOp.get();
                            String ciName = resourceElement.attributeValue("ci");
                            if (StringUtils.isNotBlank(ciName)) {
                                CiVo ciVo = getCiByName(ciName);
                                if (ciVo != null) {
                                    resourceEntityVo.setCi(ciVo);
                                    resourceEntityVo.setJoinList(null);
                                    if (CollectionUtils.isNotEmpty(resourceEntityVo.getAttrList())) {
                                        //分析属性
                                        for (ResourceEntityAttrVo attr : resourceEntityVo.getAttrList()) {
                                            if (!elementMap.containsKey(resourceEntityVo.getName() + "_attr")) {
                                                elementMap.put(resourceEntityVo.getName() + "_attr", getAllChildElement(resourceElement, "attr"));
                                            }
                                            List<Element> attrElementList = elementMap.get(resourceEntityVo.getName() + "_attr");
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
                                                        attr.setTableAlias(resourceEntityVo.getName());
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
                                                        throw new ResourceCenterConfigIrregularException(resourceEntityVo.getName(), "attr", attr.getField(), "ci");
                                                    }
                                                } else {
                                                    throw new ResourceCenterConfigIrregularException(resourceEntityVo.getName(), "attr", attr.getField(), "attr");
                                                }
                                            } else {
                                                if (!elementMap.containsKey(resourceEntityVo.getName() + "_rel")) {
                                                    elementMap.put(resourceEntityVo.getName() + "_rel", getAllChildElement(resourceElement, "rel"));
                                                }
                                                List<Element> relElementList = elementMap.get(resourceEntityVo.getName() + "_rel");
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
                                                        throw new ResourceCenterConfigIrregularException(resourceEntityVo.getName(), "rel", attr.getField(), "ci");
                                                    }
                                                } else {
                                                    throw new ResourceCenterConfigIrregularException(resourceEntityVo.getName(), "attr或rel", attr.getField());
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
                                                        throw new ResourceCenterConfigIrregularException(resourceEntityVo.getName(), "attr", attrFieldName, "joinAttrName");
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
                                } else {
                                    throw new CiNotFoundException(ciName);
                                }
                            } else {
                                throw new ResourceCenterConfigIrregularException(resourceEntityVo.getName(), "ci");
                            }
                        } else {
                            throw new ResourceCenterConfigIrregularException(resourceEntityVo.getName());
                        }
                        resourceEntityVo.setStatus(Status.PENDING.getValue());
                    } catch (Exception ex) {
                        resourceEntityVo.setStatus(Status.ERROR.getValue());
                        resourceEntityVo.setError(ex.getMessage());
                    }
                }
                convertToSceneEntityVo(root);
            }
        } catch (DocumentException e) {
            throw new ResourceCenterConfigIrregularException(e);
        }
    }

    /**
     * 将<scene></scene>标签内容转换成SceneEntityVo
     * @param root
     * @return
     */
    private void convertToSceneEntityVo(Element root) {
        Map<String, List<Element>> elementMap = new HashMap<>();
        for (ScenceView scenceView : ScenceView.values()) {
            String viewName = scenceView.getValue();
            SceneEntityVo sceneEntityVo = new SceneEntityVo();
            sceneEntityVo.setName(viewName);
            sceneEntityVo.setLabel(scenceView.getText());
            sceneEntityList.add(sceneEntityVo);
            try {
                Optional<Element> resourceOp = root.elements("scene").stream().filter(e -> e.attributeValue("id").equalsIgnoreCase(scenceView.getValue())).findFirst();
                if (!resourceOp.isPresent()) {
                    throw new ResourceCenterConfigIrregularException(scenceView.getValue());
                }
                Element resourceElement = resourceOp.get();
                String ciName = resourceElement.attributeValue("ci");
                if (StringUtils.isBlank(ciName)) {
                    throw new ResourceCenterConfigIrregularException(scenceView.getValue(), "ci");
                }
                CiVo ciVo = getCiByName(ciName);
                if (ciVo == null) {
                    throw new CiNotFoundException(ciName);
                }
                sceneEntityVo.setCi(ciVo);
                Map<String, SceneEntityJoinVo> attToCiJoinMap = new HashMap<>();
                Map<String, SceneEntityJoinVo> relFromCiJoinMap = new HashMap<>();
                Map<String, SceneEntityJoinVo> relToCiJoinMap = new HashMap<>();
                //分析连接查询
                List<SceneEntityJoinVo> sceneEntityJoinSet = new ArrayList<>();
                sceneEntityVo.setJoinList(sceneEntityJoinSet);
                //先分析<join>标签内容，因为后面分析<attr>和<rel>标签时需要从<join>标签中补充默认信息
                List<Element> joinElementList = resourceElement.elements("join");
                for (Element joinElement : joinElementList) {
                    List<Element> attrElementList = joinElement.elements("attr");
                    if (CollectionUtils.isNotEmpty(attrElementList)) {
                        for (Element attrElement : attrElementList) {
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
                                joinVo.setFromCi(fromCi);
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
//                                    joinVo.setFromAttrId(attrVo.getId());
//                                    joinVo.setFromAttrCiId(attrVo.getCiId());
//                                    joinVo.setFromAttrCiName(attrVo.getCiName());
                                }
                            } else if (attrElement.getParent().getName().equalsIgnoreCase("join")) {
                                joinVo.setFromAttr("_id");
                            } else {
                                throw new ResourceCenterConfigIrregularException(viewName, "attr", joinVo.getField(), "fromAttr");
                            }
                            sceneEntityJoinSet.add(joinVo);
                            attToCiJoinMap.put(toCi, joinVo);
                        }
                    }

                    List<Element> relElementList = joinElement.elements("rel");
                    if (CollectionUtils.isNotEmpty(relElementList)) {
                        for (Element relElement : relElementList) {
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
                                    joinVo.setFromCi(fromCi);
                                }
                                CiVo fromCiVo = getCiByName(fromCi);
                                if (fromCiVo == null) {
                                    throw new CiNotFoundException(fromCi);
                                }
                                joinVo.setFromCiVo(fromCiVo);
                                relToCiJoinMap.put(toCi, joinVo);
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
                                    joinVo.setToCi(toCi);
                                }
                                CiVo toCiVo = getCiByName(toCi);
                                if (toCiVo == null) {
                                    throw new CiNotFoundException(toCi);
                                }
                                joinVo.setToCiVo(toCiVo);
                                relFromCiJoinMap.put(fromCi, joinVo);
                            }
                            sceneEntityJoinSet.add(joinVo);
                        }
                    }
                }
                //分析属性
                {
                    List<Element> attrElementList = elementMap.get(viewName + "_attr");
                    if (attrElementList == null) {
                        attrElementList = getAllChildElement(resourceElement, "attr");
                        elementMap.put(viewName + "_attr", attrElementList);
                    }
                    List<SceneEntityAttrVo> sceneEntityAttrSet = new ArrayList<>();
                    sceneEntityVo.setAttrList(sceneEntityAttrSet);
                    for (Element attrElement : attrElementList) {
                        SceneEntityAttrVo entityAttrVo = convertToSceneEntityAttrVo(viewName, attrElement, JoinType.ATTR);
                        String fromCi = entityAttrVo.getFromCi();
                        if (StringUtils.isBlank(fromCi)) {
                            fromCi = ciName;
                            entityAttrVo.setFromCi(fromCi);
                        }
                        CiVo fromCiVo = getCiByName(fromCi);
                        if (fromCiVo == null) {
                            throw new CiNotFoundException(fromCi);
                        }
                        entityAttrVo.setFromCiVo(fromCiVo);
//                        entityAttrVo.setFromCiId(fromCiVo.getId());
                        String fromAttr = entityAttrVo.getFromAttr();
                        if (StringUtils.isNotBlank(fromAttr)) {
                            if (!fromAttr.startsWith("_")) {
                                AttrVo attrVo = fromCiVo.getAttrByName(fromAttr);
                                if (attrVo == null) {
                                    throw new AttrNotFoundException(fromCi, fromAttr);
                                }
                                entityAttrVo.setFromAttrVo(attrVo);
//                                entityAttrVo.setFromAttrId(attrVo.getId());
//                                entityAttrVo.setFromAttrCiId(attrVo.getCiId());
//                                entityAttrVo.setFromAttrCiName(attrVo.getCiName());
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
//                            entityAttrVo.setToCiId(toCiVo.getId());
                            String toAttr = entityAttrVo.getToAttr();
                            if (StringUtils.isNotBlank(toAttr)) {
                                if (!toAttr.startsWith("_")) {
                                    AttrVo attrVo = toCiVo.getAttrByName(toAttr);
                                    if (attrVo == null) {
                                        throw new AttrNotFoundException(toCi, toAttr);
                                    }
                                    entityAttrVo.setToAttrVo(attrVo);
//                                    entityAttrVo.setToAttrId(attrVo.getId());
//                                    entityAttrVo.setToAttrCiId(attrVo.getCiId());
//                                    entityAttrVo.setToAttrCiName(attrVo.getCiName());
                                }
                            } else if (attrElement.getParent().getName().equalsIgnoreCase("join")) {
                                entityAttrVo.setToAttr("_id");
                            } else {
                                throw new ResourceCenterConfigIrregularException(viewName, "attr", entityAttrVo.getField(), "toAttr");
                            }
                            Integer toCiIsVirtual = toCiVo.getIsVirtual();
//                            entityAttrVo.setToCiIsVirtual(toCiIsVirtual);
                            if (Objects.equals(toCiIsVirtual, 1)) {
                                entityAttrVo.setToAttrCiId(toCiVo.getId());
                                entityAttrVo.setToAttrCiName(toCiVo.getName());
                            }
                            if (StringUtils.isBlank(fromAttr)) {
                                SceneEntityJoinVo joinVo = attToCiJoinMap.get(toCi);
                                if (joinVo != null) {
                                    entityAttrVo.setFromCi(joinVo.getFromCi());
//                                    entityAttrVo.setFromCiId(joinVo.getFromCiVo().getId());
                                    entityAttrVo.setFromCiVo(joinVo.getFromCiVo());
                                    entityAttrVo.setFromAttr(joinVo.getFromAttr());
                                    entityAttrVo.setFromAttrVo(joinVo.getFromAttrVo());
//                                    entityAttrVo.setFromAttrCiId(joinVo.getFromAttrCiId());
//                                    entityAttrVo.setFromAttrCiName(joinVo.getFromAttrCiName());
//                                    entityAttrVo.setFromAttrId(joinVo.getFromAttrId());
                                }
                            }
                        }
                        sceneEntityAttrSet.add(entityAttrVo);
                    }
                    List<Element> relElementList = elementMap.get(viewName + "_rel");
                    if (relElementList == null) {
                        relElementList = getAllChildElement(resourceElement, "rel");
                        elementMap.put(viewName + "_rel", relElementList);
                    }
                    for (Element relElement : relElementList) {
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
//                            entityAttrVo.setToCiId(toCiVo.getId());
                            if (StringUtils.isBlank(fromCi)) {
                                SceneEntityJoinVo joinVo = relToCiJoinMap.get(toCi);
                                entityAttrVo.setFromCi(joinVo.getFromCi());
                                entityAttrVo.setFromCiVo(joinVo.getFromCiVo());
//                                entityAttrVo.setFromCiId(joinVo.getFromCiVo().getId());
                            } else {
                                CiVo fromCiVo = getCiByName(fromCi);
                                if (fromCiVo == null) {
                                    throw new CiNotFoundException(fromCi);
                                }
                                entityAttrVo.setFromCiVo(fromCiVo);
//                                entityAttrVo.setFromCiId(fromCiVo.getId());
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
//                            entityAttrVo.setFromCiId(fromCiVo.getId());
                            if (StringUtils.isBlank(toCi)) {
                                SceneEntityJoinVo joinVo = relFromCiJoinMap.get(fromCi);
                                entityAttrVo.setToCiVo(joinVo.getToCiVo());
//                                entityAttrVo.setToCiId(joinVo.getToCiVo().getId());
                                entityAttrVo.setToCi(joinVo.getToCi());
                            } else {
                                CiVo toCiVo = getCiByName(toCi);
                                if (toCiVo == null) {
                                    throw new CiNotFoundException(toCi);
                                }
                                entityAttrVo.setToCiVo(toCiVo);
//                                entityAttrVo.setToCiId(toCiVo.getId());
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
//                                    entityAttrVo.setFromAttrId(attrVo.getId());
//                                    entityAttrVo.setFromAttrCiId(attrVo.getCiId());
//                                    entityAttrVo.setFromAttrCiName(attrVo.getCiName());
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
//                                    entityAttrVo.setToAttrId(attrVo.getId());
//                                    entityAttrVo.setToAttrCiId(attrVo.getCiId());
//                                    entityAttrVo.setToAttrCiName(attrVo.getCiName());
                                }
                            } else if (relElement.getParent().getName().equalsIgnoreCase("join")) {
                                entityAttrVo.setToAttr("_id");
                            } else {
                                throw new ResourceCenterConfigIrregularException(viewName, "rel", entityAttrVo.getField(), "toAttr");
                            }
                        }
                        sceneEntityAttrSet.add(entityAttrVo);
                    }
                }
                sceneEntityVo.setStatus(Status.PENDING.getValue());
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                sceneEntityVo.setStatus(Status.ERROR.getValue());
                sceneEntityVo.setError(ex.getMessage());
            }
        }
    }

    /**
     * 将<attr></attr>和<rel></rel>标签内容转换成SceneEntityAttrVo
     * @param element
     * @return
     */
    private SceneEntityAttrVo convertToSceneEntityAttrVo(String viewName, Element element, JoinType joinType) {
        String field = element.attributeValue("field");
        String resource = element.attributeValue("resource");
        if (StringUtils.isBlank(field)) {
            throw new ResourceCenterConfigIrregularException(viewName, joinType, "field");
        }
        if (StringUtils.isBlank(resource)) {
            throw new ResourceCenterConfigIrregularException(viewName, joinType, "resource");
        }
        checkResourceAndField(resource, field);
        String direction = element.attributeValue("direction");
        String fromCi = element.attributeValue("fromCi");
        String toCi = element.attributeValue("toCi");
        String fromAttr = element.attributeValue("fromAttr");
        String toAttr = element.attributeValue("toAttr");
        SceneEntityAttrVo attrVo = new SceneEntityAttrVo();
        attrVo.setField(field);
        attrVo.setResource(resource);
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
        String resource = element.attributeValue("resource");
        if (StringUtils.isBlank(field)) {
            throw new ResourceCenterConfigIrregularException(viewName, joinType, "field");
        }
        if (StringUtils.isBlank(resource)) {
            throw new ResourceCenterConfigIrregularException(viewName, joinType, "resource");
        }
        checkResourceAndField(resource, field);
        String direction = element.attributeValue("direction");
        String fromCi = element.attributeValue("fromCi");
        String toCi = element.attributeValue("toCi");
        String fromAttr = element.attributeValue("fromAttr");
        String toAttr = element.attributeValue("toAttr");
        SceneEntityJoinVo joinVo = new SceneEntityJoinVo(joinType);
        joinVo.setField(field);
        joinVo.setResource(resource);
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
        List<ResourceEntityVo> oldResourceEntityList = resourceEntityMapper.getAllResourceEntity();
        oldResourceEntityList.removeAll(resourceEntityList);
        if (CollectionUtils.isNotEmpty(oldResourceEntityList)) {
            for (ResourceEntityVo entity : oldResourceEntityList) {
                resourceEntityMapper.deleteResourceEntityByName(entity.getName());
                schemaMapper.deleteView(TenantContext.get().getDataDbName() + "." + entity.getName());
            }
        }
        if (CollectionUtils.isNotEmpty(resourceEntityList)) {

            for (ResourceEntityVo resourceEntity : resourceEntityList) {
                resourceEntityMapper.insertResourceEntity(resourceEntity);
            }
            for (ResourceEntityVo resourceEntity : resourceEntityList) {
                if (resourceEntity.getStatus().equals(Status.PENDING.getValue())) {
                    Table mainTable = new Table();
                    mainTable.setSchemaName(TenantContext.get().getDbName());
                    mainTable.setName("cmdb_cientity");
                    mainTable.setAlias(new Alias("ci_base"));
                    Select select = SelectUtils.buildSelectFromTableAndSelectItems(mainTable);
                    SelectBody selectBody = select.getSelectBody();
                    PlainSelect plainSelect = (PlainSelect) selectBody;


                    plainSelect.addJoins(new Join()
                            .withRightItem(new SubSelect()
                                    .withSelectBody(buildSubSelectForCi(resourceEntity.getCi()).getSelectBody())
                                    .withAlias(new Alias(resourceEntity.getName().toLowerCase(Locale.ROOT))))
                            .withOnExpression(new EqualsTo().withLeftExpression(new Column()
                                            .withTable(new Table("ci_base"))
                                            .withColumnName("id"))
                                    .withRightExpression(new Column()
                                            .withTable(new Table(resourceEntity.getName()))
                                            .withColumnName("id"))));

                    if (CollectionUtils.isNotEmpty(resourceEntity.getAttrList())) {
                        for (ResourceEntityAttrVo entityAttr : resourceEntity.getAttrList()) {
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

                    if (CollectionUtils.isNotEmpty(resourceEntity.getJoinList())) {
                        List<Join> joinList = new ArrayList<>();
                        for (ResourceEntityJoinVo entityJoin : resourceEntity.getJoinList()) {
                            if (entityJoin.getJoinType() == JoinType.ATTR) {
                                plainSelect.addJoins(new Join()
                                        //.withLeft(true)
                                        .withRightItem(new Table()
                                                .withName("cmdb_attrentity")
                                                .withSchemaName(TenantContext.get().getDbName())
                                                .withAlias(new Alias("cmdb_attrentity_" + entityJoin.getField().toLowerCase(Locale.ROOT))))
                                        .withOnExpression(new EqualsTo()
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
                                            ).withOnExpression(new AndExpression()
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
                                        ).withOnExpression(new EqualsTo()
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
                                        .withOnExpression(new EqualsTo()
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
                                        ).withOnExpression(new EqualsTo()
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
                    try {
                        String sql = "CREATE OR REPLACE VIEW " + TenantContext.get().getDataDbName() + "." + resourceEntity.getName() + " AS " + select;
                        if (logger.isDebugEnabled()) {
                            logger.debug(sql);
                        }
                        schemaMapper.deleteTable(TenantContext.get().getDataDbName() + "." + resourceEntity.getName());
                        schemaMapper.insertView(sql);
                        resourceEntity.setError("");
                        resourceEntity.setStatus(Status.READY.getValue());
                    } catch (Exception ex) {
                        resourceEntity.setError(ex.getMessage());
                        resourceEntity.setStatus(Status.ERROR.getValue());
                        Table table = new Table();
                        table.setName(resourceEntity.getName());
                        table.setSchemaName(TenantContext.get().getDataDbName());
                        List<ColumnDefinition> columnDefinitions = new ArrayList<>();
                        Set<ResourceEntityAttrVo> attrList = resourceEntity.getAttrList();
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
                        schemaMapper.insertView(createTable.toString());
                    } finally {
                        resourceEntityMapper.updateResourceEntity(resourceEntity);
                    }
                }
            }
        }

        //创建场景视图
        for (SceneEntityVo sceneEntityVo : sceneEntityList) {
            if (StringUtils.isNotBlank(sceneEntityVo.getError())) {
                sceneEntityVo.setStatus(Status.ERROR.getValue());
                resourceEntityMapper.insertSceneEntity(sceneEntityVo);
                continue;
            }
            String name = sceneEntityVo.getName();
            SceneEntityGenerateSqlUtil sceneEntityGenerateSqlUtil = new SceneEntityGenerateSqlUtil(sceneEntityVo);
            String sql = sceneEntityGenerateSqlUtil.getSql();
            try {
                sql = "CREATE OR REPLACE VIEW " + TenantContext.get().getDataDbName() + "." + name + " AS " + sql;
                if (logger.isDebugEnabled()) {
                    logger.debug(sql);
                }
                schemaMapper.deleteTable(TenantContext.get().getDataDbName() + "." + name);
                schemaMapper.insertView(sql);
                sceneEntityVo.setError("");
                sceneEntityVo.setStatus(Status.READY.getValue());
            } catch (Exception ex) {
                sceneEntityVo.setError(ex.getMessage());
                sceneEntityVo.setStatus(Status.ERROR.getValue());
            } finally {
                resourceEntityMapper.insertSceneEntity(sceneEntityVo);
            }
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
