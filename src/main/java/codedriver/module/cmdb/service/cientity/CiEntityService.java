package codedriver.module.cmdb.service.cientity;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;

public interface CiEntityService {
    public List<CiEntityVo> searchCiEntity(CiEntityVo ciEntityVo);

    @Transactional
    /**
     * @Author: chenqiwei
     * @Time:Sep 2, 2020
     * @Description: 保存配置项
     * @param @param
     *            ciEntityVo
     * @param @param
     *            action
     * @param @return
     * @return Long 事务id
     */
    public Long saveCiEntity(CiEntityTransactionVo ciEntityTransactionVo, TransactionActionType action);

    /**
     * @Author: chenqiwei
     * @Time:2020年11月11日
     * @Description: 获取配置项详细信息
     * @param @param
     *            ciEntityId
     * @param @return
     * @return CiEntityVo
     */
    public CiEntityVo getCiEntityDetailById(Long ciEntityId);

    public Long commitTransaction(Long transactionId);

    /**
     * 
     * @Author: chenqiwei
     * @Time:2020年11月11日
     * @Description: 删除单个配置项
     * @param @param
     *            ciEntityId
     * @param @return
     * @return Long
     */
    public Long deleteCiEntity(Long ciEntityId);

    /**
    * @Author: chenqiwei
    * @Time:2020年11月11日
    * @Description: 根据配置项id和视图配置查询配置项 
    * @param @param ciEntityIdList
    * @param @param ciEntityVo
    * @param @return 
    * @return List<CiEntityVo>
     */
    public List<CiEntityVo> searchCiEntityByIds(List<Long> ciEntityIdList, CiEntityVo ciEntityVo);

}
