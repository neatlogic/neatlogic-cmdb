/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.startup.handler;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityAttrVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.customview.ICustomView;
import codedriver.framework.dao.mapper.SchemaMapper;
import codedriver.framework.startup.IStartup;
import codedriver.module.cmdb.utils.ResourceCustomViewFactory;
import codedriver.module.cmdb.utils.ResourceEntityFactory;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.apache.commons.collections4.CollectionUtils;
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
public class CreateResourceViewStartupHandler implements IStartup {
    private Logger logger = LoggerFactory.getLogger(CreateResourceViewStartupHandler.class);

    @Resource
    private SchemaMapper schemaMapper;
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
        List<ICustomView> custonViewList = ResourceCustomViewFactory.getCustomViewList();
        List<ResourceEntityVo> resourceEntityList = ResourceEntityFactory.getResourceEntityList();
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
                }
            }
        }
        // 创建自定义视图
        for (ICustomView custonView : custonViewList) {
            String selectBody = custonView.getSelectBody();
            if (StringUtils.isNotBlank(selectBody)) {
                try {
                    String sql = "CREATE OR REPLACE VIEW " + TenantContext.get().getDataDbName() + "." + custonView.getName() + " AS " + selectBody;
                    if (logger.isDebugEnabled()) {
                        logger.debug(sql);
                    }
                    schemaMapper.insertView(sql);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
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
