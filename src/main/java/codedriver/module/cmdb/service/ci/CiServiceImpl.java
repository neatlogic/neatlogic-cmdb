package codedriver.module.cmdb.service.ci;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.ci.RelVo;
import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.elasticsearch.aop.ElasticSearchAspect;
import codedriver.framework.elasticsearch.core.ElasticSearchHandlerFactory;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.dto.ci.CiVo;

@Service
public class CiServiceImpl implements CiService {
    private final static Logger logger = LoggerFactory.getLogger(CiServiceImpl.class);

    @Autowired
    private CiMapper ciMapper;

    @Autowired
    private CiEntityMapper ciEntityMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private RelEntityMapper relEntityMapper;

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private RelMapper relMapper;

    @Override
    public int saveCi(CiVo ciVo) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int deleteCi(Long ciId) {
        // 获取所有配置项id
        List<Long> ciEntityIdList = ciEntityMapper.getCiEntityIdByCiId(ciId);
        // 获取关系有变化的配置项id
        final List<Long> changeCiEntityIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
            changeCiEntityIdList.addAll(relEntityMapper.getFromToCiEntityIdByCiEntityIdList(ciEntityIdList));
            // 去掉需要删除的配置项
            changeCiEntityIdList.removeAll(ciEntityIdList);
        }
        // 删除配置项相关信息
        ciEntityMapper.deleteCiEntityByCiId(ciId);
        // 删除事务相关信息
        transactionMapper.deleteTransactionByCiId(ciId);
        // 删除模型相关信息
        ciMapper.deleteCiById(ciId);

        //处理es索引
        if (CollectionUtils.isNotEmpty(ciEntityIdList) || CollectionUtils.isNotEmpty(changeCiEntityIdList)) {
            CachedThreadPool.execute(new CodeDriverThread("ELASTICSEARCH-DOCUMENT-DELETE-CI:" + ciId) {
                @Override
                protected void execute() { // 删除索引
                    if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
                        for (Long ciEntityId : ciEntityIdList) {
                            try {
                                ElasticSearchHandlerFactory.getHandler("cientity").delete(ciEntityId.toString());
                            } catch (Exception ex) {
                                logger.error(ex.getMessage(), ex);

                            }
                        }
                    } // 更新受影响配置项索引
                    if (CollectionUtils.isNotEmpty(changeCiEntityIdList)) {
                        for (Long ciEntityId : changeCiEntityIdList) {
                            try {
                                this.setThreadName("ELASTICSEARCH-DOCUMENT-SAVE-" + ciEntityId);
                                ElasticSearchHandlerFactory.getHandler("cientity").save(ciEntityId);
                            } catch (Exception ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        }
                    }
                }
            });
        }
        return 1;
    }

    @Override
    public CiVo getCiById(Long id) {
        CiVo ciVo = ciMapper.getCiById(id);
        List<AttrVo> attrList = attrMapper.getAttrByCiId(id);
        List<RelVo> relList = relMapper.getRelByCiId(id);
        ciVo.setRelList(relList);
        ciVo.setAttrList(attrList);
//        ciVo.setOperateList(operateMapper.getOperateListByCiId(ciVo.getId()));
        return ciVo;
    }
}
