/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.service.cientity;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.AttrFilterVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.RelFilterVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrFilterVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionGroupVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionStatusVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface CiEntityService {
    CiEntityVo getCiEntityById(CiEntityVo ciEntityVo);

    /**
     * 按明确属性投影读取配置项；空属性列表表示不读取任何属性和关系。
     */
    CiEntityVo getCiEntityByIdForProjection(Long ciId, Long ciEntityId,
                                             List<Long> attrIdList, Integer maxAttrEntity);



    List<CiEntityVo> getCiEntityNameByIdList(Long ciId, List<Long> ciEntityIdList);

    //List<CiEntityVo> getCiEntityByIdList(CiEntityVo ciEntityVo);

    List<Long> getCiEntityIdByCiId(CiEntityVo ciEntityVo);

    /**
     * 查询配置项
     *
     * @param ciEntityVo 条件
     * @return 配置项列表
     */
    List<CiEntityVo> searchCiEntity(CiEntityVo ciEntityVo);

    Long saveCiEntityWithoutTransaction(List<CiEntityTransactionVo> ciEntityTransactionList, TransactionGroupVo transactionGroupVo);

    @Transactional
    Long saveCiEntity(List<CiEntityTransactionVo> ciEntityTransactionList, TransactionGroupVo transactionGroupVo);

    /**
     * 保存配置项
     *
     * @param ciEntityTransactionVo 配置项事务
     * @return 事务id
     */
    @Transactional
    Long saveCiEntity(CiEntityTransactionVo ciEntityTransactionVo);

    CiEntityVo getCiEntityBaseInfoById(Long ciEntityId);

    List<CiEntityVo> getCiEntityBaseInfoByName(Long ciId, String name);

    CiEntityVo getCiEntityById(Long ciId, Long ciEntityId, Integer maxAttrEntity, Integer maxRelEntity);

    /**
     * 获取单个配置项详细信息
     *
     * @param ciEntityId 配置项id
     * @param ciId       模型id
     * @return ciEntityVo
     */
    CiEntityVo getCiEntityById(Long ciId, Long ciEntityId);

    /**
     * 保存配置项
     *
     * @param ciEntityTransactionVo 配置项事务
     * @param transactionGroupVo    配置项事务组
     * @return 事务id
     */
    @Transactional
    Long saveCiEntity(CiEntityTransactionVo ciEntityTransactionVo, TransactionGroupVo transactionGroupVo);

    void updateCiEntityName(CiEntityVo ciEntityVo);

    /**
     * 根据模型名字表达式修改配置项名称
     *
     * @param ciVo 模型
     */
    void updateCiEntityNameForCi(CiVo ciVo);

    void createSnapshot(CiEntityTransactionVo ciEntityTransactionVo);

    /**
     * 验证配置项是否合法
     *
     * @param ciEntityTransactionVo 配置项事务
     * @return 是否有变化
     */
    boolean validateCiEntityTransaction(CiEntityTransactionVo ciEntityTransactionVo);

    void rebuildAttrEntityIndex(Long attrId, Long fromCiEntityId);

    void rebuildRelEntityIndex(RelDirectionType direction, Long relId, Long ciEntityId);

    void updateCiEntity(CiEntityVo ciEntityVo);

    /**
     * 提交事务组
     *
     * @param transactionGroupVo 事务组（包含事务对象列表）
     * @return 状态
     */
    List<TransactionStatusVo> commitTransactionGroup(TransactionGroupVo transactionGroupVo);

    List<CiEntityVo> searchCiEntityBaseInfo(CiEntityVo ciEntityVo);


    @Transactional
    Long deleteCiEntityList(List<CiEntityVo> ciEntityList, Boolean allowCommit);

    /**
     * 删除整个配置项
     *
     * @param ciEntityVo 配置项
     * @return 事务id
     */
    @Transactional
    Long deleteCiEntity(CiEntityVo ciEntityVo, Boolean allowCommit, TransactionGroupVo transactionGroupVo);

    @Transactional
    Long deleteCiEntity(CiEntityVo ciEntityVo, Boolean allowCommit);


    /**
     * 批量保存多个配置项
     *
     * @param ciEntityTransactionList 事务列表
     * @return 事务组id
     */
    Long saveCiEntity(List<CiEntityTransactionVo> ciEntityTransactionList);

    @Transactional
    void recoverCiEntity(TransactionVo transactionVo);

    @Transactional
    void recoverTransactionGroup(Long transactionGroupId);

    /**
     * 根据模型id获取所有属性和关系map
     *
     * @param ciId      模型id
     * @param attrMap   属性map
     * @param relMap    关系map
     * @param ciViewMap 显示map
     * @param globalAttrMap 全局属性map
     */
    void getCiViewMapAndAttrMapAndRelMap(Long ciId, Map<Long, AttrVo> attrMap, Map<Long, RelVo> relMap, Map<String, CiViewVo> ciViewMap, Map<Long, GlobalAttrVo> globalAttrMap);


    RelFilterVo convertFromRelFilter(RelVo relVo, String expression, List<String> valueList, String direction);

    AttrFilterVo convertAttrFilter(AttrVo attrVo, String expression, List<String> valueList);

    GlobalAttrFilterVo convertGlobalAttrFilter(GlobalAttrVo globalAttrVo, String expression, List<String> valueList);

    /**
     * 查询配置项，构造tbodyList的每一行
     *
     * @param viewConstNameList 回显的固化属性
     * @param ciEntity 配置项
     */
    JSONObject getTbodyRowData(List<String> viewConstNameList, CiEntityVo ciEntity);

    /**
     * 查询配置项，构造tbodyList的每一行
     *
     * @param ciEntity 配置项
     */
    JSONObject getTbodyRowData(CiEntityVo ciEntity);
}
