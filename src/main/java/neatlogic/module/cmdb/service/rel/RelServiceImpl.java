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

package neatlogic.module.cmdb.service.rel;

import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionGroupVo;
import neatlogic.framework.util.SnowflakeUtil;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.RelEntityMapper;
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
    //TODO 此方法需要参考deleteAttr优化
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
