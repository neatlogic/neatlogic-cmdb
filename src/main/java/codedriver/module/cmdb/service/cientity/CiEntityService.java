package codedriver.module.cmdb.service.cientity;

import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CiEntityService {
    public List<CiEntityVo> searchCiEntity(CiEntityVo ciEntityVo);

    /**
     * @param @param  ciEntityTransactionVo
     * @param @return
     * @return Long 事务id
     * @Author: chenqiwei
     * @Time: 2020年11月17日
     * @Description: 保存配置项
     */
    @Transactional
    public Long saveCiEntity(CiEntityTransactionVo ciEntityTransactionVo);

    /**
     * @param @param  ciEntityId
     * @param @return
     * @return CiEntityVo
     * @Author: chenqiwei
     * @Time: 2020年11月11日
     * @Description: 获取配置项详细信息
     */
    public CiEntityVo getCiEntityDetailById(Long ciEntityId);

    void createCiEntityName(CiEntityTransactionVo ciEntityTransactionVo);

    void createSnapshot(CiEntityTransactionVo ciEntityTransactionVo);


    public Long commitTransaction(Long transactionId);

    /*
     * @Description: 删除整个配置项
     * @Author: chenqiwei
     * @Date: 2021/1/5 3:50 下午
     * @Params: [ciEntityId]
     * @Returns: java.lang.Long
     **/
    public Long deleteCiEntity(Long ciEntityId);

    /**
     * @param @param  ciEntityIdList
     * @param @param  ciEntityVo
     * @param @return
     * @return List<CiEntityVo>
     * @Author: chenqiwei
     * @Time: 2020年11月11日
     * @Description: 根据配置项id和视图配置查询配置项
     */
    public List<CiEntityVo> searchCiEntityByIds(List<Long> ciEntityIdList, CiEntityVo ciEntityVo);

    /**
     * @param @param  ciEntityTransactionVo
     * @param @param  action
     * @param @return
     * @return boolean
     * @Author: chenqiwei
     * @Time: 2020年11月17日
     * @Description: 验证配置项是否合法
     */
    public boolean validateCiEntity(CiEntityTransactionVo ciEntityTransactionVo);

    /**
     * @param @param  ciEntityTransactionList
     * @param @return
     * @return Long 事务组id
     * @Author: chenqiwei
     * @Time: 2020年11月17日
     * @Description: 批量保存多个配置项
     */
    public Long saveCiEntity(List<CiEntityTransactionVo> ciEntityTransactionList);

}
