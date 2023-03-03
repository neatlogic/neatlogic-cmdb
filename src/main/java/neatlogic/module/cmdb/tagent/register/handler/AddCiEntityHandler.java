/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.tagent.register.handler;

import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.cmdb.enums.sync.CollectMode;
import neatlogic.framework.tagent.dto.TagentVo;
import neatlogic.framework.tagent.register.core.AfterRegisterBase;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import neatlogic.module.cmdb.service.sync.CiSyncManager;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 添加tagent数据进配置项
 */
@Service
public class AddCiEntityHandler extends AfterRegisterBase {

    @Resource
    private SyncMapper syncMapper;


    /**
     * 需要满足以下条件才能完成同步：
     * 1、tagentVo中的osType属性需要能在mongodb的dictionary中的找到对应的配置。
     * 2、dictionary中的对应配置配置了模型映射，并且配置是"主动同步模式"。
     * 3、如果发现了多套模型映射配置，则需要看映射属性是否都包含了目标模型的唯一判定属性。
     * 4、如果配置项已经存在，则不再添加或修改任何属性。
     *
     * @param tagentVo tagent对象
     */
    @Override
    public void myExecute(TagentVo tagentVo) {
        if (StringUtils.isNotBlank(tagentVo.getOsType())) {
            List<SyncCiCollectionVo> tmpList = syncMapper.getSyncCiCollectionByCollectionName(tagentVo.getOsType());
            List<SyncCiCollectionVo> ciCollectionList = tmpList.stream().filter(d -> d.getCollectMode().equals(CollectMode.INITIATIVE.getValue())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(ciCollectionList)) {
                //组装成mongodb约定的数据格式
                JSONObject dataObj = new JSONObject();
                dataObj.put("_OBJ_CATEGORY", "OS");
                dataObj.put("_OBJ_TYPE", tagentVo.getOsType());
                dataObj.put("OS_TYPE", tagentVo.getOsType());
                dataObj.put("MGMT_IP", tagentVo.getIp());
                dataObj.put("CPU_ARCH", tagentVo.getOsbit());
                dataObj.put("HOSTNAME", tagentVo.getName());
                dataObj.put("VERSION", tagentVo.getOsVersion());
                CiSyncManager.doSync(dataObj, ciCollectionList);
            }
        }

    }
}
