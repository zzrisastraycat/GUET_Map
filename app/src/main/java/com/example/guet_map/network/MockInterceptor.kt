package com.example.guet_map.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * 开发阶段 Mock 拦截器：拦截 API 请求并返回样本数据。
 * 接入真实后端后删除此拦截器即可。
 */
class MockInterceptor : Interceptor {

    private val jsonMediaType = "application/json".toMediaType()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        return when {
            path.matches(Regex("/api/v1/locations")) -> {
                mockResponse(request, LOCATIONS_JSON)
            }
            path.matches(Regex("/api/v1/locations/[^/]+")) -> {
                val locationId = path.substringAfterLast("/")
                val locationJson = LOCATION_DETAIL_JSON[locationId]
                    ?: """{"error":"not found","locationId":"$locationId"}"""
                mockResponse(request, locationJson)
            }
            path.matches(Regex("/api/v1/locations/[^/]+/guides")) -> {
                val locationId = path.split("/")[4]
                val guideJson = GUIDE_STEPS_JSON[locationId]
                    ?: GUIDE_STEPS_JSON["default"]!!
                mockResponse(request, guideJson)
            }
            path == "/api/v1/guides/upload" && request.method == "POST" -> {
                mockResponse(request, UPLOAD_RESPONSE_JSON)
            }
            else -> chain.proceed(request)
        }
    }

    private fun mockResponse(request: okhttp3.Request, body: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK (Mock)")
            .body(body.toResponseBody(jsonMediaType))
            .build()
    }
}

// ── Mock 数据 ─────────────────────────────────────────────────────

private val LOCATIONS_JSON = """
[
  {"locationId":"building_11a","name":"第十一教学楼A区","latitude":25.2865,"longitude":110.4145,"category":"教室","rating":4.3,"openingHours":"07:00-22:30","imageUrl":"https://example.com/img/11a.jpg","hasGuide":true},
  {"locationId":"building_11b","name":"第十一教学楼B区","latitude":25.2857,"longitude":110.4141,"category":"教室","rating":4.5,"openingHours":"07:00-22:30","imageUrl":"https://example.com/img/11b.jpg","hasGuide":true},
  {"locationId":"canteen_1","name":"第一学生食堂","latitude":25.2840,"longitude":110.4120,"category":"食堂","rating":4.0,"openingHours":"06:30-21:00","imageUrl":"https://example.com/img/canteen1.jpg","hasGuide":false},
  {"locationId":"canteen_2","name":"第二学生食堂","latitude":25.2830,"longitude":110.4130,"category":"食堂","rating":4.2,"openingHours":"06:30-21:30","imageUrl":"https://example.com/img/canteen2.jpg","hasGuide":true},
  {"locationId":"library","name":"校图书馆","latitude":25.2870,"longitude":110.4110,"category":"图书馆","rating":4.7,"openingHours":"08:00-22:00","imageUrl":"https://example.com/img/library.jpg","hasGuide":true},
  {"locationId":"coffee_lab","name":"实验室咖啡","latitude":25.2860,"longitude":110.4150,"category":"咖啡","rating":4.4,"openingHours":"08:30-21:00","imageUrl":"https://example.com/img/coffee.jpg","hasGuide":false},
  {"locationId":"sports_center","name":"体育中心","latitude":25.2820,"longitude":110.4160,"category":"运动场","rating":4.1,"openingHours":"06:00-22:00","imageUrl":"https://example.com/img/sports.jpg","hasGuide":false},
  {"locationId":"gate_south","name":"南门","latitude":25.2810,"longitude":110.4130,"category":"校门","rating":4.0,"openingHours":"全天","imageUrl":"https://example.com/img/gate.jpg","hasGuide":true}
]
""".trimIndent()

private val LOCATION_DETAIL_JSON = mapOf(
    "building_11b" to """{"locationId":"building_11b","name":"第十一教学楼B区","latitude":25.2857,"longitude":110.4141,"category":"教室","rating":4.5,"openingHours":"07:00-22:30","imageUrl":"https://example.com/img/11b.jpg","hasGuide":true}"""
)

private val GUIDE_STEPS_JSON = mapOf(
    "building_11b" to """
[
  {"id":1,"locationId":"building_11b","stepNumber":1,"description":"从南门进入校园，沿主干道直行约200米","imageUrl":"https://example.com/guide/11b_01.jpg"},
  {"id":2,"locationId":"building_11b","stepNumber":2,"description":"看到「创新大楼」指示牌后左转","imageUrl":"https://example.com/guide/11b_02.jpg"},
  {"id":3,"locationId":"building_11b","stepNumber":3,"description":"走到创维半岛大厦西塔大门","imageUrl":"https://example.com/guide/11b_03.jpg"},
  {"id":4,"locationId":"building_11b","stepNumber":4,"description":"乘坐电梯至3楼，出门后右侧即为B区入口","imageUrl":"https://example.com/guide/11b_04.jpg"}
]
""".trimIndent(),
    "default" to """
[
  {"id":1,"locationId":"unknown","stepNumber":1,"description":"暂无详细指引，请联系管理员添加","imageUrl":""}
]
""".trimIndent()
)

private val UPLOAD_RESPONSE_JSON = """
{"success":true,"message":"上传成功，待审核通过后将发放积分","pointsAwarded":5}
""".trimIndent()
