/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
