/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.utils;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.annotation.ResourceField;
import codedriver.framework.cmdb.annotation.ResourceType;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityAttrVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityJoinVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.enums.resourcecenter.JoinType;
import codedriver.framework.cmdb.enums.resourcecenter.Status;
import codedriver.framework.cmdb.exception.attr.AttrNotFoundException;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterConfigIrregularException;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterConfigMapper;
import codedriver.module.cmdb.service.ci.CiService;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.SelectUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.*;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

@Component
public class ResourceCenterViewBuilder {

    private List<ResourceEntityVo> resourceEntityList;
    private final Map<String, CiVo> ciMap = new HashMap<>();
    private static ResourceCenterConfigMapper resourceCenterConfigMapper;
    private static CiService ciService;


    @Autowired
    public ResourceCenterViewBuilder(ResourceCenterConfigMapper _resourceCenterConfigMapper, CiService _ciService) {
        resourceCenterConfigMapper = _resourceCenterConfigMapper;
        ciService = _ciService;
    }

    private CiVo getCiByName(String ciName) {
        if (!ciMap.containsKey(ciName)) {
            CiVo ciVo = ciService.getCiByName(ciName);
            ciMap.put(ciName, ciVo);
        }
        return ciMap.get(ciName);
    }

    public ResourceCenterViewBuilder(String xml) {
        try {
            resourceEntityList = findResourceEntity();
            if (CollectionUtils.isNotEmpty(resourceEntityList)) {
                Document document = DocumentHelper.parseText(xml);
                Element root = document.getRootElement();
                for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
                    try {
                        Node resourceNode = root.selectSingleNode("//resource[@id='" + resourceEntityVo.getName() + "']");
                        if (resourceNode != null) {
                            Element resourceElement = (Element) resourceNode;
                            String ciName = resourceElement.attributeValue("ci");
                            if (StringUtils.isNotBlank(ciName)) {
                                CiVo ciVo = getCiByName(ciName);
                                if (ciVo != null) {
                                    resourceEntityVo.setCi(ciVo);

                                    if (CollectionUtils.isNotEmpty(resourceEntityVo.getAttrList())) {
                                        //分析属性
                                        for (ResourceEntityAttrVo attr : resourceEntityVo.getAttrList()) {
                                            Node attrNode = resourceElement.selectSingleNode("//attr[@field='" + attr.getField() + "']");
                                            if (attrNode != null) {
                                                Element attrElement = (Element) attrNode;
                                                String attrName = attrElement.attributeValue("attr");
                                                if (StringUtils.isNotBlank(attrName)) {
                                                    attr.setAttr(attrName);
                                                    attr.setCiId(ciVo.getId());
                                                    attr.setCiName(ciName);
                                                    if (!attrName.startsWith("_")) {
                                                        AttrVo attrVo = ciVo.getAttrByName(attrName);
                                                        if (attrVo == null) {
                                                            throw new AttrNotFoundException(ciName, attrName);
                                                        }
                                                        attr.setAttrId(attrVo.getId());
                                                    }
                                                } else if (attrElement.getParent().getName().equalsIgnoreCase("join")) {
                                                    String attrCiName = attrElement.attributeValue("ci");
                                                    if (StringUtils.isNotBlank(attrName)) {
                                                        CiVo attrCiVo = getCiByName(attrCiName);
                                                        AttrVo attrVo = ciVo.getAttrByName(attrName);
                                                        if (attrVo == null) {
                                                            throw new AttrNotFoundException(attrCiName, attrName);
                                                        }
                                                        attr.setAttr("_id");
                                                        attr.setAttrId(attrVo.getId());
                                                        attr.setCiId(attrCiVo.getId());
                                                        attr.setCiName(attrCiName);
                                                    } else {
                                                        throw new ResourceCenterConfigIrregularException(resourceEntityVo.getName(), "attr", attr.getField(), "ci");
                                                    }
                                                } else {
                                                    throw new ResourceCenterConfigIrregularException(resourceEntityVo.getName(), "attr", attr.getField(), "attr");
                                                }
                                            } else {
                                                throw new ResourceCenterConfigIrregularException(resourceEntityVo.getName(), "attr", attr.getField());
                                            }
                                        }
                                        //分析连接查询
                                        Element joinElement = resourceElement.element("join");
                                        if (joinElement != null) {
                                            List<Element> attrElementList = joinElement.elements("attr");
                                            if (CollectionUtils.isNotEmpty(attrElementList)) {
                                                for (Element attrElement : attrElementList) {
                                                    String attrCiName = attrElement.attributeValue("ci");
                                                    if (StringUtils.isNotBlank(attrCiName)) {
                                                        CiVo joinCiVo = getCiByName(attrCiName);
                                                        if (joinCiVo == null) {
                                                            throw new CiNotFoundException(attrCiName);
                                                        }
                                                        ResourceEntityJoinVo joinVo = new ResourceEntityJoinVo(JoinType.ATTR);
                                                        joinVo.setCi(joinCiVo);
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
                    } finally {
                        resourceCenterConfigMapper.insertResourceEntity(resourceEntityVo);
                    }
                }
            }
        } catch (DocumentException e) {
            throw new ResourceCenterConfigIrregularException(e);
        }
    }


    public void buildView() {
        if (CollectionUtils.isNotEmpty(resourceEntityList)) {
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
                                    .withAlias(new Alias(resourceEntity.getName())))
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
                                selectItem.setExpression(new Column(resourceEntity.getName() + "." + entityAttr.getAttr().substring(1)));
                            } else if (entityAttr.getAttrId() != null) {
                                selectItem.setExpression(new Column(resourceEntity.getName() + ".`" + entityAttr.getAttrId() + "`"));
                            }
                            selectItem.setAlias(new Alias(entityAttr.getField()));
                            plainSelect.addSelectItems(selectItem);
                        }
                    }

                    if (CollectionUtils.isNotEmpty(resourceEntity.getJoinList())) {
                        List<Join> joinList = new ArrayList<>();
                        for (ResourceEntityJoinVo entityJoin : resourceEntity.getJoinList()) {
                            if (entityJoin.getJoinType() == JoinType.ATTR) {

                                plainSelect.addJoins(new Join()
                                        .withLeft(true)
                                        .withRightItem(new Table()
                                                .withName("cmdb_attrentity")
                                                .withSchemaName(TenantContext.get().getDbName()))
                                        .withOnExpression(new EqualsTo()
                                                .withLeftExpression(new Column()
                                                        .withTable(new Table("ci_base"))
                                                        .withColumnName("id"))
                                                .withRightExpression(new Column()
                                                        .withTable(new Table("cmdb_attrentity"))
                                                        .withColumnName("from_cientity_id"))));

                                plainSelect.addJoins(new Join()
                                        .withLeft(true)
                                        .withRightItem(new SubSelect()
                                                .withSelectBody(buildSubSelectForCi(entityJoin.getCi()).getSelectBody())
                                                .withAlias(new Alias(entityJoin.getCi().getName()))
                                        ).withOnExpression(new EqualsTo()
                                                .withLeftExpression(new Column()
                                                        .withTable(new Table("cmdb_attrentity"))
                                                        .withColumnName("to_cientity_id"))
                                                .withRightExpression(new Column()
                                                        .withTable(new Table(entityJoin.getCi().getName()))
                                                        .withColumnName(entityJoin.getCi().getIsVirtual().equals(0) ? "cientity_id" : "id"))));
                            }
                        }
                        if (CollectionUtils.isNotEmpty(joinList)) {
                            plainSelect.addJoins(joinList);
                        }
                    }

                    System.out.println(select.toString());

                }
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
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("id").withTable(new Table("ci_base"))));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("name").withTable(new Table("ci_base"))));
            for (AttrVo attrVo : ciVo.getAttrList()) {
                if (attrVo.getTargetCiId() == null) {
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("`" + attrVo.getId() + "`").withTable(new Table("cmdb_" + attrVo.getCiId()))));
                }
            }
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


    private List<ResourceEntityVo> findResourceEntity() {
        List<ResourceEntityVo> resourceEntityList = new ArrayList<>();
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
        return resourceEntityList;
    }
}
