/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.rel;

import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import codedriver.framework.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class RelServiceImpl implements RelService {
    //private final static Logger logger = LoggerFactory.getLogger(RelServiceImpl.class);

    @Resource
    private RelMapper relMapper;

    @Resource
    private RelEntityService relEntityService;

    @Resource
    private RelEntityMapper relEntityMapper;


    @Override
    @Transactional
    public void deleteRel(RelVo relVo) {
        //删除所有relEntity属性值，需要生成事务
        RelEntityVo relEntityVo = new RelEntityVo();
        relEntityVo.setRelId(relVo.getId());
        relEntityVo.setPageSize(100);
        List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByRelId(relEntityVo);
        TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
        while (CollectionUtils.isNotEmpty(relEntityList)) {
            relEntityService.deleteRelEntity(transactionGroupVo, relEntityList);
            relEntityVo.setCacheFlushKey(SnowflakeUtil.uniqueLong());//由于在同一个事务里，所以需要增加一个新参数扰乱mybatis的一级缓存
            relEntityList = relEntityMapper.getRelEntityByRelId(relEntityVo);
        }
        //删除模型关系
        relMapper.deleteRelById(relVo.getId());
    }
}
