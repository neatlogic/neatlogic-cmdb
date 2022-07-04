/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.utils;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityAttrVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityJoinVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.enums.resourcecenter.JoinType;
import codedriver.framework.cmdb.enums.resourcecenter.Status;
import codedriver.framework.cmdb.exception.attr.AttrNotFoundException;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterConfigIrregularException;
import codedriver.framework.dao.mapper.SchemaMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import codedriver.module.cmdb.service.ci.CiService;
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

import java.util.*;

@Component
public class ResourceEntityViewBuilder {
    private final static Logger logger = LoggerFactory.getLogger(ResourceEntityViewBuilder.class);

    private final static List<String> defaultAttrList = Arrays.asList("_id", "_uuid", "_name", "_fcu", "_fcd", "_lcu", "_lcd", "_inspectStatus", "_inspectTime", "_monitorStatus", "_monitorTime", "_typeId", "_typeName", "_typeLabel");

    private final List<ResourceEntityVo> resourceEntityList = new ArrayList<>();

    public List<ResourceEntityVo> getResourceEntityList() {
        return resourceEntityList;
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
//            List<ResourceEntityVo> oldResourceEntityList = resourceEntityMapper.getAllResourceEntity();
//            oldResourceEntityList.removeAll(resourceEntityList);
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
//                    finally {
//                        resourceEntityMapper.insertResourceEntity(resourceEntityVo);
//                    }
                }
            }

//            if (CollectionUtils.isNotEmpty(oldResourceEntityList)) {
//                for (ResourceEntityVo entity : oldResourceEntityList) {
//                    resourceEntityMapper.deleteResourceEntityByName(entity.getName());
//                    schemaMapper.deleteView(TenantContext.get().getDataDbName() + "." + entity.getName());
//                }
//            }
        } catch (DocumentException e) {
            throw new ResourceCenterConfigIrregularException(e);
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
