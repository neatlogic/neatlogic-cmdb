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

package neatlogic.module.cmdb.service.customview;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.crossover.ICustomViewCrossoverService;
import neatlogic.framework.cmdb.dto.customview.*;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.cmdb.exception.customview.CreateCustomViewFailedException;
import neatlogic.framework.cmdb.exception.customview.CustomViewAttrNameIsExistsException;
import neatlogic.framework.cmdb.exception.customview.DeleteCustomViewFailedException;
import neatlogic.framework.transaction.core.EscapeTransactionJob;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewMapper;
import neatlogic.module.cmdb.dao.mapper.tag.CmdbTagMapper;
import neatlogic.module.cmdb.utils.CustomViewBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CustomViewServiceImpl implements CustomViewService, ICustomViewCrossoverService {
    @Resource
    private CustomViewMapper customViewMapper;

    @Resource
    private CmdbTagMapper cmdbTagMapper;


    @Override
    public List<CustomViewAttrVo> getCustomViewAttrByCustomViewId(CustomViewAttrVo customViewAttrVo) {
        return customViewMapper.getCustomViewAttrByCustomViewId(customViewAttrVo);
    }

    @Override
    public List<CustomViewConstAttrVo> getCustomViewConstAttrByCustomViewId(CustomViewConstAttrVo customViewConstAttrVo) {
        return customViewMapper.getCustomViewConstAttrByCustomViewId(customViewConstAttrVo);
    }


    @Override
    public void updateCustomViewActive(CustomViewVo customViewVo) {
        customViewMapper.updateCustomViewActive(customViewVo);
    }

    @Override
    public CustomViewVo getCustomViewById(Long id) {
        return customViewMapper.getCustomViewById(id);
    }

    @Override
    public CustomViewVo getCustomViewDetailById(Long id) {
        CustomViewVo customViewVo = customViewMapper.getCustomViewById(id);
        customViewVo.setLinkList(customViewMapper.getCustomViewLinkByCustomViewId(id));
        customViewVo.setCiList(customViewMapper.getCustomViewCiByCustomViewId(id));
        return customViewVo;
    }


    @Override
    public List<CustomViewVo> searchCustomView(CustomViewVo customViewVo) {
        List<CustomViewVo> customViewList = customViewMapper.searchCustomView(customViewVo);
        if (CollectionUtils.isNotEmpty(customViewList)) {
            int rowNum = customViewMapper.searchCustomViewCount(customViewVo);
            customViewVo.setRowNum(rowNum);
        }
        return customViewList;
    }

    @Override
    @Transactional
    public void insertCustomView(CustomViewVo customViewVo) {
        customViewMapper.insertCustomView(customViewVo);
        saveCustomView(customViewVo);
    }

    @Override
    @Transactional
    public void updateCustomView(CustomViewVo customViewVo) {
        customViewMapper.updateCustomView(customViewVo);
        customViewMapper.deleteCustomViewCiByCustomViewId(customViewVo.getId());
        customViewMapper.deleteCustomViewAttrByCustomViewId(customViewVo.getId());
        customViewMapper.deleteCustomViewConstAttrByCustomViewId(customViewVo.getId());
        customViewMapper.deleteCustomViewRelByCustomViewId(customViewVo.getId());
        customViewMapper.deleteCustomViewLinkByCustomViewId(customViewVo.getId());
        customViewMapper.deleteCustomViewTagByCustomViewId(customViewVo.getId());
        customViewMapper.deleteCustomViewAuthByCustomViewId(customViewVo.getId());
        saveCustomView(customViewVo);
    }

    private void saveCustomView(CustomViewVo customViewVo) {
        if (CollectionUtils.isNotEmpty(customViewVo.getCiList())) {
            for (CustomViewCiVo customViewCiVo : customViewVo.getCiList()) {
                customViewCiVo.setCustomViewId(customViewVo.getId());
                customViewMapper.insertCustomViewCi(customViewCiVo);
                if (CollectionUtils.isNotEmpty(customViewCiVo.getAttrList())) {
                    for (CustomViewAttrVo customViewAttrVo : customViewCiVo.getAttrList()) {
                        if (StringUtils.isNotBlank(customViewAttrVo.getName()) && customViewMapper.checkCustomViewAttrIsExists(customViewAttrVo) > 0) {
                            throw new CustomViewAttrNameIsExistsException(customViewAttrVo);
                        }
                        customViewAttrVo.setCustomViewId(customViewVo.getId());
                        customViewAttrVo.setCustomViewCiUuid(customViewCiVo.getUuid());
                        customViewMapper.insertCustomViewAttr(customViewAttrVo);
                    }
                }
                if (CollectionUtils.isNotEmpty(customViewCiVo.getRelList())) {
                    for (CustomViewRelVo customViewRelVo : customViewCiVo.getRelList()) {
                        customViewRelVo.setCustomViewId(customViewVo.getId());
                        customViewRelVo.setCustomViewCiUuid(customViewCiVo.getUuid());
                        customViewMapper.insertCustomViewRel(customViewRelVo);
                    }
                }
                if (CollectionUtils.isNotEmpty(customViewCiVo.getConstAttrList())) {
                    for (CustomViewConstAttrVo customViewConstAttrVo : customViewCiVo.getConstAttrList()) {
                        customViewConstAttrVo.setCustomViewId(customViewVo.getId());
                        customViewConstAttrVo.setCustomViewCiUuid(customViewCiVo.getUuid());
                        customViewMapper.insertCustomViewConstAttr(customViewConstAttrVo);
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(customViewVo.getLinkList())) {
            for (CustomViewLinkVo customViewLinkVo : customViewVo.getLinkList()) {
                customViewLinkVo.setCustomViewId(customViewVo.getId());
                customViewMapper.insertCustomViewLink(customViewLinkVo);
            }
        }

        if (CollectionUtils.isNotEmpty(customViewVo.getTagList())) {
            for (TagVo tagVo : customViewVo.getTagList()) {
                cmdbTagMapper.insertCmdbTag(tagVo);
                customViewMapper.insertCustomViewTag(customViewVo.getId(), tagVo.getId());
            }
        }

        if (CollectionUtils.isNotEmpty(customViewVo.getCustomViewAuthList())) {
            for (CustomViewAuthVo authVo : customViewVo.getCustomViewAuthList()) {
                authVo.setCustomViewId(customViewVo.getId());
                customViewMapper.insertCustomViewAuth(authVo);
            }
        }

        EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
            CustomViewBuilder builder = new CustomViewBuilder(customViewVo);
            builder.buildView();
        }).execute();
        if (!s.isSucceed()) {
            throw new CreateCustomViewFailedException(s.getError());
        }
    }

    public void buildCustomView(String sql) {
        customViewMapper.buildCustomView(sql);
    }

    @Override
    @Transactional
    public void deleteCustomView(Long id) {
        customViewMapper.deleteCustomViewCiByCustomViewId(id);
        customViewMapper.deleteCustomViewAttrByCustomViewId(id);
        customViewMapper.deleteCustomViewRelByCustomViewId(id);
        customViewMapper.deleteCustomViewLinkByCustomViewId(id);
        customViewMapper.deleteCustomViewTagByCustomViewId(id);
        customViewMapper.deleteCiCustomViewByCustomViewId(id);
        customViewMapper.deleteCustomViewById(id);
        EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
            customViewMapper.dropCustomView(TenantContext.get().getDataDbName() + ".`customview_" + id + "`");
        }).execute();
        if (!s.isSucceed()) {
            throw new DeleteCustomViewFailedException(s.getError());
        }
    }


}
