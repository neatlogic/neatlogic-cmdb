/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.tagent.register.handler;

import codedriver.framework.cmdb.dto.sync.SyncCiCollectionVo;
import codedriver.framework.cmdb.enums.sync.CollectMode;
import codedriver.framework.tagent.dto.TagentVo;
import codedriver.framework.tagent.register.core.AfterRegisterBase;
import codedriver.module.cmdb.dao.mapper.sync.SyncMapper;
import codedriver.module.cmdb.service.sync.CiSyncManager;
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
