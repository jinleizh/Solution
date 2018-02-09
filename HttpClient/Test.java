package cn.webank.pmbank.mbac.biz.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class TestServiceImpl implements TestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestServiceImpl.class);
    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    @Value("${http.request.timeout:5000}")
    private static int httpRequestTimeout;

    @Value("${http.connect.timeout:5000}")
    private static int httpConnectTimeout;

    @Value("${http.read.timeout:5000}")
    private static int httpReadTimeout;

    private CloseableHttpClient httpClient = null;

    private static RequestConfig requestConfig;

    @PostConstruct
    public void init() {
        try {
            httpClient = HttpClientUtils.acceptsUntrustedCertsHttpClient(webankProxy, webankProxyPort);
        } catch (Exception e) {
            LOGGER.error("init httpClient", e);
        }
    }

    static {
        requestConfig = RequestConfig.custom().setSocketTimeout(httpReadTimeout).
                setConnectTimeout(httpConnectTimeout).build();
    }

    public Test test() {
        LOGGER.info("bizSeqNo={} Test begin, request={}", MumbleContextUtil.getBizSeqNo(), request);

        //1.计算签名
        PresentResponse response = new PresentResponse();
        PresentParam param = new PresentParam();
        param.setMobile(request.getMobile());
        Map<String, String> allParams = new HashMap<>(16);
        allParams.put("method", method);
        allParams.put("params", JSON_MAPPER.toJson(param));
        allParams.put("timestamp", String.valueOf(System.currentTimeMillis()));
        allParams.put("appid", appid);
        String sign = SignUtils.CountSign(allParams, appSecret);
        allParams.put("sign", sign);

        //2.发起调用
        String url = presentUrl + "?method=" + method;
        LOGGER.info("request url={}", url);
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(requestConfig);
            List<NameValuePair> list = new ArrayList<>(allParams.size());
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }

            LOGGER.info("post param list= {}", list);
            UrlEncodedFormEntity postEntity = new UrlEncodedFormEntity(list, HTTP.UTF_8);
            httpPost.setEntity(postEntity);
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            String rspBody = EntityUtils.toString(httpResponse.getEntity());

            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                response = JSON_MAPPER.fromJson(rspBody, PresentResponse.class);
            } else {
                LOGGER.error("Present, httpStatus: " + httpResponse.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
        }

        return response;
    }
}
