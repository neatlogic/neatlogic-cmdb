/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.cientity;

import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.framework.cmdb.dto.transaction.TransactionStatusVo;
import codedriver.framework.cmdb.dto.transaction.TransactionVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CiEntityService {
    /**
     * 根据配置项id列表返回配置项
     *
     * @param ciId           模型id
     * @param ciEntityIdList 配置项id列表
     * @return 配置项列表
     */
    List<CiEntityVo> getCiEntityByIdList(Long ciId, List<Long> ciEntityIdList);

    /**
     * 根据配置项id列表返回配置项
     *
     * @param ciId           模型id
     * @param ciEntityIdList 配置项id列表
     * @param groupIdList    圈子id列表
     * @return 配置项列表
     */
    List<CiEntityVo> getCiEntityByIdList(Long ciId, List<Long> ciEntityIdList, List<Long> groupIdList);

    List<CiEntityVo> searchCiEntity(CiEntityVo ciEntityVo);

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
    Long deleteCiEntityList(List<Long> ciEntityIdList, Boolean allowCommit);

    /**
     * 删除整个配置项
     *
     * @param ciEntityId 配置项id
     * @return 事务id
     */
    @Transactional
    Long deleteCiEntity(Long ciEntityId, Boolean allowCommit, TransactionGroupVo transactionGroupVo);

    @Transactional
    Long deleteCiEntity(Long ciEntityId, Boolean allowCommit);

    /**
     * 验证配置项是否合法
     *
     * @param ciEntityTransactionVo 事务
     * @return 是否合法
     */
    boolean validateCiEntity(CiEntityTransactionVo ciEntityTransactionVo);

    /**
     * 批量保存多个配置项
     *
     * @param ciEntityTransactionList 事务列表
     * @return 事务组id
     */
    Long saveCiEntity(List<CiEntityTransactionVo> ciEntityTransactionList);

    @Transactional
    void recoverCiEntity(TransactionVo transactionVo);

}
