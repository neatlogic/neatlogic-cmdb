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

package neatlogic.module.cmdb.importexport.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.framework.cmdb.enums.CmdbImportExportHandlerType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

@Component
public class ProtocolImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public ImportExportHandlerType getType() {
        return CmdbImportExportHandlerType.PROTOCOL;
    }

    @Override
    public boolean checkImportAuth(ImportExportVo importExportVo) {
        return true;
    }

    @Override
    public boolean checkExportAuth(Object primaryKey) {
        return true;
    }

    @Override
    public boolean checkIsExists(ImportExportBaseInfoVo importExportBaseInfoVo) {
        return resourceAccountMapper.getAccountProtocolVoByProtocolName(importExportBaseInfoVo.getName()) != null;
    }

    @Override
    public Object getPrimaryByName(ImportExportVo importExportVo) {
        AccountProtocolVo oldProtocolVo = resourceAccountMapper.getAccountProtocolVoByProtocolName(importExportVo.getName());
        if (oldProtocolVo == null) {
            throw new ResourceCenterAccountProtocolNotFoundException(importExportVo.getName());
        }
        return oldProtocolVo.getId();
    }

    @Override
    public Long importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        // 导入前判断协议是否已经存在的标准是名称是否相同，如果存在则更新，如果不存在则新增。
        // 如果需要的数据，名称不存在但id已存在，则更新导入数据的id。
        JSONObject data = importExportVo.getData();
        AccountProtocolVo protocolVo = data.toJavaObject(AccountProtocolVo.class);
        AccountProtocolVo oldProtocolVo = resourceAccountMapper.getAccountProtocolVoByProtocolName(protocolVo.getName());
        if (oldProtocolVo != null) {
            protocolVo.setId(oldProtocolVo.getId());
            if (Objects.equals(oldProtocolVo.getPort(), protocolVo.getPort())) {
                return protocolVo.getId();
            }
            oldProtocolVo.setPort(protocolVo.getPort());
        } else {
            if (resourceAccountMapper.getAccountProtocolVoByProtocolId(protocolVo.getId()) != null) {
                // 更新id
                protocolVo.setId(null);
            }
        }
        protocolVo.setFcu(UserContext.get().getUserUuid());
        protocolVo.setLcu(UserContext.get().getUserUuid());
        resourceAccountMapper.insertAccountProtocol(protocolVo);
        return protocolVo.getId();
    }

    @Override
    public ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        Long id = (Long) primaryKey;
        AccountProtocolVo accountProtocolVo = resourceAccountMapper.getAccountProtocolVoByProtocolId(id);
        if (accountProtocolVo == null) {
            throw new ResourceCenterAccountProtocolNotFoundException(id);
        }
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, accountProtocolVo.getName());
        importExportVo.setDataWithObject(accountProtocolVo);
        return importExportVo;
    }
}
