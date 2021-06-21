package com.sankuai.mdp.lotterylidongservice.util;

import com.google.common.collect.Maps;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName ApiTest.java
 * @Description TODO
 * @createTime 2021年06月21日 14:37:00
 */
public class ApiTest {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static void main(String[] args) {
        try {
            for (int i = 0; i < 200; i++) {
                Map<String, Object> param = Maps.newHashMap();
                param.put("ip","10.12.11"+i);
                param.put("uuid","9e560a08632548a7bda2f69152852041");
                RequestBody loginBody =
                        RequestBody.create(JSON, com.alibaba.fastjson.JSON.toJSONString(param));
                Response response = httpPost("http://127.0.0.1:8081/activity/draw", loginBody);
                // System.out.println(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static Response httpPost(String url, RequestBody requestBody) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request=new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client
                .newCall(request)
                .execute();
        if (response.isSuccessful()) {
            return response;
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

}
