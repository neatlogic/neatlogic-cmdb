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

package neatlogic.module.cmdb.tagent.register.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.cmdb.enums.sync.CollectMode;
import neatlogic.framework.tagent.dto.TagentVo;
import neatlogic.framework.tagent.register.core.AfterRegisterBase;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import neatlogic.module.cmdb.service.sync.CiSyncManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 添加tagent数据进配置项
 */
@Service
public class AddCiEntityHandler extends AfterRegisterBase {
    private static final Logger logger = LoggerFactory.getLogger(AddCiEntityHandler.class);
    @Resource
    private SyncMapper syncMapper;
    @Resource
    private MongoTemplate mongoTemplate;

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
        String tagentStr;
        if (logger.isDebugEnabled()) {
            tagentStr = JSON.toJSONString(tagentVo);
            logger.debug("AddCiEntityHandler init! {}", tagentStr);
        }
        if (StringUtils.isNotBlank(tagentVo.getOsType()) && StringUtils.isNotBlank(tagentVo.getIp())) {
            //ip在_virtualized_tagent_unique_ip不存在才同步入cmdb
            if (!isOsIpExistsInCollection(tagentVo.getIp())) {
                List<SyncCiCollectionVo> tmpList = syncMapper.getSyncCiCollectionByCollectionName(tagentVo.getOsType());
                List<SyncCiCollectionVo> ciCollectionList = tmpList.stream().filter(d -> d.getCollectMode().equals(CollectMode.INITIATIVE.getValue())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(ciCollectionList)) {
                    //组装成mongodb约定的数据格式
                    JSONObject dataObj = new JSONObject();
                    dataObj.put("_OBJ_CATEGORY", "OS");
                    dataObj.put("_OBJ_TYPE", capitalizeFirst(tagentVo.getOsType()));
                    dataObj.put("OS_TYPE", capitalizeFirst(tagentVo.getOsType()));
                    dataObj.put("MGMT_IP", tagentVo.getIp());
                    dataObj.put("CPU_ARCH", tagentVo.getOsbit());
                    dataObj.put("HOSTNAME", tagentVo.getName());
                    dataObj.put("VERSION", tagentVo.getOsVersion());
                    if (logger.isDebugEnabled()) {
                        logger.debug("AddCiEntityHandler sync mongodb to cmdb start! {},{}", JSON.toJSONString(dataObj), JSON.toJSONString(ciCollectionList));
                    }
                    CiSyncManager.doSync(dataObj, ciCollectionList);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("AddCiEntityHandler os collectMode is not initiative, no need to sync! ");
                    }
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("AddCiEntityHandler _virtualized_tagent_unique_ip exist {}, no need to sync! ", tagentVo.getIp());
                }
            }
        }

    }

    /**
     * 首字母大写
     *
     * @param str 字符串
     */
    public static String capitalizeFirst(String str) {
        if (StringUtils.isBlank(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     *
     * 构建查询条件：
     * 1. _virtualized_tagent_unique_ip 字段存在
     * 2. osIp 是否存在
     *
     * @param osIp 操作系统ip
     *
     */
    public boolean isOsIpExistsInCollection(String osIp) {
        String collectionName = "_virtualized_tagent_unique_ip";
        if (!mongoTemplate.collectionExists(collectionName)) {
            return false;
        }
        Query query = Query.query(Criteria.where("os_ip").is(osIp));
        return mongoTemplate.exists(query, collectionName);
    }
}
