package indi.dakaRobot;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class daka {
  public static String jsessionid;
  public static OkHttpClient client = new OkHttpClient();
  public static ObjectMapper mapper = new ObjectMapper();
  public static Date lastDay;
  public static Date date;
  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  // 修改地址既可，默认填报体温为36°C~36.5°C，可在下面修改
  public static String location = "你的地址";
  public static final String file = "daka.txt";

  static {
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
      jsessionid = bufferedReader.readLine();
      SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
      lastDay = ft.parse(bufferedReader.readLine());
    } catch (IOException | ParseException e) {
      e.printStackTrace();
    }
    mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
  }

  public static void updateInfo() {
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
      jsessionid = bufferedReader.readLine();
      SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
      lastDay = ft.parse(bufferedReader.readLine());
      date = new Date();
      String today = ft.format(date);
      date = ft.parse(today);
    } catch (IOException | ParseException e) {
      e.printStackTrace();
    }
  }

  public static void checkLastDay() throws IOException {
    updateInfo();
    if (date.getTime() - lastDay.getTime() > 24 * 60 * 60 * 1000) {
      throw new IOException("昨天没有打卡");
    }
  }

  public static void daka() throws IOException {
    updateInfo();
    if (date.equals(lastDay)) {
      return;
    }
    ObjectNode nodeTemp = mapper.createObjectNode();
    nodeTemp.put("healthCondition", "正常");
    nodeTemp.put("todayMorningTemperature", "36°C~36.5°C");
    nodeTemp.put("yesterdayEveningTemperature", "36°C~36.5°C");
    nodeTemp.put("yesterdayMiddayTemperature", "36°C~36.5°C");
    nodeTemp.put("location", location);
    String temp = nodeTemp.toString();
    RequestBody body = RequestBody.create(temp, JSON);

    Request request =
        new Request.Builder()
            .url("https://jzsz.uestc.edu.cn/wxvacation/monitorRegisterForReturned")
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36 MicroMessenger/7.0.9.501 NetType/WIFI MiniProgramEnv/Windows WindowsWechat")
            //            .addHeader("X-Tag", "flyio")
            //            .addHeader("Encode", "false")
            //            .addHeader("Referer",
            // "https://servicewechat.com/wx521c0c16b77041a0/30/page-frame.html")
            // 可加可不加
            .addHeader("Accept-Encoding", "identity")
            .addHeader("Connection", "close")
            .addHeader("Content-Type", "application/json")
            .addHeader("Cookie", "JSESSIONID=" + jsessionid)
            .post(body)
            .build();
    Response response = client.newCall(request).execute();
    String result = "";
    if (response.isSuccessful()) {
      result = response.body().string();
    } else {
      throw new IOException("打卡失败,网络错误");
    }
    try {
      JsonNode node = mapper.readTree(result);
      String code = node.get("code").asText();
      switch (code) {
        case "0": // 打卡成功
          try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
            bufferedWriter.write(jsessionid + "\n" + ft.format(date));
            bufferedWriter.flush();
          } catch (IOException e) {
            e.printStackTrace();
          }
          break;
        case "40001": // 登录失败
          throw new IOException("登录失败");
        case "50000": // 重复打卡
          throw new IOException("服务器返回重复打卡但与本地记录不符");
        default: // 未知错误
          throw new IOException("未知错误");
      }
    } catch (NullPointerException e) {
      throw new IOException("打卡失败,解析json错误");
    }
  }

  public static void main(String[] args) {
    updateInfo();
    try {
      System.out.println(jsessionid);
      daka();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}