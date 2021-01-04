package codedriver.module.cmdb.attrvaluehandler.handler;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;
import codedriver.framework.cmdb.prop.core.PropertyHandlerFactory;
import codedriver.module.cmdb.dao.mapper.cientity.AttrEntityContentMapper;

/**
 * @Author:chenqiwei
 * @Time:Sep 1, 2020
 * @ClassName: HashValueHandler
 * @Description: 把属性内容转换成hash
 */
@Service
public class HashValueHandler implements IAttrValueHandler {

    @Autowired
    private AttrEntityContentMapper attrEntityContentMapper;

    @Override
    public String getProtocol() {
        return "hash";
    }

    @Override
    public String getTransferedValue(String propHandler, String value) {
        if (StringUtils.isNotBlank(value)) {
            String hash = DigestUtils.md5DigestAsHex(value.getBytes());
            String valueHash = "";
            if (attrEntityContentMapper.checkAttrEntityHashIsExists(hash) <= 0) {
                if (StringUtils.isNotBlank(propHandler)) {
                    IPropertyHandler handler = PropertyHandlerFactory.getHandler(propHandler);
                    valueHash = handler.getValueHash(value);
                }
                if (StringUtils.isBlank(valueHash)) {
                    // valuehash不区分大小写
                    valueHash = DigestUtils.md5DigestAsHex(value.toLowerCase().getBytes());
                }
                attrEntityContentMapper.insertAttrEntityContent(hash, valueHash, value);
            }
            return hash;
        }
        return "";
    }

    @Override
    public String getActualValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            return attrEntityContentMapper.getAttrEntityContentByHash(value);
        }
        return "";
    }

}
