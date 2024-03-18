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

import neatlogic.framework.asynchronization.threadlocal.InputFromContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.crossover.IRelEntityCrossoverService;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionGroupVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.enums.TransactionStatus;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class RelEntityServiceImpl implements RelEntityService, IRelEntityCrossoverService {
    @Resource
    private TransactionMapper transactionMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private RelMapper relMapper;

    @Resource
    private RelEntityMapper relEntityMapper;

    @Override
    public List<RelEntityVo> getRelEntityByCiEntityId(Long ciEntityId) {
        return relEntityMapper.getRelEntityByCiEntityId(ciEntityId);
    }

    @Override
    public void deleteRelEntity(TransactionGroupVo transactionGroupVo, List<RelEntityVo> relEntityList) {
        for (RelEntityVo item : relEntityList) {
            RelVo relVo = relMapper.getRelById(item.getRelId());
            //检查当前配置项在当前事务组下是否已经存在事务，如果已经存在则无需创建新的事务
            List<CiEntityTransactionVo> fromCiEntityTransactionList = transactionMapper.getCiEntityTransactionByTransactionGroupIdAndCiEntityId(transactionGroupVo.getId(), item.getFromCiEntityId());
            if (CollectionUtils.isEmpty(fromCiEntityTransactionList)) {
                //写入事务
                TransactionVo fromTransactionVo = new TransactionVo();
                fromTransactionVo.setCiId(item.getFromCiId());
                fromTransactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                fromTransactionVo.setInputFrom(InputFromContext.get().getInputFrom());
                fromTransactionVo.setCreateUser(UserContext.get().getUserUuid(true));
                fromTransactionVo.setCommitUser(UserContext.get().getUserUuid(true));
                transactionMapper.insertTransaction(fromTransactionVo);
                //写入事务分组
                transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), fromTransactionVo.getId());
                //写入来源端配置项事务
                CiEntityTransactionVo fromCiEntityTransactionVo = new CiEntityTransactionVo();
                fromCiEntityTransactionVo.setCiEntityId(item.getFromCiEntityId());
                fromCiEntityTransactionVo.setCiId(item.getFromCiId());
                fromCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                fromCiEntityTransactionVo.setTransactionId(fromTransactionVo.getId());
                fromCiEntityTransactionVo.setOldCiEntityVo(ciEntityService.getCiEntityById(item.getFromCiId(), item.getFromCiEntityId()));
                // 创建快照快照
                ciEntityService.createSnapshot(fromCiEntityTransactionVo);
                //补充关系删除事务数据
                fromCiEntityTransactionVo.addRelEntityData(relVo, RelDirectionType.FROM.getValue(), item.getToCiId(), item.getToCiEntityId(), item.getToCiEntityName(), TransactionActionType.DELETE.getValue());

                //写入配置项事务
                transactionMapper.insertCiEntityTransaction(fromCiEntityTransactionVo);
            } else {
                //补充关系删除事务数据到同一个配置项的事务数据中
                for (CiEntityTransactionVo fromCiEntityTransactionVo : fromCiEntityTransactionList) {
                    fromCiEntityTransactionVo.addRelEntityData(relVo, RelDirectionType.FROM.getValue(), item.getToCiId(), item.getToCiEntityId(), item.getToCiEntityName(), TransactionActionType.DELETE.getValue());
                    transactionMapper.updateCiEntityTransactionContent(fromCiEntityTransactionVo);
                }
            }

            //针对目标配置项重新做一遍以上逻辑
            List<CiEntityTransactionVo> toCiEntityTransactionList = transactionMapper.getCiEntityTransactionByTransactionGroupIdAndCiEntityId(transactionGroupVo.getId(), item.getToCiEntityId());
            if (CollectionUtils.isEmpty(toCiEntityTransactionList)) {
                //写入目标端配置项事务
                //写入事务
                TransactionVo toTransactionVo = new TransactionVo();
                toTransactionVo.setCiId(item.getToCiId());
                toTransactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                toTransactionVo.setInputFrom(InputFromContext.get().getInputFrom());
                toTransactionVo.setCreateUser(UserContext.get().getUserUuid(true));
                toTransactionVo.setCommitUser(UserContext.get().getUserUuid(true));
                transactionMapper.insertTransaction(toTransactionVo);
                //写入事务分组
                transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), toTransactionVo.getId());
                CiEntityTransactionVo toCiEntityTransactionVo = new CiEntityTransactionVo();
                toCiEntityTransactionVo.setCiEntityId(item.getToCiEntityId());
                toCiEntityTransactionVo.setCiId(item.getToCiId());
                toCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                toCiEntityTransactionVo.setTransactionId(toTransactionVo.getId());
                toCiEntityTransactionVo.setOldCiEntityVo(ciEntityService.getCiEntityById(item.getToCiId(), item.getToCiEntityId()));

                // 创建快照
                ciEntityService.createSnapshot(toCiEntityTransactionVo);
                //补充事务删除数据
                toCiEntityTransactionVo.addRelEntityData(relVo, RelDirectionType.TO.getValue(), item.getFromCiId(), item.getFromCiEntityId(), item.getFromCiEntityName(), TransactionActionType.DELETE.getValue());

                transactionMapper.insertCiEntityTransaction(toCiEntityTransactionVo);
            } else {
                for (CiEntityTransactionVo toCiEntityTransactionVo : toCiEntityTransactionList) {
                    toCiEntityTransactionVo.addRelEntityData(relVo, RelDirectionType.TO.getValue(), item.getFromCiId(), item.getFromCiEntityId(), item.getFromCiEntityName(), TransactionActionType.DELETE.getValue());
                    transactionMapper.updateCiEntityTransactionContent(toCiEntityTransactionVo);
                }
            }
            //正式删除关系数据
            relEntityMapper.deleteRelEntityByRelIdFromCiEntityIdToCiEntityId(item.getRelId(),
                    item.getFromCiEntityId(), item.getToCiEntityId());
        }
    }
}
