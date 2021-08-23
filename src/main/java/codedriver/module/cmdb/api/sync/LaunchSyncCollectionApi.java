/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.sync;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.sync.SyncCiCollectionVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.sync.SyncMapper;
import codedriver.module.cmdb.service.sync.CiSyncManager;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class LaunchSyncCollectionApi extends PrivateApiComponentBase {

    @Autowired
    private SyncMapper syncMapper;

    @Resource
    private CiMapper ciMapper;


    @Override
    public String getToken() {
        return "/cmdb/sync/lanuch";
    }

    @Override
    public String getName() {
        return "发起配置项自动发现";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "collection", type = ApiParamType.STRING, isRequired = true, desc = "配置id")})
    @Description(desc = "发起配置项自动发现接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String collectionName = jsonObj.getString("collection");
        List<SyncCiCollectionVo> syncCiCollectionList = syncMapper.getSyncCiCollectionByCollectionName(collectionName);
        CiSyncManager.doSync(syncCiCollectionList);
//        if (CollectionUtils.isNotEmpty(mappingList) && syncConfigVo.getCiId() != null) {
//            CiVo ciVo = ciMapper.getCiById(syncConfigVo.getCiId());
//            if (ciVo == null) {
//                throw new CiNotFoundException(syncConfigVo.getCiId());
//            }
//            if (CollectionUtils.isEmpty(ciVo.getUniqueAttrIdList())) {
//                throw new CiUniqueAttrNotFoundException(ciVo.getLabel());
//            }
//
//            syncConfigVo.setCiVo(ciVo);
//            CiSyncManager.doSync(syncConfigVo);
//        }
        return null;
    }

}
