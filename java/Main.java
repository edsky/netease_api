import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.*;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

// 提交数据结构
//@AutoValue
//abstract class music{
//    public abstract String ids();
//    public abstract String br();   // 可能是码率
//    public abstract String csrf_token();
//}

public class Main {
    // for aes , 第一个 key
    private final String g = "0CoJUm6Qyw8W8jud";
    // for aes , 密码向量
    private final String iv = "0102030405060708";
    // for rsa
    private String e = "010001";    // e-> ‭65537‬
    // 公钥
    private String pubKey = "00e0b509f6259df8642dbc35662901477df22" +
            "677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e4" +
            "17629ec4ee341f56135fccf695280104e0312ecbda92557c938701" +
            "14af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe" +
            "4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";
    // for 随机数 [a-zA-Z0-9]
    private String randomStr = "abcdefghijklmnopqrstuvwxyzABCDEFGHI" +
            "JKLMNOPQRSTUVWXYZ0123456789";

    // API地址
    private final String URL = "http://music.163.com/weapi/song/enhance" +
            "/player/url?csrf_token=";

    private OkHttpClient client = new OkHttpClient();

    private String post(String params,String encSecKey) throws IOException {
        FormBody.Builder builder = new FormBody.Builder();
        // for encode
        params.replace("+","%2B");
        params.replace("-","%2F");
        params.replace("=","%3D");
        builder.add("params", params);
        builder.add("encSecKey", encSecKey);

        RequestBody body = builder.build();
        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .build();

//        Buffer buffer = new Buffer();
//        body.writeTo(buffer);
//        System.out.println("生成的post请求为:" + buffer.readUtf8());

         Response response = client.newCall(request).execute();
         return response.body().string();
//        return "";
    }

    // AES
    private String aes(String key, String initVector, String value) {

        try {
            IvParameterSpec iv = new IvParameterSpec(
                    initVector.getBytes("UTF-8"));
            SecretKeySpec sKeySpec = new SecretKeySpec(
                    key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKC" +
                    "S5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, sKeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());

            return Base64.encodeBase64String(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    // rsa
    private String rsa(String data){
        BigInteger _e = new BigInteger(e, 16);
        BigInteger _n = new BigInteger(pubKey, 16);

        // 反向字符串
        byte[] byteArray = new byte[16];
        for(int i = 0; i < data.length(); i++){
            byteArray[15-i] = (byte)data.charAt(i);
        }

        BigInteger _data = new BigInteger(byteArray);

//        System.out.println("e:"+ _e.toString());
//        System.out.println("n:"+ _n.toString());
//        System.out.println("data:"+ _data.toString());

        BigInteger rc = _data.modPow(_e, _n);

        return rc.toString(16);
    }

    // 产生字符串
    private String randomString(int length){
        Random random = new Random();
        StringBuffer buf = new StringBuffer();
        for(int i = 0; i < length;i++){
            int num = random.nextInt(randomStr.length());
            buf.append(randomStr.charAt(num));
        }

        return buf.toString();
//        return "E0p6G30EilQZahMP";
    }

    public void test(String id){
        // build params
        ObjectMapper objectMapper = new ObjectMapper();
        HashMap<String, Object> map = new HashMap<>();
        // csrf_token长期空,br可能是码率
        map.put("csrf_token","");
        map.put("br", 128000);
        // 歌曲id
        List<Integer> array = new ArrayList<>();
        array.add(Integer.parseInt(id, 10));
        map.put("ids", array);

        // 生成post请求text
        String query = null;
        try {
            query = objectMapper.writeValueAsString(map);
            System.out.println("查询语句:" + query);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }
//        query = "{\"ids\":\"[423227301]\",\"br\":128000,\"csrf_token\":\"\"}";
        // for aes , 第二个 key, 随机产生
        String key = randomString(16);
        System.out.println("随机产生的key:" + key);

        // 参数加密
        query = aes(g, iv, query);
        System.out.println("aes第一次加密:" + query);

        // 第二次用随机key加密
        query = aes(key, iv, query);
        System.out.println("aes第二次加密:" + query);

        // rsa加密key
        String enc = rsa(key);
        System.out.println("rsa加密后的key为:" + enc);

        // 提交请求
        String response = null;
        try {
            response = post(query, enc);
            System.out.println("POST返回结果:"+ response);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        // 简单解析一下
        ObjectMapper mapper = new ObjectMapper();
        try {
            HashMap<String, Object> hashMap =
                    mapper.readValue(response, HashMap.class);
            List<Object> data = (List<Object>) hashMap.get("data");

            for(Object obj : data){
                Map<String, Object> song = (Map<String, Object>) obj;

                Integer iid = (Integer) song.get("id");
                String url = (String) song.get("url");

                System.out.println("**简单解析结果**");
                System.out.println("id:"+ iid);
                System.out.println("url:"+url);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    public static void main(String[] args) {
        // http://music.163.com/song?id=423227301
        new Main().test("423227301");
//        new test().test();
    }
}
