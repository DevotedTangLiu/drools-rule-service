package me.ltang.rules.junit;

import cn.hutool.http.HttpUtil;

import java.io.IOException;

public class Test {

    static String req = "{\"businessType\":\"1002\",\"params\":{\"isHadProduct\":\"\",\"isHaveProduct\":\"\",\"faceRecognResult\":\"\",\"projectCode\":\"2222\",\"companyName\":\"**有限公司\"},\"ruleId\":\"RULEB\"}";

    static String req2 = "{\n" +
            "  \"businessType\": 1001,\n" +
            "  \"ruleId\": \"RH_\",\n" +
            "  \"params\": {\n" +
            "    \"rh_score\": 740,\n" +
            "    \"rh_query_3\": 1,\n" +
            "    \"rh_query_1\": 5\n" +
            "  }\n" +
            "}";

    static String req3 = "{\n" +
            "  \"businessType\": 1001,\n" +
            "  \"ruleId\": \"PreLoanStrategy.RefuseStrategy\",\n" +
            "  \"params\": {\n" +
            "    \"rh_score\": 740,\n" +
            "    \"rh_query_3\": 1,\n" +
            "    \"rh_query_1\": 5\n" +
            "  }\n" +
            "}";

    public static void main(String[] args) throws IOException, InterruptedException {

        while (true) {
            try {
                System.out.println(HttpUtil.post("http://127.0.0.1:9090/rule/exec", req, 5000));
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class RequestTask implements Runnable {

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                try {
                    HttpUtil.post("http://127.0.0.1:9090/rule/exec", req);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("10000次请求完成，耗时: " + (System.currentTimeMillis() - start) + " ms");
        }
    }
}
