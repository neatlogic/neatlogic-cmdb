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

package neatlogic.module.cmdb.startup.handler;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityAttrVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.enums.resourcecenter.ScenceView;
import neatlogic.framework.cmdb.enums.resourcecenter.Status;
import neatlogic.framework.cmdb.enums.resourcecenter.ViewType;
import neatlogic.framework.dao.mapper.SchemaMapper;
import neatlogic.framework.startup.StartupBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author linbq
 * @since 2022/2/9 12:12
 **/
@Component
public class CreateResourceViewStartupHandler extends StartupBase {
    private Logger logger = LoggerFactory.getLogger(CreateResourceViewStartupHandler.class);

    @Resource
    private SchemaMapper schemaMapper;
    @Resource
    private ResourceEntityMapper resourceEntityMapper;
    /**
     * 作业名称
     *
     * @return 字符串
     */
    @Override
    public String getName() {
        return "创建资源中心视图";
    }

    /**
     * 每个租户分别执行
     */
    @Override
    public void executeForCurrentTenant() {
        List<ResourceEntityVo> resourceEntityList = ResourceEntityFactory.getResourceEntityList();
        List<ResourceEntityVo> oldResourceEntityList = resourceEntityMapper.getAllResourceEntity();
        List<ResourceEntityVo> scenceEntityList = new ArrayList<>();
        for (ScenceView scenceView : ScenceView.values()) {
            ResourceEntityVo resourceEntityVo = new ResourceEntityVo();
            resourceEntityVo.setName(scenceView.getValue());
            resourceEntityVo.setLabel(scenceView.getText());
            resourceEntityVo.setType(ViewType.SCENE.getValue());
            resourceEntityVo.setStatus(Status.PENDING.getValue());
            scenceEntityList.add(resourceEntityVo);
        }
        List<ResourceEntityVo> needDeleteList = ListUtils.removeAll(oldResourceEntityList, resourceEntityList);
        needDeleteList = ListUtils.removeAll(needDeleteList, scenceEntityList);
        if (CollectionUtils.isNotEmpty(needDeleteList)) {
            for (ResourceEntityVo entity : needDeleteList) {
                resourceEntityMapper.deleteResourceEntityByName(entity.getName());
                schemaMapper.deleteView(TenantContext.get().getDataDbName() + "." + entity.getName());
            }
        }
        if (CollectionUtils.isNotEmpty(resourceEntityList)) {
            List<ResourceEntityVo> newResourceEntityList = new ArrayList<>();
            for (ResourceEntityVo resourceEntity : resourceEntityList) {
                Set<ResourceEntityAttrVo> attrList = resourceEntity.getAttrList();
                if (CollectionUtils.isNotEmpty(attrList)) {
                    String tableType = schemaMapper.checkTableOrViewIsExists(TenantContext.get().getDataDbName(), resourceEntity.getName());
                    if (StringUtils.isNotBlank(tableType)) {
                        List<String> columnNameList = schemaMapper.getTableOrViewAllColumnNameList(TenantContext.get().getDataDbName(), resourceEntity.getName());
                        for (ResourceEntityAttrVo attrVo : attrList) {
                            //如果已存在的视图需要新增字段，就删除旧视图，先新建一个空表代替视图
                            if (!columnNameList.contains(attrVo.getField())) {
                                newResourceEntityList.add(resourceEntity);
                                if ("BASE TABLE".equals(tableType)) {
                                    logger.debug("删除表：" + resourceEntity.getName());
                                    schemaMapper.deleteTable(TenantContext.get().getDataDbName() + "." + resourceEntity.getName());
                                } else if("VIEW".equals(tableType)) {
                                    schemaMapper.deleteView(TenantContext.get().getDataDbName() + "." + resourceEntity.getName());
                                    logger.debug("删除视图：" + resourceEntity.getName());
                                }
                                break;
                            }
                        }
                    } else {
                        newResourceEntityList.add(resourceEntity);
                    }
                }
            }
            // 如果通过@ResourceType注解定义的视图不存在，先创建具有相同字段的空表代替
            if (CollectionUtils.isNotEmpty(newResourceEntityList)) {
                for (ResourceEntityVo resourceEntity : newResourceEntityList) {
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
                    logger.debug("创建表：" + resourceEntity.getName());
                    schemaMapper.insertView(createTable.toString());
                    ResourceEntityVo resourceEntityVo = new ResourceEntityVo();
                    resourceEntityVo.setType(ViewType.RESOURCE.getValue());
                    resourceEntityVo.setName(resourceEntity.getName());
                    resourceEntityVo.setLabel(resourceEntity.getLabel());
                    resourceEntityVo.setStatus(Status.PENDING.getValue());
                    resourceEntityMapper.insertResourceEntity(resourceEntityVo);
                }
            }
        }
        List<ResourceEntityVo> needInsertList = ListUtils.removeAll(scenceEntityList, oldResourceEntityList);
        for (ResourceEntityVo resourceEntityVo : needInsertList) {
            resourceEntityMapper.insertResourceEntity(resourceEntityVo);
        }
    }

    @Override
    public void executeForAllTenant() {

    }

    /**
     * 排序
     *
     * @return 顺序
     */
    @Override
    public int sort() {
        return 4;
    }
}
