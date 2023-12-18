/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.service.ci;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.crossover.ICiCrossoverService;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.exception.attr.AttrIsUsedInExpressionException;
import neatlogic.framework.cmdb.exception.attr.AttrIsUsedInUniqueRuleException;
import neatlogic.framework.cmdb.exception.ci.*;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.dao.mapper.DataBaseViewInfoMapper;
import neatlogic.framework.dao.mapper.SchemaMapper;
import neatlogic.framework.dto.DataBaseViewInfoVo;
import neatlogic.framework.exception.database.DataBaseNotFoundException;
import neatlogic.framework.lrcode.LRCodeManager;
import neatlogic.framework.transaction.core.AfterTransactionJob;
import neatlogic.framework.transaction.core.EscapeTransactionJob;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import neatlogic.module.cmdb.utils.VirtualCiSqlBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CiServiceImpl implements CiService, ICiCrossoverService {

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;


    @Resource
    private CiSchemaMapper ciSchemaMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private SchemaMapper schemaMapper;

    @Resource
    private DataBaseViewInfoMapper dataBaseViewInfoMapper;

    @Override
    @Transactional
    public void insertCi(CiVo ciVo) {
        if (ciMapper.checkCiNameIsExists(ciVo) > 0) {
            throw new CiNameIsExistsException(ciVo.getName());
        }
        if (ciMapper.checkCiLabelIsExists(ciVo) > 0) {
            throw new CiLabelIsExistsException(ciVo.getLabel());
        }


        int lft = LRCodeManager.beforeAddTreeNode("cmdb_ci", "id", "parent_ci_id", ciVo.getParentCiId());
        ciVo.setLft(lft);
        ciVo.setRht(lft + 1);
        ciMapper.insertCi(ciVo);
        //如果父模型有配置显示视图，则复制一份，加快配置效率
        if (ciVo.getParentCiId() != null) {
            List<CiViewVo> ciViewList = ciViewMapper.getCiViewBaseInfoByCiId(ciVo.getParentCiId());
            if (CollectionUtils.isNotEmpty(ciViewList)) {
                for (CiViewVo ciViewVo : ciViewList) {
                    ciViewVo.setCiId(ciVo.getId());
                    ciViewMapper.insertCiView(ciViewVo);
                }
            }
        }

        if (Objects.equals(ciVo.getIsVirtual(), 1)) {
            buildCiView(ciVo);
        } else if (Objects.equals(ciVo.getIsVirtual(), 0)) {
            EscapeTransactionJob.State s = buildCiSchema(ciVo);
            if (!s.isSucceed()) {
                throw new CreateCiSchemaException(ciVo.getName());
            }
        }
    }

    private EscapeTransactionJob.State buildCiSchema(CiVo ciVo) {
        return new EscapeTransactionJob(() -> {
            if (ciSchemaMapper.checkSchemaIsExists(TenantContext.get().getDataDbName()) > 0) {
                if (ciSchemaMapper.checkTableIsExists(TenantContext.get().getDataDbName(), "cmdb_" + ciVo.getId()) <= 0) {
                    //创建配置项表
                    ciSchemaMapper.insertCiTable(ciVo.getId(), ciVo.getCiTableName());
                } else {
                    //如果已存在但没有数据，重建表
                    if (ciSchemaMapper.checkTableHasData(ciVo.getCiTableName()) <= 0) {
                        ciSchemaMapper.deleteCiTable(ciVo.getId(), ciVo.getCiTableName());
                        ciSchemaMapper.insertCiTable(ciVo.getId(), ciVo.getCiTableName());
                        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
                        for (AttrVo attrVo : attrList) {
                            //这里的attrlist包含了所有集成模型的属性，不是自己模型的属性就不要添加
                            if (attrVo.getCiId().equals(ciVo.getId()) && attrVo.getTargetCiId() == null) {
                                ciSchemaMapper.insertAttrToCiTable(ciVo.getId(), ciVo.getCiTableName(), attrVo);
                            }
                        }
                    }
                }
            } else {
                throw new DataBaseNotFoundException();
            }
        }).execute();
    }

    @Override
    public void buildCiView(CiVo ciVo) {
        VirtualCiSqlBuilder viewBuilder = new VirtualCiSqlBuilder(ciVo.getViewXml());
        viewBuilder.setCiId(ciVo.getId());

        if (viewBuilder.valid()) {
            //测试一下语句是否能正常执行
            try {
                ciSchemaMapper.testCiViewSql(viewBuilder.getTestSql());
            } catch (Exception ex) {
                throw new CiViewSqlIrregularException(ex);
            }
            List<AttrVo> attrList = viewBuilder.getAttrList();
            if (CollectionUtils.isNotEmpty(attrList)) {
                Map<String, Long> attrIdMap = new HashMap<>();
                List<AttrVo> oldAttrList = attrMapper.getAttrByCiId(ciVo.getId());
                Map<String, Long> oldAttrMap = new HashMap<>();
                Map<Long, Boolean> attrExistsMap = new HashMap<>();
                if (CollectionUtils.isNotEmpty(oldAttrList)) {
                    for (AttrVo attrVo : oldAttrList) {
                        oldAttrMap.put(attrVo.getName(), attrVo.getId());
                        attrExistsMap.put(attrVo.getId(), false);
                    }
                }
                for (AttrVo attrVo : attrList) {
                    if (!oldAttrMap.containsKey(attrVo.getName())) {
                        attrVo.setCiId(ciVo.getId());
                        attrMapper.insertAttr(attrVo);
                    } else {
                        //还原原来的id
                        attrVo.setId(oldAttrMap.get(attrVo.getName()));
                        attrMapper.updateAttr(attrVo);
                        attrExistsMap.put(attrVo.getId(), true);
                    }
                    attrIdMap.put(attrVo.getName(), attrVo.getId());
                }
                //删除没用的属性
                for (Long key : attrExistsMap.keySet()) {
                    if (!attrExistsMap.get(key)) {
                        attrMapper.deleteAttrById(key);
                    }
                }
                viewBuilder.setAttrIdMap(attrIdMap);
                String selectSql = viewBuilder.getSql();
                String md5 = Md5Util.encryptMD5(selectSql);
                String viewName = "cmdb_" + ciVo.getId();
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
                EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                    String sql = "CREATE OR REPLACE VIEW " + TenantContext.get().getDataDbName() + "." + viewName + " AS " + selectSql;
                    if (ciSchemaMapper.checkSchemaIsExists(TenantContext.get().getDataDbName()) > 0) {
                        //创建配置项表
                        ciSchemaMapper.insertCiView(sql);
                    } else {
                        throw new DataBaseNotFoundException();
                    }
                }).execute();
                if (!s.isSucceed()) {
                    throw new CreateCiSchemaException(ciVo.getName(), true);
                }
                DataBaseViewInfoVo dataBaseViewInfoVo = new DataBaseViewInfoVo();
                dataBaseViewInfoVo.setViewName(viewName);
                dataBaseViewInfoVo.setMd5(md5);
                dataBaseViewInfoVo.setLcu(UserContext.get().getUserUuid());
                dataBaseViewInfoMapper.insertDataBaseViewInfo(dataBaseViewInfoVo);
            }
        }
    }

    @Override
    @Transactional
    public void updateCiUnique(Long ciId, List<Long> attrIdList) {
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        ciMapper.deleteCiUniqueByCiId(ciId);
        if (CollectionUtils.isNotEmpty(attrIdList)) {
            for (Long attrId : attrIdList) {
                if (attrList.stream().noneMatch(attr -> attr.getId().equals(attrId))) {
                    throw new CiUniqueRuleHasNotExistsAttrException(attrId);
                }
                ciMapper.insertCiUnique(ciId, attrId);
            }
        }
    }

    @Override
    @Transactional
    public void updateCiNameAttrId(CiVo ciVo) {
        ciMapper.updateCiNameAttrId(ciVo);
        AfterTransactionJob<CiVo> job = new AfterTransactionJob<>("UPDATE-CIENTITY-NAME-" + ciVo.getId());
        job.execute(ciVo, dataCiVo -> ciEntityService.updateCiEntityNameForCi(dataCiVo));
    }


    @Override
    @Transactional
    public void updateCi(CiVo ciVo) {
        CiVo checkCiVo = ciMapper.getCiById(ciVo.getId());
        if (checkCiVo == null) {
            throw new CiNotFoundException(ciVo.getId());
        }
        List<AttrVo> parentAttrList = null;
        boolean needRebuildLRCode = false;
        if (!Objects.equals(checkCiVo.getParentCiId(), ciVo.getParentCiId())) {

            //如果继承发生改变需要检查是否有配置项数据，有数据不允许变更
            int ciEntityCount = ciEntityMapper.getDownwardCiEntityCountByLR(ciVo.getLft(), ciVo.getRht());
            if (ciEntityCount > 0) {
                //有关系的情况太复杂，例如父模型引用了子模型，这部分数据能否删除？想清楚之前暂时不能自动清理带关系的数据数据
                if (ciVo.getParentCiId() != null || CollectionUtils.isNotEmpty(relMapper.getRelByCiId(ciVo.getId()))) {
                    throw new CiParentCanNotBeChangedException(ciVo.getName(), ciEntityCount);
                } else {
                    List<CiVo> parentCiList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                    parentCiList.removeIf(d -> d.getId().equals(ciVo.getId()));
                    //清空继承需要清除父模型的数据
                    if (CollectionUtils.isNotEmpty(parentCiList)) {
                        ciEntityMapper.deleteParentCiEntity(ciVo, parentCiList);
                    }
                }
            }

            parentAttrList = attrMapper.getAttrByCiId(checkCiVo.getParentCiId());
            if (CollectionUtils.isNotEmpty(parentAttrList)) {
                //检查子模型的表达式属性是否引用了父模型的属性
                List<AttrVo> attrExpressionList = attrMapper.getExpressionAttrByValueCiIdAndAttrIdList(ciVo.getId(), parentAttrList.stream().map(AttrVo::getId).collect(Collectors.toList()));
                attrExpressionList.removeIf(attr -> !attr.getCiId().equals(ciVo.getId()));
                if (CollectionUtils.isNotEmpty(attrExpressionList)) {
                    throw new AttrIsUsedInExpressionException(attrExpressionList);
                }

                //检查唯一规则是否有被引用
                if (CollectionUtils.isNotEmpty(checkCiVo.getUniqueAttrIdList())) {
                    for (Long uaId : checkCiVo.getUniqueAttrIdList()) {
                        if (parentAttrList.stream().anyMatch(attr -> attr.getId().equals(uaId))) {
                            throw new AttrIsUsedInUniqueRuleException();
                        }
                    }
                }

                //检查名称属性是否有被引用
               /* if (checkCiVo.getNameAttrId() != null && parentAttrList.stream().anyMatch(a -> a.getId().equals(checkCiVo.getNameAttrId()))) {
                    throw new AttrIsUsedInNameAttrException();
                }*/
            }
            needRebuildLRCode = true;
            //LRCodeManager.moveTreeNode("cmdb_ci", "id", "parent_ci_id", ciVo.getId(), MoveType.INNER, ciVo.getParentCiId());
        }
        if (ciMapper.checkCiNameIsExists(ciVo) > 0) {
            throw new CiNameIsExistsException(ciVo.getName());
        }
        if (ciMapper.checkCiLabelIsExists(ciVo) > 0) {
            throw new CiLabelIsExistsException(ciVo.getLabel());
        }
        if (Objects.equals(ciVo.getIsVirtual(), 1)) {
            buildCiView(ciVo);
        } else if (Objects.equals(ciVo.getIsVirtual(), 0)) {
            EscapeTransactionJob.State s = buildCiSchema(ciVo);
            if (!s.isSucceed()) {
                throw new CreateCiSchemaException(ciVo.getName());
            }
        }
        ciMapper.updateCi(ciVo);
        if (!Objects.equals(checkCiVo.getParentCiId(), ciVo.getParentCiId())) {
            //由于名称属性一旦选中不能取消，所以直接修改，不告警
            if (checkCiVo.getNameAttrId() != null && CollectionUtils.isNotEmpty(parentAttrList) && parentAttrList.stream().anyMatch(a -> a.getId().equals(checkCiVo.getNameAttrId()))) {
                ciVo.setNameAttrId(null);
                ciMapper.updateCiNameAttrId(ciVo);
            }

            //更换父模型的视图数据
            ciViewMapper.deleteCiViewByCiId(ciVo.getId());
            if (ciVo.getParentCiId() != null) {
                List<CiViewVo> ciViewList = ciViewMapper.getCiViewBaseInfoByCiId(ciVo.getParentCiId());
                if (CollectionUtils.isNotEmpty(ciViewList)) {
                    for (CiViewVo ciViewVo : ciViewList) {
                        ciViewVo.setCiId(ciVo.getId());
                        ciViewMapper.insertCiView(ciViewVo);
                    }
                }
            }
        }
        if (needRebuildLRCode) {
            //重建所有左右编码，性能差点但可靠
            LRCodeManager.rebuildLeftRightCode("cmdb_ci", "id", "parent_ci_id");
        }
        if (checkCiVo.getExpiredDay() != ciVo.getExpiredDay()) {
            AfterTransactionJob<CiVo> job = new AfterTransactionJob<>("REFRESH-CIENTITY-EXPIREDTIME-" + ciVo.getId());
            job.execute(ciVo, _ciVo -> {
                //修正配置项超时数据
                if (_ciVo.getExpiredDay() == 0) {
                    ciEntityMapper.deleteCiEntityExpiredTimeByCiId(_ciVo.getId());
                } else {
                    CiEntityVo pCiEntityVo = new CiEntityVo();
                    pCiEntityVo.setCiId(_ciVo.getId());
                    pCiEntityVo.setPageSize(100);
                    pCiEntityVo.setCurrentPage(1);
                    List<CiEntityVo> ciEntityList = ciEntityMapper.searchCiEntityBaseInfo(pCiEntityVo);
                    while (CollectionUtils.isNotEmpty(ciEntityList)) {
                        for (CiEntityVo cientity : ciEntityList) {
                            if (cientity.getRenewTime() != null) {
                                Calendar c = Calendar.getInstance();
                                c.setTime(cientity.getRenewTime());
                                c.add(Calendar.DAY_OF_YEAR, ciVo.getExpiredDay());
                                cientity.setExpiredTime(c.getTime());
                                cientity.setExpiredDay(ciVo.getExpiredDay());
                                ciEntityMapper.insertCiEntityExpiredTime(cientity);
                            }
                        }
                        pCiEntityVo.setCurrentPage(pCiEntityVo.getCurrentPage() + 1);
                        ciEntityList = ciEntityMapper.searchCiEntityBaseInfo(pCiEntityVo);
                    }
                }
            });

        }
    }

    @Override
    public int deleteCi(Long ciId) {
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        //检查当前模型是否被继承
        List<CiVo> childCiList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        if (childCiList.size() > 1) {
            throw new CiHasBeenExtendedException(ciVo.getLabel(), childCiList.subList(1, childCiList.size()));
        }
        // 检查当前模型是否有被引用
        List<CiVo> fromCiList = ciMapper.getCiByToCiId(ciId);
        if (CollectionUtils.isNotEmpty(fromCiList)) {
            throw new CiHasRelException(
                    fromCiList.stream().map(CiVo::getLabel).collect(Collectors.joining("、")));
        }
        List<CiVo> toCiList = ciMapper.getCiByFromCiId(ciId);
        if (CollectionUtils.isNotEmpty(toCiList)) {
            throw new CiHasRelException(
                    toCiList.stream().map(CiVo::getLabel).collect(Collectors.joining("、")));
        }
        //检查是否被属性引用
        List<CiVo> attrCiList = ciMapper.getCiByTargetCiId(ciId);
        if (CollectionUtils.isNotEmpty(attrCiList)) {
            throw new CiHasAttrException(
                    attrCiList.stream().map(CiVo::getLabel).collect(Collectors.joining("、")));
        }
        //检查模型以及子模型是否有数据
        int ciEntityCount = ciEntityMapper.getDownwardCiEntityCountByLR(ciVo.getLft(), ciVo.getRht());
        if (ciEntityCount > 0) {
            throw new CiIsNotEmptyException(ciId, ciEntityCount);
        }

        //检查是否被自定义视图引用
        List<CustomViewVo> viewList = ciMapper.getCustomViewByCiId(ciVo.getId());
        if (CollectionUtils.isNotEmpty(viewList)) {
            throw new CiIsUsedInCustomViewException(ciVo, viewList);
        }

        //TODO 检查是否被资源中心引用

        if (StringUtils.isNotBlank(ciVo.getCiTableName())) {
            if (ciVo.getIsVirtual().equals(0)) {
                ciSchemaMapper.deleteCiTable(ciVo.getId(), ciVo.getCiTableName());
            } else {
                ciSchemaMapper.deleteCiView(ciVo.getCiTableName());
            }
        }

        //清除模型数据
        LRCodeManager.beforeDeleteTreeNode("cmdb_ci", "id", "parent_ci_id", ciId);
        ciMapper.deleteCiById(ciId);
        return 0;
    }


    @Override
    public CiVo getCiById(Long id) {
        CiVo ciVo = ciMapper.getCiById(id);
        if (ciVo == null) {
            throw new CiNotFoundException(id);
        }
        if (ciVo.getIsVirtual().equals(0)) {
            ciVo.setUpwardCiList(ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht()));
        }
        List<AttrVo> attrList = attrMapper.getAttrByCiId(id);
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(id));
        List<GlobalAttrVo> globalAttrList = globalAttrMapper.getGlobalAttrByCiId(id);
        ciVo.setRelList(relList);
        ciVo.setAttrList(attrList);
        ciVo.setGlobalAttrList(globalAttrList);
        return ciVo;
    }

    @Override
    public CiVo getCiByName(String ciName) {
        CiVo ciVo = ciMapper.getCiByName(ciName);
        if (ciVo == null) {
            throw new CiNotFoundException(ciName);
        }
        if (ciVo.getIsVirtual().equals(0)) {
            ciVo.setUpwardCiList(ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht()));
        }
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciVo.getId()));
        ciVo.setRelList(relList);
        ciVo.setAttrList(attrList);
        return ciVo;
    }

    @Override
    public void initCiTableView() {
        List<CiVo> ciList = ciMapper.searchCi(new CiVo());
        ciList.sort(Comparator.comparing(CiVo::getLft));
        Map<Long, CiVo> ciMap = ciList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        for (CiVo ciVo : ciList) {
            if (ciVo.getIsVirtual().equals(0)) {
                List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
                ciVo.setAttrList(attrList);
                // 生成动态表
                ciSchemaMapper.initCiTable(ciVo.getId(), ciVo);
                // 向动态表中插入数据
                CiVo tempCi = ciVo;
                List<Long> upwardIdList = new ArrayList<>();
                upwardIdList.add(tempCi.getId());
                while (tempCi.getParentCiId() != null) {
                    upwardIdList.add(tempCi.getParentCiId());
                    tempCi = ciMap.get(tempCi.getParentCiId());
                }
                CiEntityVo search = new CiEntityVo();
                search.setCiId(ciVo.getId());
                search.setPageSize(100);
                List<Long> ciEntityIdList = ciEntityMapper.getCiEntityIdByCiId(search);
                for (Long ciEntityId : ciEntityIdList) {
                    CiEntityVo ciEntityVo = new CiEntityVo();
                    ciEntityVo.setId(ciEntityId);
                    for (Long ciId : upwardIdList) {
                        ciEntityVo.setCiId(ciId);
                        ciEntityMapper.insertCiEntity(ciEntityVo);
                    }
                }
            } else {
                //创建视图
                String viewXml = ciMapper.getCiViewXmlById(ciVo.getId());
                if (StringUtils.isNotBlank(viewXml)) {
                    ciVo.setViewXml(viewXml);
                    buildCiView(ciVo);
                }
            }
        }
    }
}
