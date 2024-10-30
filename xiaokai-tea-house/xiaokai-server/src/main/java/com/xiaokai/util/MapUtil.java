package com.xiaokai.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaokai.exception.MapServiceException;
import com.xiaokai.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;

import static com.xiaokai.constant.MapConstant.GET_ADDRESS_LOCATION_ERROR;
import static com.xiaokai.constant.MapConstant.GET_ROUTE_ERROR;
import static com.xiaokai.constant.MessageConstant.ORDER_DISTANCE_TOO_LONG;

@Service
@Slf4j
public class MapUtil {
    @Value("${xiaokai.shop.address}")
    private String shopAddress;

    @Value("${xiaokai.amap.key}")
    private String key;

    private final String geocodeUrl = "https://restapi.amap.com/v3/geocode/geo?parameters";
    // private final String driverUrl = "https://restapi.amap.com/v4/direction/bicycling?parameters";
    private final String driverUrl = "https://restapi.amap.com/v5/direction/electrobike?parameters";

    /**
     * 检查客户的收货地址是否超出配送范围
     *
     * @param address
     */
    public void checkOutOfRange(String address) {
        // 调用高德API计算距离
        HashMap<String, String> map = new HashMap<>();
        map.put("address", shopAddress);
        map.put("output", "JSON");
        map.put("key", key);

        // 店铺经纬度坐标
        String shopLocation = "";
        try {
            shopLocation = getLocation(map);
        } catch (MapServiceException e) {
            log.error(e.getMessage());
            throw new MapServiceException(GET_ADDRESS_LOCATION_ERROR);
        }

        // 获取用户收货地址的经纬度坐标
        String addressLocation = "";
        map.put("address", address);
        try {
            addressLocation = getLocation(map);
            log.info("用户地址是：{}, 经纬度分别为:({})", address, addressLocation);
        } catch (MapServiceException e) {
            throw new MapServiceException(GET_ADDRESS_LOCATION_ERROR);
        }

        // 重新构建对应请求参数的map
        map = new HashMap<>();
        map.put("key", key);
        map.put("origin", shopLocation);
        map.put("destination", addressLocation);

        // 路线规划
        String json = HttpClientUtil.doGet(driverUrl, map);

        JSONObject jsonObject = JSON.parseObject(json);
        if (jsonObject.getString("status").equals("0")) {
            throw new MapServiceException(GET_ROUTE_ERROR);
        }

        // 数据解析
        // 获取 paths 数组

        // JSONArray paths = (JSONArray) jsonObject.getJSONObject("route").get("paths");
        // JSONObject path = (JSONObject) paths.get(0);
        // Integer distance = (Integer) path.get("distance");
        try {
            // 获取 route 对象
            JSONObject route = jsonObject.getJSONObject("route");
            if (route == null) {
                throw new MapServiceException(GET_ROUTE_ERROR);
            }

            // 获取 paths 数组
            JSONArray paths = route.getJSONArray("paths");
            if (paths == null || paths.isEmpty()) {
                throw new MapServiceException(GET_ROUTE_ERROR);
            }

            // 获取 paths 数组中的第一个元素
            JSONObject path = paths.getJSONObject(0);
            if (path == null || !path.containsKey("distance")) {
                throw new MapServiceException(GET_ROUTE_ERROR);
            }

            // 获取 distance 字段
            Integer distance = path.getInteger("distance");
            if (distance > 5000) {
                //配送距离超过5000米
                throw new MapServiceException(ORDER_DISTANCE_TOO_LONG);
            }

        } catch (MapServiceException e) {
            // 往上继续抛出错误
            log.error(e.getMessage());
            throw new MapServiceException(e.getMessage());
        }

    }

    /**
     * 获取地址的省市区编码
     * */
    public String getLocationCode(String address){
        HashMap<String, String> map = new HashMap<>();
        map.put("address", address);
        map.put("output", "JSON");
        map.put("key", key);

        try {
            // 获取地址的经纬度坐标
            String addressCoordinate = HttpClientUtil.doGet(geocodeUrl, map);

            JSONObject jsonObject = JSON.parseObject(addressCoordinate);
            if (jsonObject.getString("status").equals("0")) {
                throw new MapServiceException(GET_ADDRESS_LOCATION_ERROR);
            }

            // 数据解析
            // 获取 geocodes 数组
            JSONArray geocodesArray = jsonObject.getJSONArray("geocodes");
            if (geocodesArray != null && !geocodesArray.isEmpty()) {
                // 获取 geocodes 数组中的第一个元素
                JSONObject firstGeocode = geocodesArray.getJSONObject(0);

                // 获取 adcode 字段
                String adcode = firstGeocode.getString("adcode");

                // 解析 adcode
                if (adcode != null) {
                    return adcode;
                }
            }
            throw new MapServiceException(GET_ADDRESS_LOCATION_ERROR);

        } catch (MapServiceException e) {
            log.error(e.getMessage());
            throw new MapServiceException(GET_ADDRESS_LOCATION_ERROR);
        }
    }


    /**
     * 通过得到的json解析出location
     */
    private String getLocation(HashMap<String, String> map) throws MapServiceException {
        // 获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet(geocodeUrl, map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if (jsonObject.getString("status").equals("0")) {
            throw new MapServiceException(GET_ADDRESS_LOCATION_ERROR);
        }

        // 数据解析
        // 获取 geocodes 数组
        JSONArray geocodesArray = jsonObject.getJSONArray("geocodes");

        if (geocodesArray != null && !geocodesArray.isEmpty()) {
            // 获取 geocodes 数组中的第一个元素
            JSONObject firstGeocode = geocodesArray.getJSONObject(0);

            // 获取 location 字段
            String location = firstGeocode.getString("location");

            // 解析 location 为经纬度
            if (location != null) {
                String[] latLng = location.split(",");
                if (latLng.length == 2)
                    return location;
            }
        }
        throw new MapServiceException(GET_ADDRESS_LOCATION_ERROR);
    }
}
