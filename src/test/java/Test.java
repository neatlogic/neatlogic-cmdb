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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Test {

    public static String encrypt(String secret, String sign) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(sign.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String shaHex = Integer.toHexString(b & 0xFF);
                if (shaHex.length() < 2) {
                    hexString.append(0);
                }
                hexString.append(shaHex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0000000000000000000000000000000000000000000000000000000000000000";
    }

    public static void main(String[] args) {
        //请求url
        String cutUrl="/neatlogic/api/rest/cmdb/cientity/dsl/search";
        String url = "http://localhost:8080/" +cutUrl;

        //请求post data
        JSONObject postDataObj = JSONObject.parseObject("{\"dsl\":\"applicationCode include \"test\" || name include \"test\",\"ciId\":479609502048256}");
        System.out.println(postDataObj.toString());//打印请求body

        //将postdata加密 进而获取authorization
        String postDataBase64 = Base64Utils.encodeToString(JSON.toJSONString(postDataObj,false).getBytes());
        String sign = "fccf704231734072a1bf80d90b2d1de2" + "#" + cutUrl + "#" + postDataBase64;
        System.out.println("sign:"+sign);
        String token = "cddd94aede445c09bacbb85da2a683db";//用户token 从用户信息可以获取
        String authorization = encrypt(token, sign);//认证
        System.out.println("authorization:"+authorization);

        //设置请求头
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.add("Tenant","develop");
        requestHeaders.add("Authorization","Hmac " + authorization);
        requestHeaders.add("AuthType","hmac");
        requestHeaders.add("x-access-key","fccf704231734072a1bf80d90b2d1de2");
        HttpEntity<JSONObject> requestEntity = new HttpEntity<>(postDataObj, requestHeaders);
        RestTemplate restTemplate = new RestTemplate();

        //调接口
        JSONObject resultObj = restTemplate.postForObject(url, requestEntity, JSONObject.class);
        System.out.println(resultObj);

    }
}
